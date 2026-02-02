package com.visitors.manager;

import com.visitors.VisitorsMod;
import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.ModEntities;
import com.visitors.entity.VisitorEntity;
import com.visitors.util.TPSManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manager que controla el spawn y despawn de visitantes basado en el ciclo
 * día/noche.
 */
public class VisitorSpawnManager {

    // Singleton instance for command access
    public static final VisitorSpawnManager INSTANCE = new VisitorSpawnManager();

    // We only register the instance on the bus manually if needed, or if the class
    // itself is registered,
    // we should rely on that. But for simplicity in this command structure, we will
    // use this static instance.
    // NOTE: This assumes ModEventBus registers this instance or similar.
    // If external class registers 'new VisitorSpawnManager()', then INSTANCE will
    // be separate.
    // Ideally we should inject this, but for now we follow the pattern.
    // Wait, if ModEvents registers a DIFFERENT instance, then 'activeVisitors' will
    // be split.
    // Let's rely on this static INSTANCE being the primary one and register IT.

    private int tickCounter = 0;
    private final Random random = new Random();
    private final List<VisitorEntity> activeVisitors = new ArrayList<>();

    // Birthday event tracking
    private long lastBirthdayEventDay = -1;
    private static final int BIRTHDAY_GROUP_SIZE_MIN = 10;
    private static final int BIRTHDAY_GROUP_SIZE_MAX = 20;
    private static final double BIRTHDAY_EVENT_CHANCE = 0.15; // 15% per day check
    private boolean worldInitialized = false; // Track first load

    private VisitorSpawnManager() {
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (event.level.isClientSide())
            return;
        if (!(event.level instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) event.level;

        // Only process overworld
        if (serverLevel.dimension() != Level.OVERWORLD)
            return;

        tickCounter++;

        // If no players are connected, remove all visitors and stop processing
        if (serverLevel.players().isEmpty()) {
            if (!activeVisitors.isEmpty()) {
                removeAllVisitors(serverLevel);
                VisitorsMod.LOGGER.info("No players connected, cleared all visitors.");
            }
            return;
        }

        // If critically lagging, skip this tick's intensive checks
        if (TPSManager.isCriticallyLagging() && tickCounter % 2 != 0) {
            return;
        }

        // Clean up dead visitors from our list
        activeVisitors.removeIf(v -> v.isRemoved() || !v.isAlive());

        // Get configuration
        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

        // Check if fully configured
        if (!data.isFullyConfigured())
            return;

        // On first tick after server restart, remove all existing visitors
        if (!worldInitialized) {
            worldInitialized = true;
            removeAllVisitors(serverLevel);
        }

        // Check time of day
        long dayTime = serverLevel.getDayTime() % 24000;
        boolean isDaytime = dayTime >= 0 && dayTime < 12500;
        long currentDay = serverLevel.getDayTime() / 24000;

        if (isDaytime) {
            // Spawn new visitors during daytime
            if (tickCounter % data.getSpawnInterval() == 0) {
                trySpawnVisitor(serverLevel, data);
            }

            // Check for birthday event (once per day at morning)
            if (dayTime >= 1000 && dayTime < 1200 && lastBirthdayEventDay != currentDay) {
                if (data.hasValidBirthdayZone() && random.nextDouble() < BIRTHDAY_EVENT_CHANCE) {
                    triggerBirthdayEvent(serverLevel, data);
                    lastBirthdayEventDay = currentDay;
                }
            }

            // Check for random daytime escapes (New Feature)
            // Checked every 5 seconds (100 ticks)
            if (tickCounter % 100 == 0) {
                checkForDaytimeEscapes(serverLevel);
            }

            // Sync scoreboard periodically (ensures HUD is visible)
            if (tickCounter % 200 == 0) {
                data.updateScoreboard(serverLevel);
            }

        } else {
            // Make all visitors leave at night - some escape!
            if (tickCounter % 100 == 0) { // Check every 5 seconds
                makeVisitorsLeaveOrEscape(serverLevel, data);
            }
        }
    }

    private void checkForDaytimeEscapes(ServerLevel level) {
        int escapeCount = 0;
        for (VisitorEntity visitor : activeVisitors) {
            if (visitor.getVisitorState() == VisitorEntity.VisitorState.WANDERING ||
                    visitor.getVisitorState() == VisitorEntity.VisitorState.GOING_TO_BABY_ZONE ||
                    visitor.getVisitorState() == VisitorEntity.VisitorState.GOING_TO_BIRTHDAY) {

                // Very low chance per check per visitor, but adds up
                // 2% chance per 5 seconds per visitor to start escaping
                // Probabilities updated by user request: More escapes, especially babies
                // Balance updated: Minimal escapes by user request
                double escapeChance = visitor.isBaby() ? 0.025 : 0.01;
                if (random.nextDouble() < escapeChance) {
                    visitor.startEscaping();
                    escapeCount++;
                    VisitorsMod.LOGGER.debug("Visitor started ESCAPING during DAY! (Baby: {})", visitor.isBaby());
                }
            }
        }

        if (escapeCount > 0) {
            // broadcastEscapeMessage(level, escapeCount, true); // Removed chat alert as
            // requested
        }
    }

    private void trySpawnVisitor(ServerLevel level, VisitorsSavedData data) {
        // Check max visitors
        if (activeVisitors.size() >= data.getMaxVisitors()) {
            return;
        }

        // Find spawn position on the spawn line
        BlockPos spawn1 = data.getSpawn1();
        BlockPos spawn2 = data.getSpawn2();

        if (spawn1 == null || spawn2 == null)
            return;

        // Random position between spawn1 and spawn2
        double t = random.nextDouble();
        int x = (int) (spawn1.getX() + t * (spawn2.getX() - spawn1.getX()));
        int y = spawn1.getY();
        int z = (int) (spawn1.getZ() + t * (spawn2.getZ() - spawn1.getZ()));

        // Find ground level
        BlockPos spawnPos = findSpawnablePosition(level, new BlockPos(x, y, z));
        if (spawnPos == null) {
            VisitorsMod.LOGGER.debug("Could not find spawnable position for visitor");
            return;
        }

        // Check if position is valid (not inside blocks)
        if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir()) {
            return;
        }

        // Pick a random VALID area for this visitor
        List<VisitorsSavedData.Area> validAreas = new ArrayList<>();
        List<VisitorsSavedData.Area> allAreas = data.getAreas();
        for (int i = 0; i < allAreas.size(); i++) {
            if (allAreas.get(i).isValid()) {
                validAreas.add(allAreas.get(i));
            }
        }

        if (validAreas.isEmpty())
            return;

        // Simple retry strategy to find a valid index
        int areaIndex = -1;
        for (int i = 0; i < 10; i++) {
            int tryIndex = random.nextInt(allAreas.size());
            if (allAreas.get(tryIndex).isValid()) {
                areaIndex = tryIndex;
                break;
            }
        }

        if (areaIndex == -1)
            return;

        // Create and spawn the visitor
        VisitorEntity visitor = ModEntities.VISITOR.get().create(level);
        if (visitor == null)
            return;

        visitor.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        visitor.setVisitorState(VisitorEntity.VisitorState.ENTERING);
        visitor.setTargetArea(areaIndex);

        // Check if this should be a baby visitor
        if (data.hasValidBabyZone() && random.nextDouble() < data.getBabyChance()) {
            visitor.setBaby(true);
            visitor.setVisitorState(VisitorEntity.VisitorState.GOING_TO_BABY_ZONE);
            VisitorsMod.LOGGER.debug("Spawned BABY visitor at {}", spawnPos);
        }

        level.addFreshEntity(visitor);
        activeVisitors.add(visitor);

        // 5% chance to be a thief - Thieves start escaping immediately!
        if (random.nextDouble() < 0.05) {
            visitor.setThief(true);
            visitor.startEscaping();
            VisitorsMod.LOGGER.debug("Spawned THIEF visitor at {}", spawnPos);
        }

        // 3% chance to be a KILLER!
        if (random.nextDouble() < 0.03) {
            visitor.setKiller(true);
            VisitorsMod.LOGGER.debug("Spawned KILLER visitor at {}", spawnPos);
        }

        VisitorsMod.LOGGER.debug("Spawned visitor at {} for area {} (active: {})", spawnPos, areaIndex,
                activeVisitors.size());
    }

    private BlockPos findSpawnablePosition(ServerLevel level, BlockPos pos) {
        // Search vertically for a valid spawn position
        for (int yOffset = -5; yOffset <= 10; yOffset++) {
            BlockPos checkPos = pos.offset(0, yOffset, 0);

            // Need solid ground below and 2 blocks of air
            if (!level.getBlockState(checkPos.below()).isAir() &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {
                return checkPos;
            }
        }

        return null;
    }

    private void makeVisitorsLeaveOrEscape(ServerLevel level, VisitorsSavedData data) {
        int escapeCount = 0;
        for (VisitorEntity visitor : activeVisitors) {
            if (visitor.getVisitorState() != VisitorEntity.VisitorState.LEAVING &&
                    visitor.getVisitorState() != VisitorEntity.VisitorState.ESCAPING) {

                // Check if this visitor should escape instead of leave normally
                // Balance updated: Minimal escapes by user request
                double escapeChance = visitor.isBaby() ? 0.07 : 0.03;
                if (random.nextDouble() < escapeChance) {
                    visitor.startEscaping();
                    escapeCount++;
                    VisitorsMod.LOGGER.debug("Visitor started ESCAPING! (Baby: {})", visitor.isBaby());
                } else {
                    visitor.forceLeave();
                }
            }
        }

        if (escapeCount > 0) {
            // broadcastEscapeMessage(level, escapeCount, false); // Removed chat alert as
            // requested
        }
    }

    private void broadcastEscapeMessage(ServerLevel level, int count, boolean isDaytime) {
        String message;
        if (isDaytime) {
            message = count == 1
                    ? "§c§l¡ALERTA! §r§e¡Un visitante intenta huir sin pagar a plena luz del día! ¡Atrápalo!"
                    : "§c§l¡ALERTA! §r§e¡" + count + " visitantes intentan huir a plena luz del día! ¡Atrápalos!";
        } else {
            message = count == 1
                    ? "§c§l¡ALERTA NOCTURNA! §r§e¡Un visitante escapa en la oscuridad! ¡Detenlo!"
                    : "§c§l¡ALERTA NOCTURNA! §r§e¡" + count + " visitantes escapan en la oscuridad! ¡Detenlos!";
        }

        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    public void triggerBirthdayEvent(ServerLevel level, VisitorsSavedData data) {
        int groupSize = BIRTHDAY_GROUP_SIZE_MIN + random.nextInt(BIRTHDAY_GROUP_SIZE_MAX - BIRTHDAY_GROUP_SIZE_MIN + 1);

        // Broadcast message to all players
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.literal(
                    "§d§l¡EVENTO DE CUMPLEAÑOS! §r§e¡Un grupo de " + groupSize + " personas ha llegado a celebrar!"));
        }

        BlockPos spawn1 = data.getSpawn1();
        BlockPos spawn2 = data.getSpawn2();

        if (spawn1 == null || spawn2 == null)
            return;

        // Spawn birthday group
        for (int i = 0; i < groupSize; i++) {
            double t = random.nextDouble();
            int x = (int) (spawn1.getX() + t * (spawn2.getX() - spawn1.getX()));
            int y = spawn1.getY();
            int z = (int) (spawn1.getZ() + t * (spawn2.getZ() - spawn1.getZ()));

            BlockPos spawnPos = findSpawnablePosition(level, new BlockPos(x, y, z));
            if (spawnPos == null)
                continue;

            VisitorEntity visitor = ModEntities.VISITOR.get().create(level);
            if (visitor == null)
                continue;

            visitor.setPos(spawnPos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2, spawnPos.getY(),
                    spawnPos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2);
            visitor.setBirthdayParty(true);
            visitor.setVisitorState(VisitorEntity.VisitorState.GOING_TO_BIRTHDAY);
            visitor.setTargetArea(0); // They first go to main area

            level.addFreshEntity(visitor);
            activeVisitors.add(visitor);
        }

        VisitorsMod.LOGGER.info("Triggered birthday event with {} visitors", groupSize);
    }

    @SubscribeEvent
    public void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            VisitorsSavedData data = VisitorsSavedData.get(serverLevel);
            com.visitors.network.ModMessages.sendToPlayer(
                    new com.visitors.network.S2CRatingSyncPacket(data.getRating(), data.getRatingCount()),
                    player);
        }
    }

    private void removeAllVisitors(ServerLevel level) {
        // Find and remove all visitor entities on server restart
        List<VisitorEntity> toRemove = new ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof VisitorEntity) {
                toRemove.add((VisitorEntity) entity);
            }
        }
        for (VisitorEntity visitor : toRemove) {
            visitor.discard();
        }
        activeVisitors.clear();
        VisitorsMod.LOGGER.info("Removed {} visitors on server restart", toRemove.size());
    }

    public int getActiveVisitorCount() {
        return activeVisitors.size();
    }
}
