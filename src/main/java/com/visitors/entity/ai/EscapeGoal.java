package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal de IA para que el visitante escape en dirección contraria al spawn.
 * Los visitantes escapando corren más rápido y pueden ser capturados para
 * FazBucks.
 */
public class EscapeGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos escapeTarget;
    private int ticksEscaping;
    private static final int MAX_ESCAPE_TICKS = 1200; // 60 segundos máximo para escapar
    private static final double ESCAPE_SPEED_MULTIPLIER = 1.6; // 60% más rápido

    private double lastPosX;
    private double lastPosZ;
    private int stuckTicks;

    public EscapeGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed * ESCAPE_SPEED_MULTIPLIER;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return visitor.isEscaping() && visitor.getVisitorState() == VisitorEntity.VisitorState.ESCAPING;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && ticksEscaping < MAX_ESCAPE_TICKS;
    }

    @Override
    public void start() {
        ticksEscaping = 0;
        lastPosX = visitor.getX();
        lastPosZ = visitor.getZ();
        stuckTicks = 0;
        findEscapeDirection();
    }

    @Override
    public void stop() {
        escapeTarget = null;
        // Remove the entity when done escaping (if not captured)
        if (!visitor.level().isClientSide) {
            visitor.discard();
        }
    }

    @Override
    public void tick() {
        ticksEscaping++;

        // Forced progress check: if not moved in 2 seconds, find new direction
        if (ticksEscaping % 10 == 0) {
            double movedDist = Math
                    .sqrt(Math.pow(visitor.getX() - lastPosX, 2) + Math.pow(visitor.getZ() - lastPosZ, 2));
            if (movedDist < 0.1) {
                stuckTicks += 10;
            } else {
                stuckTicks = 0;
            }
            lastPosX = visitor.getX();
            lastPosZ = visitor.getZ();
        }

        if (stuckTicks > 40 || escapeTarget == null || visitor.getNavigation().isDone()
                || visitor.getNavigation().isStuck()) {
            stuckTicks = 0;
            findEscapeDirection();
        }

        // Check if we're far enough from escape target to find next one
        if (escapeTarget != null) {
            double distToTarget = visitor.distanceToSqr(escapeTarget.getX(), escapeTarget.getY(), escapeTarget.getZ());
            if (distToTarget < 9.0) { // Within 3 blocks
                // Find new escape target further away
                findEscapeDirection();
            }
        }

        // Timeout - just despawn
        if (ticksEscaping >= MAX_ESCAPE_TICKS) {
            visitor.discard();
        }
    }

    private void findEscapeDirection() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();
        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

        BlockPos spawn1 = data.getSpawn1();
        BlockPos spawn2 = data.getSpawn2();
        BlockPos areaMin = visitor.getAreaMin();
        BlockPos areaMax = visitor.getAreaMax();

        if (spawn1 == null || spawn2 == null || areaMin == null || areaMax == null) {
            // Fallback: Just move away from current player position if possible, or random
            double angle = visitor.getRandom().nextDouble() * Math.PI * 2;
            moveInDirection(Math.cos(angle), Math.sin(angle), 20);
            return;
        }

        // Calculate midpoints
        double spawnCenterX = (spawn1.getX() + spawn2.getX()) / 2.0;
        double spawnCenterZ = (spawn1.getZ() + spawn2.getZ()) / 2.0;

        double areaCenterX = (areaMin.getX() + areaMax.getX()) / 2.0;
        double areaCenterZ = (areaMin.getZ() + areaMax.getZ()) / 2.0;

        // Inward vector: from spawn entrance towards center of restaurant
        double dirX = areaCenterX - spawnCenterX;
        double dirZ = areaCenterZ - spawnCenterZ;

        // Normalize
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length > 0.1) {
            dirX /= length;
            dirZ /= length;
        } else {
            // If they are the same (unlikely), pick a default
            dirX = 0;
            dirZ = 1;
        }

        // Add a tiny bit of sway (very tight) to avoid perfectly straight lines looking
        // robotic,
        // but keeping it "strictly" towards opposite side as requested.
        double sway = (visitor.getRandom().nextDouble() - 0.5) * 0.2;
        double finalDirX = dirX + sway * (-dirZ);
        double finalDirZ = dirZ + sway * dirX;

        // Final normalization
        double finalLength = Math.sqrt(finalDirX * finalDirX + finalDirZ * finalDirZ);
        finalDirX /= finalLength;
        finalDirZ /= finalLength;

        // Try to move 20-40 blocks in that direction
        moveInDirection(finalDirX, finalDirZ, 20 + visitor.getRandom().nextInt(20));
    }

    private void moveInDirection(double dirX, double dirZ, int distance) {
        int targetX = (int) (visitor.getX() + dirX * distance);
        int targetZ = (int) (visitor.getZ() + dirZ * distance);
        int targetY = (int) visitor.getY();

        // Find ground level at target
        ServerLevel level = (ServerLevel) visitor.level();
        for (int yOffset = 10; yOffset >= -10; yOffset--) {
            BlockPos checkPos = new BlockPos(targetX, targetY + yOffset, targetZ);
            if (!level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {
                targetY = targetY + yOffset + 1;
                break;
            }
        }

        escapeTarget = new BlockPos(targetX, targetY, targetZ);
        visitor.getNavigation().moveTo(targetX + 0.5, targetY, targetZ + 0.5, speed);
    }
}
