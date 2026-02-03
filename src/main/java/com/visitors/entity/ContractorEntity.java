package com.visitors.entity;

import com.visitors.data.VisitorsSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ContractorEntity extends PathfinderMob {
    public ContractorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            // Placeholder for FazBucks logic
            player.sendSystemMessage(
                    Component.literal("§6[Contratista] §f¿Quieres contratar un limpiador por §e32 FazBucks§f al día?"));
            player.sendSystemMessage(Component.literal("§7(Haz click derecho de nuevo para confirmar - Simulación)"));

            // Logic to spawn CleanerEntity would go here
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.level().dayTime() % 24000 > 13000) {
            // Desaparece por la noche
            this.discard();
        }
    }
}
