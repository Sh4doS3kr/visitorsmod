package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * Goal de IA para que el visitante busque una entrada al área definida.
 * Busca un bloque de aire en el borde del área y navega hacia él.
 */
public class FindEntranceGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos targetPos;
    private int tryCounter;
    private static final int MAX_TRIES = 20;

    public FindEntranceGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return visitor.getVisitorState() == VisitorEntity.VisitorState.ENTERING;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !visitor.isInArea();
    }

    @Override
    public void start() {
        tryCounter = 0;
        findEntrance();
    }

    @Override
    public void stop() {
        targetPos = null;
        visitor.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos == null || visitor.getNavigation().isDone()) {
            tryCounter++;
            if (tryCounter >= MAX_TRIES) {
                // Si no puede encontrar entrada, quedarse quieto un momento
                tryCounter = 0;
            }
            findEntrance();
        }

        // Check if we entered the area
        if (visitor.isInArea()) {
            visitor.setVisitorState(VisitorEntity.VisitorState.WANDERING);
        }
    }

    private void findEntrance() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();

        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);
        if (!data.hasValidArea())
            return;

        int areaIndex = visitor.getTargetArea();
        VisitorsSavedData.Area area = data.getArea(areaIndex);

        if (!area.isValid())
            return;

        BlockPos pos1 = area.pos1;
        BlockPos pos2 = area.pos2;

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());

        // Find an accessible entrance point on the border of the area
        BlockPos bestEntrance = null;
        double bestDistance = Double.MAX_VALUE;

        // Check all border positions
        for (int attempt = 0; attempt < 50; attempt++) {
            int x, z;
            int side = visitor.getRandom().nextInt(4);

            switch (side) {
                case 0: // North side
                    x = minX + visitor.getRandom().nextInt(maxX - minX + 1);
                    z = minZ;
                    break;
                case 1: // South side
                    x = minX + visitor.getRandom().nextInt(maxX - minX + 1);
                    z = maxZ;
                    break;
                case 2: // West side
                    x = minX;
                    z = minZ + visitor.getRandom().nextInt(maxZ - minZ + 1);
                    break;
                default: // East side
                    x = maxX;
                    z = minZ + visitor.getRandom().nextInt(maxZ - minZ + 1);
                    break;
            }

            // Find ground level
            for (int y = maxY; y >= minY; y--) {
                BlockPos checkPos = new BlockPos(x, y, z);
                BlockPos abovePos = checkPos.above();
                BlockPos above2Pos = checkPos.above(2);

                // Check if this is a valid entrance (solid ground, air above)
                if (!serverLevel.getBlockState(checkPos).isAir() &&
                        serverLevel.getBlockState(abovePos).isAir() &&
                        serverLevel.getBlockState(above2Pos).isAir()) {

                    BlockPos entrancePos = abovePos;
                    double distance = visitor.distanceToSqr(entrancePos.getX(), entrancePos.getY(), entrancePos.getZ());

                    // Check if we can path there
                    Path path = visitor.getNavigation().createPath(entrancePos, 1);
                    if (path != null && path.canReach()) {
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestEntrance = entrancePos;
                        }
                    }
                    break;
                }
            }
        }

        if (bestEntrance != null) {
            targetPos = bestEntrance;
            visitor.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, speed);
        }
    }
}
