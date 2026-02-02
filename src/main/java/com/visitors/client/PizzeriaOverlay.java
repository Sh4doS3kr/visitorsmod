package com.visitors.client;

import com.visitors.VisitorsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class PizzeriaOverlay {

    private static final ResourceLocation STAR_FULL = new ResourceLocation(VisitorsMod.MOD_ID,
            "textures/gui/star_full.png");
    private static final ResourceLocation STAR_HALF = new ResourceLocation(VisitorsMod.MOD_ID,
            "textures/gui/star_half.png");

    public static final IGuiOverlay HUD_RATING = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective obj = scoreboard.getObjective("pizzeria_rating");
        if (obj == null)
            return;

        if (!scoreboard.hasPlayerScore("GlobalRating", obj))
            return;
        int scoreVal = scoreboard.getOrCreatePlayerScore("GlobalRating", obj).getScore();
        float rating = scoreVal / 10.0f;

        int x = width - 110;
        int y = 10;

        // Draw Stars
        for (int i = 1; i <= 5; i++) {
            ResourceLocation texture = STAR_FULL;
            float r = 1.0f, g = 1.0f, b = 1.0f;

            if (rating >= i) {
                // Full
            } else if (rating >= i - 0.5f) {
                texture = STAR_HALF;
            } else {
                // Empty star (darkened full)
                r = 0.3f;
                g = 0.3f;
                b = 0.3f;
            }

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0f);
            guiGraphics.blit(texture, x + (i * 18), y, 0, 0, 16, 16, 16, 16);
        }

        // Reset color
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw Text
        String text = String.format("%.1f/5", rating);
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, x + 90 - (textWidth / 2), y + 20, 0xFFFFFF, false);
    };
}
