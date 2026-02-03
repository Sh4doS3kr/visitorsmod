package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import java.util.EnumSet;

public class SitInChairGoal extends Goal {
    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos chairPos;
    private ArmorStand currentSeat;

    public SitInChairGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (visitor.isHungry() || visitor.isEscaping() || visitor.shouldLeave())
            return false;

        // Only start if wandering (WANDERING or SITTING if it was already sitting but
        // goal restarted)
        if (visitor.getVisitorState() != VisitorEntity.VisitorState.WANDERING &&
                visitor.getVisitorState() != VisitorEntity.VisitorState.SITTING)
            return false;

        // 5% chance every check if wandering
        if (visitor.getVisitorState() == VisitorEntity.VisitorState.WANDERING
                && visitor.getRandom().nextFloat() > 0.05f)
            return false;

        Level level = visitor.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) level;
            this.chairPos = VisitorsSavedData.get(serverLevel).getAvailableChair();
            return this.chairPos != null;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Stay until hungry or should leave.
        return chairPos != null && !visitor.isHungry() && !visitor.shouldLeave();
    }

    @Override
    public void start() {
        Level level = visitor.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) level;
            VisitorsSavedData.get(serverLevel).setChairOccupied(chairPos, visitor.getUUID());
        }
        visitor.getNavigation().moveTo(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        if (visitor.isPassenger()) {
            visitor.stopRiding();
        }
        cleanupSeat();

        Level level = visitor.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) level;
            VisitorsSavedData.get(serverLevel).releaseChair(chairPos);
        }

        if (visitor.getVisitorState() == VisitorEntity.VisitorState.SITTING) {
            visitor.setVisitorState(VisitorEntity.VisitorState.WANDERING);
        }
        this.chairPos = null;
    }

    private void cleanupSeat() {
        if (currentSeat != null) {
            currentSeat.discard();
            currentSeat = null;
        }
    }

    @Override
    public void tick() {
        if (chairPos == null)
            return;

        double dist = visitor.distanceToSqr(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5);
        if (dist < 2.0) { // Slightly tighter for mounting
            if (!visitor.isPassenger()) {
                Level level = visitor.level();
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) level;

                    // If we have a seat but are not riding it, try to re-mount or replace it
                    if (currentSeat != null) {
                        if (!currentSeat.isAlive()) {
                            cleanupSeat();
                        } else {
                            visitor.startRiding(currentSeat);
                            return; // Wait for next tick to see if it stuck
                        }
                    }

                    // Create new seat if none exists
                    if (currentSeat == null) {
                        visitor.setPos(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5);
                        double offsetY = VisitorsSavedData.get(serverLevel).getChairYOffset();

                        currentSeat = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
                        currentSeat.setPos(chairPos.getX() + 0.5, chairPos.getY() + offsetY, chairPos.getZ() + 0.5);
                        currentSeat.setInvisible(true);
                        currentSeat.setInvulnerable(true);
                        // Marker and Small are private/hard access in some forge versions, keeping it
                        // simple

                        if (serverLevel.addFreshEntity(currentSeat)) {
                            visitor.setVisitorState(VisitorEntity.VisitorState.SITTING);
                            visitor.getNavigation().stop();
                            visitor.startRiding(currentSeat);
                        }
                    }
                }
            } else {
                // Ensure state is SITTING if riding
                if (visitor.getVisitorState() != VisitorEntity.VisitorState.SITTING) {
                    visitor.setVisitorState(VisitorEntity.VisitorState.SITTING);
                }
                visitor.getNavigation().stop();
            }
        } else {
            // If we are too far, move towards it. If we were riding, something pushed us,
            // cleanup and re-route.
            if (visitor.isPassenger()) {
                visitor.stopRiding();
                cleanupSeat();
            }
            visitor.getNavigation().moveTo(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5, speed);
        }
    }
}
