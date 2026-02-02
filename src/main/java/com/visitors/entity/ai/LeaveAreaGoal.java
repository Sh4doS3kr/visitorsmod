package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal de IA para que el visitante salga del área y desaparezca.
 */
public class LeaveAreaGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos exitTarget;
    private int ticksLeaving;
    private static final int MAX_LEAVE_TICKS = 600; // 30 segundos máximo para salir

    public LeaveAreaGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return visitor.getVisitorState() == VisitorEntity.VisitorState.LEAVING;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && ticksLeaving < MAX_LEAVE_TICKS;
    }

    @Override
    public void start() {
        ticksLeaving = 0;
        findExit();
    }

    @Override
    public void stop() {
        exitTarget = null;
        // Remove the entity when done leaving
        if (!visitor.level().isClientSide) {
            visitor.discard();
        }
    }

    @Override
    public void tick() {
        ticksLeaving++;

        if (exitTarget == null || visitor.getNavigation().isDone()) {
            findExit();
        }

        // Check if we're far enough from the area
        if (!visitor.isInArea() && exitTarget != null) {
            double distToExit = visitor.distanceToSqr(exitTarget.getX(), exitTarget.getY(), exitTarget.getZ());
            if (distToExit < 4.0) {
                // We reached the exit, despawn
                visitor.discard();
            }
        }

        // Timeout - just despawn
        if (ticksLeaving >= MAX_LEAVE_TICKS) {
            visitor.discard();
        }
    }

    private void findExit() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();

        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);
        if (!data.hasValidSpawn()) {
            // No spawn defined, just go in a random direction away from area
            findRandomExit();
            return;
        }

        // Go back towards spawn area
        BlockPos spawn1 = data.getSpawn1();
        BlockPos spawn2 = data.getSpawn2();

        // Pick a random point on the spawn line
        int x = spawn1.getX() + visitor.getRandom().nextInt(Math.abs(spawn2.getX() - spawn1.getX()) + 1);
        int z = spawn1.getZ() + visitor.getRandom().nextInt(Math.abs(spawn2.getZ() - spawn1.getZ()) + 1);
        int y = spawn1.getY();

        exitTarget = new BlockPos(x, y, z);

        Path path = visitor.getNavigation().createPath(exitTarget, 1);
        if (path != null && path.canReach()) {
            visitor.getNavigation().moveTo(path, speed);
        } else {
            findRandomExit();
        }
    }

    private void findRandomExit() {
        BlockPos areaMin = visitor.getAreaMin();
        BlockPos areaMax = visitor.getAreaMax();

        if (areaMin == null || areaMax == null) {
            visitor.discard();
            return;
        }

        // Move away from area center
        double centerX = (areaMin.getX() + areaMax.getX()) / 2.0;
        double centerZ = (areaMin.getZ() + areaMax.getZ()) / 2.0;

        double dirX = visitor.getX() - centerX;
        double dirZ = visitor.getZ() - centerZ;
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (length > 0) {
            dirX /= length;
            dirZ /= length;
        } else {
            dirX = visitor.getRandom().nextDouble() - 0.5;
            dirZ = visitor.getRandom().nextDouble() - 0.5;
        }

        int targetX = (int) (visitor.getX() + dirX * 20);
        int targetZ = (int) (visitor.getZ() + dirZ * 20);

        exitTarget = new BlockPos(targetX, (int) visitor.getY(), targetZ);
        visitor.getNavigation().moveTo(targetX, visitor.getY(), targetZ, speed);
    }
}
