package com.visitors.util;

import com.visitors.data.VisitorsSavedData;
import net.minecraft.server.level.ServerLevel;

public class PlaceholderUtil {

    /**
     * Replaces placeholders in a string.
     * Currently supports: %pizzeria_estrellas%
     */
    public static String process(String text, ServerLevel level) {
        if (text == null || !text.contains("%pizzeria_estrellas%")) {
            return text;
        }

        VisitorsSavedData data = VisitorsSavedData.get(level);
        float rating = data.getRating();
        int fullStars = Math.round(rating);

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < fullStars) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }

        return text.replace("%pizzeria_estrellas%", stars.toString());
    }
}
