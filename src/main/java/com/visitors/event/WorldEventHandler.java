package com.visitors.event;

import com.visitors.VisitorsMod;
import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VisitorsMod.MOD_ID)
public class WorldEventHandler {

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) event.level;
            VisitorsSavedData data = VisitorsSavedData.get(level);
            long currentTime = level.dayTime();

            // Initialize timers if they are 0 (first run or world creation)
            // to avoid immediate spawning
            if (data.getLastContractorTime() == 0) {
                data.setLastContractorTime(currentTime);
            }
            if (data.getLastInspectionTime() == 0) {
                data.setLastInspectionTime(currentTime);
            }

            // Contractor Event (Every 3 days = 72000 ticks)
            if (currentTime - data.getLastContractorTime() >= 72000) {
                // Check if it's daytime (0-12000 ticks)
                long timeOfDay = currentTime % 24000;
                if (timeOfDay < 12000) {
                    spawnManagementNPC(level, data, "contractor");
                    data.setLastContractorTime(currentTime);
                }
            }

            // Inspection Event (Every 10 days = 240000 ticks)
            if (currentTime - data.getLastInspectionTime() >= 240000) {
                // Check if it's daytime (0-12000 ticks)
                long timeOfDay = currentTime % 24000;
                if (timeOfDay < 12000) {
                    spawnManagementNPC(level, data, "inspector");
                    data.setLastInspectionTime(currentTime);
                }
            }

            // Sync with HUD (every 20 ticks)
            if (level.getGameTime() % 20 == 0) {
                long nextInsp = Math.max(0, 240000 - (currentTime - data.getLastInspectionTime()));
                long nextCont = Math.max(0, 72000 - (currentTime - data.getLastContractorTime()));
                com.visitors.network.ModMessages.sendToAllClients(
                        new com.visitors.network.S2CManagementSyncPacket(nextInsp, nextCont, data.isClosedBySanity()));
            }
        }
    }

    private static void spawnManagementNPC(ServerLevel level, VisitorsSavedData data, String type) {
        BlockPos spawnPos = data.getSpawn1(); // Default to spawn1
        if (spawnPos == null)
            return;

        Entity npc = null;
        String message = "";

        if (type.equals("contractor")) {
            npc = ModEntities.CONTRACTOR.get().create(level);
            message = "§6[Gestión] §fUn contratista ha llegado al local.";
        } else if (type.equals("inspector")) {
            npc = ModEntities.INSPECTOR.get().create(level);
            message = "§d[Sanidad] §f¡EL INSPECTOR DE SANIDAD HA LLEGADO! Prepárate para la revisión.";
        }

        if (npc != null) {
            npc.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            level.addFreshEntity(npc);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }
}
