package com.visitors.entity.ai;

import com.visitors.entity.VisitorEntity;
import com.visitors.util.TPSManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal de IA para que el visitante se mueva aleatoriamente dentro del área.
 */
public class WanderInAreaGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos targetPos;
    private int cooldown;
    private static final int MIN_COOLDOWN = 10; // 0.5 segundos
    private static final int MAX_COOLDOWN = 60; // 3 segundos

    public WanderInAreaGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return visitor.getVisitorState() == VisitorEntity.VisitorState.WANDERING
                && visitor.isInArea();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !visitor.shouldLeave();
    }

    @Override
    public void start() {
        cooldown = 0;
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

        // Reduced frequency: only look for new target if navigation is done
        if (targetPos == null || visitor.getNavigation().isDone() || visitor.getNavigation().isStuck()) {
            findNewTarget();
            // Higher cooldown when lagging
            int baseCooldown = TPSManager.isLagging() ? MAX_COOLDOWN : MIN_COOLDOWN;
            cooldown = baseCooldown + visitor.getRandom().nextInt(MAX_COOLDOWN);
        }
    }

    private void findNewTarget() {
        BlockPos randomPos = visitor.getRandomPositionInArea();
        if (randomPos == null)
            return;

        // Try to find a valid path
        // Reduce attempts to avoid lag! 3 is enough.
        for (int attempt = 0; attempt < 3; attempt++) {
            Path path = visitor.getNavigation().createPath(randomPos, 1);
            if (path != null && path.canReach()) {
                targetPos = randomPos;
                visitor.getNavigation().moveTo(path, speed);
                return;
            }
            randomPos = visitor.getRandomPositionInArea();
        }
    }
}
