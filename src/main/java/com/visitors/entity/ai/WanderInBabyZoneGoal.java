package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import com.visitors.util.TPSManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * Goal de IA para que los bebés se muevan RÁPIDO y activamente dentro de la
 * zona baby.
 * Los niños corren mucho más rápido que los adultos y apenas descansan.
 */
public class WanderInBabyZoneGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos targetPos;
    private int cooldown;
    private static final int MIN_COOLDOWN = 10; // Muy poco cooldown - 0.5 segundos
    private static final int MAX_COOLDOWN = 40; // Máximo 2 segundos
    private static final double BABY_SPEED_MULTIPLIER = 1.8; // 80% más rápido que adultos

    private AABB babyZoneBounds = null;

    public WanderInBabyZoneGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed * BABY_SPEED_MULTIPLIER;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!visitor.isBaby())
            return false;
        if (visitor.getVisitorState() != VisitorEntity.VisitorState.WANDERING)
            return false;

        // Check if we're in the baby zone
        return isInBabyZone();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !visitor.shouldLeave();
    }

    @Override
    public void start() {
        cooldown = 0;
        updateBabyZoneBounds();
        findNewTarget();
    }

    @Override
    public void stop() {
        targetPos = null;
        visitor.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Reduced frequency check
        if (targetPos == null || visitor.getNavigation().isDone()) {
            findNewTarget();
            // Higher cooldown when lagging
            int baseCooldown = TPSManager.isLagging() ? MAX_COOLDOWN : MIN_COOLDOWN;
            cooldown = baseCooldown + visitor.getRandom().nextInt(MAX_COOLDOWN);
        }

        // Occasionally jump while running (hyper babies!)
        if (visitor.getRandom().nextFloat() < 0.02f && visitor.onGround()) {
            visitor.doJump();
        }
    }

    private boolean isInBabyZone() {
        if (!(visitor.level() instanceof ServerLevel))
            return false;

        if (babyZoneBounds == null || visitor.tickCount % 100 == 0) {
            updateBabyZoneBounds();
        }

        return babyZoneBounds != null && babyZoneBounds.contains(visitor.position());
    }

    private void updateBabyZoneBounds() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();
        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

        if (!data.hasValidBabyZone()) {
            babyZoneBounds = null;
            return;
        }

        BlockPos bp1 = data.getBabyZonePos1();
        BlockPos bp2 = data.getBabyZonePos2();
        BlockPos min = new BlockPos(
                Math.min(bp1.getX(), bp2.getX()),
                Math.min(bp1.getY(), bp2.getY()),
                Math.min(bp1.getZ(), bp2.getZ()));
        BlockPos max = new BlockPos(
                Math.max(bp1.getX(), bp2.getX()),
                Math.max(bp1.getY(), bp2.getY()),
                Math.max(bp1.getZ(), bp2.getZ()));
        babyZoneBounds = new AABB(min, max.offset(1, 1, 1));
    }

    private void findNewTarget() {
        if (babyZoneBounds == null) {
            updateBabyZoneBounds();
            if (babyZoneBounds == null)
                return;
        }

        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();

        // Get random position within baby zone bounds
        int minX = (int) babyZoneBounds.minX;
        int maxX = (int) babyZoneBounds.maxX;
        int minZ = (int) babyZoneBounds.minZ;
        int maxZ = (int) babyZoneBounds.maxZ;
        int minY = (int) babyZoneBounds.minY;
        int maxY = (int) babyZoneBounds.maxY;

        // Reduced attempts to avoid lag! 3 is enough.
        for (int attempt = 0; attempt < 3; attempt++) {
            int x = minX + visitor.getRandom().nextInt(Math.max(1, maxX - minX));
            int z = minZ + visitor.getRandom().nextInt(Math.max(1, maxZ - minZ));
            int y = minY;

            // Find ground level
            for (int checkY = maxY; checkY >= minY; checkY--) {
                BlockPos checkPos = new BlockPos(x, checkY, z);
                if (!serverLevel.getBlockState(checkPos).isAir() &&
                        serverLevel.getBlockState(checkPos.above()).isAir()) {
                    y = checkY + 1;
                    break;
                }
            }

            BlockPos randomPos = new BlockPos(x, y, z);
            Path path = visitor.getNavigation().createPath(randomPos, 1);
            if (path != null && path.canReach()) {
                targetPos = randomPos;
                visitor.getNavigation().moveTo(path, speed);
                return;
            }
        }
    }
}
