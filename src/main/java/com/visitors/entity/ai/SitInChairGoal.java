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

        if (visitor.getVisitorState() != VisitorEntity.VisitorState.WANDERING &&
                visitor.getVisitorState() != VisitorEntity.VisitorState.SITTING)
            return false;

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
        return chairPos != null && !visitor.isHungry() && !visitor.shouldLeave();
    }

    @Override
    public void start() {
        Level level = visitor.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) level;
            VisitorsSavedData.get(serverLevel).setChairOccupied(chairPos, visitor.getUUID());
        }
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

        if (dist < 1.5) { // Closer threshold
            if (!visitor.isPassenger()) {
                Level level = visitor.level();
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) level;

                    if (currentSeat != null && !currentSeat.isAlive()) {
                        cleanupSeat();
                    }

                    if (currentSeat == null) {
                        // Teleport slightly above chair center
                        visitor.setPos(chairPos.getX() + 0.5, chairPos.getY() + 0.1, chairPos.getZ() + 0.5);

                        double baseOffset = VisitorsSavedData.get(serverLevel).getChairYOffset();
                        // Correction for normal ArmorStand (NPC sits high, so we lower the stand)
                        double correction = -1.5;

                        currentSeat = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
                        currentSeat.setPos(chairPos.getX() + 0.5, chairPos.getY() + baseOffset + correction,
                                chairPos.getZ() + 0.5);
                        currentSeat.setInvisible(true);
                        currentSeat.setInvulnerable(true);
                        currentSeat.setNoGravity(true);

                        if (serverLevel.addFreshEntity(currentSeat)) {
                            visitor.setVisitorState(VisitorEntity.VisitorState.SITTING);
                            visitor.getNavigation().stop();
                            visitor.startRiding(currentSeat, true); // Force riding
                        }
                    } else {
                        // Re-mount if lost
                        visitor.startRiding(currentSeat, true);
                    }
                }
            }
            // Keep looking forward or at something
            visitor.getNavigation().stop();
        } else {
            // Move to chair
            visitor.getNavigation().moveTo(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5, speed);
            if (visitor.isPassenger()) {
                visitor.stopRiding();
                cleanupSeat();
            }
        }
    }
}
