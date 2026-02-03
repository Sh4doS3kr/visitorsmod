package com.visitors.entity.ai;

import com.visitors.entity.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class WaitInQueueGoal extends Goal {
    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos targetPos;

    // Queue start and end on Z axis at X=-506, Y=68
    private static final int QUEUE_X = -506;
    private static final int QUEUE_Y = 68;
    private static final int QUEUE_Z_START = 546;
    private static final int QUEUE_Z_END = 551;

    public WaitInQueueGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only if hungry and NOT leaving/escaping
        return visitor.isHungry() && !visitor.shouldLeave() && !visitor.isEscaping();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        visitor.addToQueue();
    }

    @Override
    public void stop() {
        visitor.removeFromQueue();
        visitor.getNavigation().stop();
    }

    @Override
    public void tick() {
        int posInQueue = visitor.getQueuePosition();

        if (posInQueue >= 0) {
            // Queue advances from Z=551 to Z=546. Pos 0 is 551, Pos 5 is 546.
            int targetZ = QUEUE_Z_END - posInQueue;

            // Limit to start of queue line
            if (targetZ < QUEUE_Z_START) {
                targetZ = QUEUE_Z_START;
            }

            targetPos = new BlockPos(QUEUE_X, QUEUE_Y, targetZ);

            double distSq = visitor.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            if (distSq > 1.0) {
                visitor.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, speed);
            } else {
                visitor.getNavigation().stop();
                // Look towards where the food comes from? (Assume Z < 546)
                visitor.getLookControl().setLookAt(QUEUE_X + 0.5, QUEUE_Y + 1.5, QUEUE_Z_START - 2.0);
            }
        }
    }
}
