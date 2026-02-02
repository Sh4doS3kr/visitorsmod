package com.visitors.entity.ai;

import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.VisitorEntity;
import com.visitors.util.TPSManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * Goal de IA para que los visitantes de cumpleaños se muevan normalmente
 * dentro de la zona de cumpleaños (sin correr, velocidad normal).
 */
public class WanderInBirthdayZoneGoal extends Goal {

    private final VisitorEntity visitor;
    private final double speed;
    private BlockPos targetPos;
    private int cooldown;
    private static final int MIN_COOLDOWN = 40; // 2 segundos - cooldown normal
    private static final int MAX_COOLDOWN = 160; // 8 segundos - se mueven tranquilos

    private AABB birthdayZoneBounds = null;

    public WanderInBirthdayZoneGoal(VisitorEntity visitor, double speed) {
        this.visitor = visitor;
        this.speed = speed; // Velocidad normal, sin multiplicador
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!visitor.isBirthdayParty())
            return false;
        if (visitor.getVisitorState() != VisitorEntity.VisitorState.WANDERING)
            return false;

        // Check if we're in the birthday zone
        return isInBirthdayZone();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !visitor.shouldLeave();
    }

    @Override
    public void start() {
        cooldown = 0;
        updateBirthdayZoneBounds();
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

        // Reduced frequency check
        if (targetPos == null || visitor.getNavigation().isDone()) {
            findNewTarget();
            // Even higher cooldown when lagging for birthday visitors
            int baseCooldown = TPSManager.isLagging() ? MAX_COOLDOWN * 2 : MIN_COOLDOWN;
            cooldown = baseCooldown + visitor.getRandom().nextInt(MAX_COOLDOWN);
        }
    }

    private boolean isInBirthdayZone() {
        if (!(visitor.level() instanceof ServerLevel))
            return false;

        if (birthdayZoneBounds == null || visitor.tickCount % 100 == 0) {
            updateBirthdayZoneBounds();
        }

        return birthdayZoneBounds != null && birthdayZoneBounds.contains(visitor.position());
    }

    private void updateBirthdayZoneBounds() {
        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();
        VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

        if (!data.hasValidBirthdayZone()) {
            birthdayZoneBounds = null;
            return;
        }

        BlockPos bp1 = data.getBirthdayPos1();
        BlockPos bp2 = data.getBirthdayPos2();
        BlockPos min = new BlockPos(
                Math.min(bp1.getX(), bp2.getX()),
                Math.min(bp1.getY(), bp2.getY()),
                Math.min(bp1.getZ(), bp2.getZ()));
        BlockPos max = new BlockPos(
                Math.max(bp1.getX(), bp2.getX()),
                Math.max(bp1.getY(), bp2.getY()),
                Math.max(bp1.getZ(), bp2.getZ()));
        birthdayZoneBounds = new AABB(min, max.offset(1, 1, 1));
    }

    private void findNewTarget() {
        if (birthdayZoneBounds == null) {
            updateBirthdayZoneBounds();
            if (birthdayZoneBounds == null)
                return;
        }

        if (!(visitor.level() instanceof ServerLevel))
            return;
        ServerLevel serverLevel = (ServerLevel) visitor.level();

        // Get random position within birthday zone bounds
        int minX = (int) birthdayZoneBounds.minX;
        int maxX = (int) birthdayZoneBounds.maxX;
        int minZ = (int) birthdayZoneBounds.minZ;
        int maxZ = (int) birthdayZoneBounds.maxZ;
        int minY = (int) birthdayZoneBounds.minY;
        int maxY = (int) birthdayZoneBounds.maxY;

        // Reduced attempts to avoid lag! 3 is enough.
        for (int attempt = 0; attempt < 3; attempt++) {
            int x = minX + visitor.getRandom().nextInt(Math.max(1, maxX - minX));
            int z = minZ + visitor.getRandom().nextInt(Math.max(1, maxZ - minZ));
            int y = minY;

            // Find ground level
            for (int checkY = maxY; checkY >= minY; checkY--) {
                BlockPos checkPos = new BlockPos(x, checkY, z);
                if (!serverLevel.getBlockState(checkPos).isAir() &&
                        serverLevel.getBlockState(checkPos.above()).isAir()) {
                    y = checkY + 1;
                    break;
                }
            }

            BlockPos randomPos = new BlockPos(x, y, z);
            Path path = visitor.getNavigation().createPath(randomPos, 1);
            if (path != null && path.canReach()) {
                targetPos = randomPos;
                visitor.getNavigation().moveTo(path, speed);
                return;
            }
        }
    }
}
