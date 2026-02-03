package com.visitors.event;

import com.visitors.command.ModCommands;
import com.visitors.data.VisitorsSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "visitors", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChairEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) event.player;
            if (ModCommands.chairEditors.contains(serverPlayer.getUUID())) {
                // Every 10 ticks, show particles
                if (serverPlayer.tickCount % 10 == 0) {
                    ServerLevel level = (ServerLevel) serverPlayer.level();
                    List<BlockPos> chairs = VisitorsSavedData.get(level).getChairs();

                    for (BlockPos pos : chairs) {
                        // Check distance to player to avoid too many particles
                        if (pos.closerThan(serverPlayer.blockPosition(), 32)) {
                            // Show outline of the block
                            showBlockOutline(level, pos);
                        }
                    }
                }
            }
        }
    }

    private static void showBlockOutline(ServerLevel level, BlockPos pos) {
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;

        // Show corners or edges
        level.sendParticles(ParticleTypes.WAX_ON, minX + 0.5, minY + 0.5, minZ + 0.5, 3, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, minX + 0.5, minY + 1.0, minZ + 0.5, 1, 0.2, 0.1, 0.2, 0.01);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
            if (ModCommands.chairEditors.contains(serverPlayer.getUUID())) {
                ServerLevel level = (ServerLevel) serverPlayer.level();
                BlockPos pos = event.getPos();
                VisitorsSavedData data = VisitorsSavedData.get(level);

                if (data.getChairs().contains(pos)) {
                    data.removeChair(pos);
                    serverPlayer.displayClientMessage(
                            Component.literal("§c[-] Silla eliminada en " + pos.toShortString()), true);
                } else {
                    data.addChair(pos);
                    serverPlayer.displayClientMessage(
                            Component.literal("§a[+] Silla agregada en " + pos.toShortString()), true);
                }

                // Visual feedback particles
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.05);

                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }
}
