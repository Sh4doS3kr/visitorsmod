package com.visitors.block;

import com.visitors.data.VisitorsSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class StarsPosterBlock extends Block {
    public StarsPosterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

            float rating = data.getRating();
            int fullStars = Math.round(rating);

            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < fullStars) {
                    stars.append("§6★"); // Golden star
                } else {
                    stars.append("§7☆"); // Grey empty star
                }
            }

            player.sendSystemMessage(Component.literal("§e§l--- Reputación del Local ---"));
            player.sendSystemMessage(
                    Component.literal("§fRating: " + stars.toString() + " §e" + String.format("%.1f/5.0", rating)));
            player.sendSystemMessage(Component.literal("§7(Basado en " + data.getRatingCount() + " reseñas)"));
        }
        return InteractionResult.SUCCESS;
    }
}
