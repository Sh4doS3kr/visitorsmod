package com.visitors.entity;

import com.visitors.data.VisitorsSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class InspectorEntity extends PathfinderMob {
    private int inspectionTicks = 0;
    private boolean finished = false;

    public InspectorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new InspectionWanderGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !finished) {
            inspectionTicks++;
            if (inspectionTicks >= 1200) { // 1 minute of inspection
                finishInspection();
            }
        }
    }

    private void finishInspection() {
        finished = true;
        if (this.level() instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

            // Comprehensive Scan
            List<String> reportPages = generateReport(serverLevel, data);

            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag tag = book.getOrCreateTag();
            tag.putString("title", "Informe de Sanidad");
            tag.putString("author", "Inspector de Sanidad");
            ListTag pages = new ListTag();
            for (String page : reportPages) {
                pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
            }
            tag.put("pages", pages);

            // Give to nearest player
            Player player = serverLevel.getNearestPlayer(this, 10.0D);
            if (player != null) {
                if (!player.getInventory().add(book)) {
                    player.drop(book, false);
                }
                player.sendSystemMessage(
                        Component.literal("§d[Inspector] §fHe terminado mi inspección. Aquí está el informe."));
            }

            this.discard();
        }
    }

    private List<String> generateReport(ServerLevel level, VisitorsSavedData data) {
        List<String> pages = new ArrayList<>();
        int trashCount = 0;
        int lowCeilingCount = 0;
        int darkCount = 0;

        // Scan active area
        VisitorsSavedData.Area area = data.getArea(0); // Primary area
        if (area.isValid()) {
            BlockPos p1 = area.pos1;
            BlockPos p2 = area.pos2;
            int minX = Math.min(p1.getX(), p2.getX());
            int maxX = Math.max(p1.getX(), p2.getX());
            int minY = Math.min(p1.getY(), p2.getY());
            int maxY = Math.max(p1.getY(), p2.getY());
            int minZ = Math.min(p1.getZ(), p2.getZ());
            int maxZ = Math.max(p1.getZ(), p2.getZ());

            // Check Trash Entities
            AABB auditBox = new AABB(minX, minY, minZ, maxX, maxY + 5, maxZ);
            trashCount = level.getEntitiesOfClass(TrashEntity.class, auditBox).size();

            // Height Audit
            if (Math.abs(maxY - minY) < 2)
                lowCeilingCount++;

            // Light Audit (Sampled)
            for (int x = minX; x <= maxX; x += 5) {
                for (int z = minZ; z <= maxZ; z += 5) {
                    if (level.getMaxLocalRawBrightness(new BlockPos(x, maxY, z)) < 7)
                        darkCount++;
                }
            }
        }

        StringBuilder p1 = new StringBuilder("§0§lINFORME DE SANIDAD§r\n\n");
        p1.append("Basura detectada: ").append(trashCount > 5 ? "§4" : "§2").append(trashCount).append("\n");
        p1.append("Zonas oscuras: ").append(darkCount > 3 ? "§4" : "§2").append(darkCount).append("\n");
        p1.append("Techos bajos: ").append(lowCeilingCount > 0 ? "§4" : "§2").append(lowCeilingCount > 0 ? "SÍ" : "NO")
                .append("\n\n");

        boolean passed = trashCount <= 10 && darkCount <= 5 && lowCeilingCount == 0;
        p1.append("RESULTADO: ").append(passed ? "§2§lAPROBADO" : "§4§lSUSPENDIDO");
        pages.add(p1.toString());

        if (!passed) {
            pages.add(
                    "§0Debido a las graves deficiencias encontradas, el establecimiento debe PERMANECER CERRADO hasta que se solventen los problemas.");
            data.setClosedBySanity(true);
        } else {
            pages.add("§0El establecimiento cumple con las normas mínimas. Puede continuar operando.");
            data.setClosedBySanity(false);
        }

        return pages;
    }

    static class InspectionWanderGoal extends WaterAvoidingRandomStrollGoal {
        public InspectionWanderGoal(PathfinderMob mob) {
            super(mob, 0.8D);
        }
    }
}
