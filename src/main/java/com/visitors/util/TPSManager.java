package com.visitors.util;

import net.minecraft.server.MinecraftServer;
import com.visitors.network.ModMessages;
import com.visitors.network.S2CPerformancePacket;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.visitors.VisitorsMod;

@Mod.EventBusSubscriber(modid = VisitorsMod.MOD_ID)
public class TPSManager {
    private static long lastUpdateTime = 0;
    private static double currentTps = 20.0;
    private static double currentMspt = 0;
    private static final double[] tpsHistory = new double[100];
    private static final double[] msptHistory = new double[100];
    private static int historyIndex = 0;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter % 20 == 0) {
                ModMessages.sendToAllClients(new S2CPerformancePacket((float) currentTps, (float) currentMspt));
            }

            long currentTime = System.currentTimeMillis();
            if (lastUpdateTime != 0) {
                long diff = currentTime - lastUpdateTime;

                // MSPT is the actual time the tick took (diff)
                // TPS is 1000 / MAX(diff, 50)
                double tps = 1000.0 / Math.max(diff, 50);
                double mspt = (double) diff;

                tpsHistory[historyIndex] = tps;
                msptHistory[historyIndex] = mspt;
                historyIndex = (historyIndex + 1) % tpsHistory.length;

                // Calculate average TPS
                double tpsSum = 0;
                for (double d : tpsHistory)
                    tpsSum += d;
                currentTps = tpsSum / tpsHistory.length;

                // Calculate average MSPT
                double msptSum = 0;
                for (double d : msptHistory)
                    msptSum += d;
                currentMspt = msptSum / msptHistory.length;
            }
            lastUpdateTime = currentTime;
        }
    }

    public static double getTps() {
        return currentTps;
    }

    public static double getMspt() {
        return currentMspt;
    }

    public static boolean isLagging() {
        return currentTps < 18.0;
    }

    public static boolean isCriticallyLagging() {
        return currentTps < 12.0;
    }
}
