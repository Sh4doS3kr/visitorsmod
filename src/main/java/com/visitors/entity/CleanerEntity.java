package com.visitors.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class CleanerEntity extends PathfinderMob {
    public CleanerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new CleanGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.level().dayTime() % 24000 > 13000) {
            this.discard();
        }
    }

    static class CleanGoal extends Goal {
        private final CleanerEntity cleaner;
        private TrashEntity targetTrash;

        public CleanGoal(CleanerEntity cleaner) {
            this.cleaner = cleaner;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            AABB box = cleaner.getBoundingBox().inflate(16.0D);
            List<TrashEntity> list = cleaner.level().getEntitiesOfClass(TrashEntity.class, box);
            if (!list.isEmpty()) {
                targetTrash = list.get(0);
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            cleaner.getNavigation().moveTo(targetTrash, 1.2D);
        }

        @Override
        public void tick() {
            if (targetTrash == null || !targetTrash.isAlive())
                return;
            if (cleaner.distanceToSqr(targetTrash) < 2.0D) {
                targetTrash.discard();
                if (cleaner.level() instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) cleaner.level();
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetTrash.getX(), targetTrash.getY(),
                            targetTrash.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                }
                targetTrash = null;
            }
        }
    }
}
