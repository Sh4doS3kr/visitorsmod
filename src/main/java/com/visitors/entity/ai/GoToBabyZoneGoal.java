package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * Goal de IA para que los visitantes bebés vayan primero a la zona
 * principal y luego a la zona baby.
 */
public class GoToBabyZoneGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos targetPos;
    private int ticksMoving;
    private boolean reachedMainArea = false;
    private boolean reachedBabyArea = false;
    private static final int MAX_TICKS = 2400; // 2 minutes max

    public GoToBabyZoneGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return visitor.isBaby() &&
                visitor.getVisitorState() == VisitorEntity.VisitorState.GOING_TO_BABY_ZONE;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !reachedBabyArea && ticksMoving < MAX_TICKS;
    }

    @Override
    public void start() {
        ticksMoving = 0;
        reachedMainArea = false;
        reachedBabyArea = false;
        findTargetPosition();
    }

    @Override
    public void stop() {
        targetPos = null;
        if (reachedBabyArea) {
            // Stay wandering in baby area
            visitor.setVisitorState(VisitorEntity.VisitorState.WANDERING);
        }
    }

    @Override
    public void tick() {
        ticksMoving++;

        if (targetPos == null || visitor.getNavigation().isDone()) {
            checkProgress();
            findTargetPosition();
        }

        // Timeout protection
        if (ticksMoving >= MAX_TICKS) {
            visitor.setVisitorState(VisitorEntity.VisitorState.WANDERING);
        }
    }

    private void checkProgress() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();
        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

        // Check if in main area
        if (!reachedMainArea && visitor.isInArea()) {
            reachedMainArea = true;
        }

        // Check if in baby area
        if (reachedMainArea && data.hasValidBabyZone()) {
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
            AABB babyBounds = new AABB(min, max.offset(1, 1, 1));
            if (babyBounds.contains(visitor.position())) {
                reachedBabyArea = true;
            }
        }
    }

    private void findTargetPosition() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();
        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

        if (!reachedMainArea) {
            // First go to main area
            targetPos = visitor.getRandomPositionInArea();
        } else if (!reachedBabyArea && data.hasValidBabyZone()) {
            // Then go to baby zone
            BlockPos bp1 = data.getBabyZonePos1();
            BlockPos bp2 = data.getBabyZonePos2();

            int x = bp1.getX() + visitor.getRandom().nextInt(Math.abs(bp2.getX() - bp1.getX()) + 1);
            int z = bp1.getZ() + visitor.getRandom().nextInt(Math.abs(bp2.getZ() - bp1.getZ()) + 1);
            int y = Math.min(bp1.getY(), bp2.getY());

            // Find ground level
            for (int checkY = Math.max(bp1.getY(), bp2.getY()); checkY >= y; checkY--) {
                BlockPos checkPos = new BlockPos(x, checkY, z);
                if (!serverLevel.getBlockState(checkPos).isAir() &&
                        serverLevel.getBlockState(checkPos.above()).isAir()) {
                    y = checkY + 1;
                    break;
                }
            }

            targetPos = new BlockPos(x, y, z);
        }

        if (targetPos != null) {
            Path path = visitor.getNavigation().createPath(targetPos, 1);
            if (path != null && path.canReach()) {
                visitor.getNavigation().moveTo(path, speed);
            }
        }
    }
}
