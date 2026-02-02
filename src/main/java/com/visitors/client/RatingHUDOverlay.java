package com.visitors.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class RatingHUDOverlay {
    private static float currentRating = 0.0f;
    private static int currentCount = 0;
    private static float currentTps = 20.0f;
    private static float currentMspt = 0.0f;

    public static void setRating(float rating, int count) {
        currentRating = rating;
        currentCount = count;
    }

    public static void setPerformance(float tps, float mspt) {
        currentTps = tps;
        currentMspt = mspt;
    }

    public static final IGuiOverlay HUD_RATING = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui)
            return;

        int x = screenWidth - 110;
        int y = 10;

        // Render background/title (optional, simple text for now)
        guiGraphics.drawString(mc.font, "Reseñas: " + currentCount, x, y, 0xFFFFFF);

        // Render Stars
        int starX = x;
        int starY = y + 12;
        int fullStars = Math.round(currentRating);

        for (int i = 0; i < 5; i++) {
            String star = (i < fullStars) ? "§6★" : "§7☆";
            guiGraphics.drawString(mc.font, star, starX + (i * 12), starY, 0xFFFFFF);
        }

        guiGraphics.drawString(mc.font, String.format("%.1f/5.0", currentRating), x + 65, starY,
                0xEECB00);

        // --- PERFORMANCE MONITOR ---
        int perfY = starY + 15;

        // TPS Color
        String tpsColor = "§a";
        if (currentTps < 12)
            tpsColor = "§c";
        else if (currentTps < 18)
            tpsColor = "§e";

        // MSPT Color
        String msptColor = "§a";
        if (currentMspt > 50)
            msptColor = "§c";
        else if (currentMspt > 35)
            msptColor = "§e";

        String perfText = String.format("%sTPS: %.1f§f  %sMSPT: %.1f§f",
                tpsColor, currentTps, msptColor, currentMspt);

        guiGraphics.drawString(mc.font, perfText, x - 10, perfY, 0xFFFFFF, true);
    };
}
