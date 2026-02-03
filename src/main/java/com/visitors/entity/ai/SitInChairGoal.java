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
        return chairPos != null && !visitor.isHungry() && !visitor.shouldLeave() && sittingTicks < 2400;
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
        if (dist < 0.5) {
            if (!visitor.isPassenger()) {
                Level level = visitor.level();
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) level;
                    // Create invisible seat
                    // Offset Y by -0.5 to -0.6 for standard blocks so they look seated ON the
                    // surface
                    double offsetY = -0.55;

                    // Check if block below is a table/high block (detecting by height in image)
                    // If the user put chairs on tables, Y might be higher. we trust chairPos.

                    ArmorStand seat = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
                    seat.setPos(chairPos.getX() + 0.5, chairPos.getY() + offsetY, chairPos.getZ() + 0.5);
                    seat.setInvisible(true);

                    seat.setNoBasePlate(true);
                    seat.setNoGravity(true);
                    seat.setInvulnerable(true);

                    if (serverLevel.addFreshEntity(seat)) {
                        visitor.setVisitorState(VisitorEntity.VisitorState.SITTING);
                        visitor.getNavigation().stop();
                        visitor.startRiding(seat);
                    }
                }
            }
            sittingTicks++;
        } else {
            if (visitor.isPassenger()) {
                visitor.stopRiding();
            }
            visitor.getNavigation().moveTo(chairPos.getX() + 0.5, chairPos.getY(), chairPos.getZ() + 0.5, speed);
        }
    }
}
