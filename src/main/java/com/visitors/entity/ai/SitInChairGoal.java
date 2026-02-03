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
    private int sittingTicks;
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
        if (visitor.getVisitorState() != VisitorEntity.VisitorState.WANDERING)
            return false;

        // 5% chance every check
        if (visitor.getRandom().nextFloat() > 0.05f)
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
        // Stay until hungry or should leave. Remove the timer limit as requested.
        return chairPos != null && !visitor.isHungry() && !visitor.shouldLeave();
    }

    @Override
    public void start() {
        sittingTicks = 0;
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
        if (currentSeat != null) {
            currentSeat.discard();
            currentSeat = null;
        }

        Level level = visitor.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) level;
            VisitorsSavedData.get(serverLevel).releaseChair(chairPos);
        }
        visitor.setVisitorState(VisitorEntity.VisitorState.WANDERING);
        this.chairPos = null;
    }

    @Override
    public void tick() {
        if (chairPos == null)
            return;

        double dist = visitor.distanceToSqr(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5);
        if (dist < 2.5) {
            if (!visitor.isPassenger()) {
                // If we were already sitting and lost the seat, something is wrong, stop goal
                // to avoid spamming Armor Stands. This is the fix for the 5000 Armor Stands.
                if (currentSeat != null) {
                    this.stop();
                    return;
                }

                Level level = visitor.level();
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) level;

                    // Force position to center
                    visitor.setPos(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5);

                    double offsetY = VisitorsSavedData.get(serverLevel).getChairYOffset();

                    currentSeat = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
                    currentSeat.setPos(chairPos.getX() + 0.5, chairPos.getY() + offsetY, chairPos.getZ() + 0.5);
                    currentSeat.setInvisible(true);
                    currentSeat.setInvulnerable(true);

                    if (serverLevel.addFreshEntity(currentSeat)) {
                        visitor.setVisitorState(VisitorEntity.VisitorState.SITTING);
                        visitor.getNavigation().stop();
                        visitor.startRiding(currentSeat);

                        // Small smoke to indicate sitting
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                                chairPos.getX() + 0.5, chairPos.getY() + 0.5, chairPos.getZ() + 0.5, 3, 0.1, 0.1, 0.1,
                                0.02);
                    }
                }
            }
            sittingTicks++;
        } else {
            // If we are too far, and we are riding, dismount.
            if (visitor.isPassenger()) {
                visitor.stopRiding();
                if (currentSeat != null) {
                    currentSeat.discard();
                    currentSeat = null;
                }
            }
            visitor.getNavigation().moveTo(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5, speed);
        }
    }
}
