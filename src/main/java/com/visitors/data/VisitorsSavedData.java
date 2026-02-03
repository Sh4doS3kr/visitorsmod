package com.visitors.data;

import com.visitors.VisitorsMod;
import com.visitors.network.ModMessages;
import com.visitors.network.S2CRatingSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Score;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Clase para guardar los datos del mod de forma persistente.
 * Almacena las posiciones del área y los puntos de spawn.
 */
public class VisitorsSavedData extends SavedData {

    private static final String DATA_NAME = VisitorsMod.MOD_ID + "_data";

    private final java.util.List<Area> areas = new java.util.ArrayList<>();
    private BlockPos spawn1;
    private BlockPos spawn2;

    // Birthday zone
    private BlockPos birthdayPos1;
    private BlockPos birthdayPos2;

    // Baby zone
    private BlockPos babyZonePos1;
    private BlockPos babyZonePos2;

    // Chairs
    private final java.util.List<BlockPos> chairs = new java.util.ArrayList<>();
    private final java.util.Map<BlockPos, UUID> occupiedChairs = new java.util.HashMap<>();

    // Configuration
    private int maxVisitors = 10;
    private int spawnInterval = 100;
    private double escapeChance = 0.12; // 12% chance to escape at night
    private double babyChance = 0.18; // 18% chance to spawn as baby

    // Rating System
    private float ratingSum = 15.0f; // Start with 3.0 avg * 5
    private int ratingCount = 5;
    private float reputationBonus = 0.0f; // Bonus stackable for heroic actions
    private float chairYOffset = -0.55f; // New Sitting Height Offset

    public VisitorsSavedData() {
        // Initialize at least one area
        areas.add(new Area());
    }

    public VisitorsSavedData(CompoundTag tag) {
        this();
        load(tag);
    }

    public static VisitorsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                VisitorsSavedData::new,
                VisitorsSavedData::new,
                DATA_NAME);
    }

    private void load(CompoundTag tag) {
        areas.clear();
        if (tag.contains("Areas")) {
            net.minecraft.nbt.ListTag areasTag = tag.getList("Areas", 10);
            for (int i = 0; i < areasTag.size(); i++) {
                CompoundTag areaTag = areasTag.getCompound(i);
                Area area = new Area();
                if (areaTag.contains("Pos1")) {
                    CompoundTag p = areaTag.getCompound("Pos1");
                    area.pos1 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
                }
                if (areaTag.contains("Pos2")) {
                    CompoundTag p = areaTag.getCompound("Pos2");
                    area.pos2 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
                }
                areas.add(area);
            }
        }

        // Backward compatibility / Legacy loading
        if (areas.isEmpty()) {
            Area area = new Area();
            if (tag.contains("Pos1")) {
                CompoundTag p = tag.getCompound("Pos1");
                area.pos1 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
            }
            if (tag.contains("Pos2")) {
                CompoundTag p = tag.getCompound("Pos2");
                area.pos2 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
            }
            areas.add(area);
        }

        if (tag.contains("Spawn1")) {
            CompoundTag spawn1Tag = tag.getCompound("Spawn1");
            spawn1 = new BlockPos(spawn1Tag.getInt("X"), spawn1Tag.getInt("Y"), spawn1Tag.getInt("Z"));
        }
        if (tag.contains("Spawn2")) {
            CompoundTag spawn2Tag = tag.getCompound("Spawn2");
            spawn2 = new BlockPos(spawn2Tag.getInt("X"), spawn2Tag.getInt("Y"), spawn2Tag.getInt("Z"));
        }
        if (tag.contains("MaxVisitors")) {
            maxVisitors = tag.getInt("MaxVisitors");
        }
        if (tag.contains("SpawnInterval")) {
            spawnInterval = tag.getInt("SpawnInterval");
        }
        if (tag.contains("EscapeChance")) {
            escapeChance = tag.getDouble("EscapeChance");
        }
        if (tag.contains("BabyChance")) {
            babyChance = tag.getDouble("BabyChance");
        }
        // Birthday zone
        if (tag.contains("BirthdayPos1")) {
            CompoundTag p = tag.getCompound("BirthdayPos1");
            birthdayPos1 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
        }
        if (tag.contains("BirthdayPos2")) {
            CompoundTag p = tag.getCompound("BirthdayPos2");
            birthdayPos2 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
        }
        // Baby zone
        if (tag.contains("BabyZonePos1")) {
            CompoundTag p = tag.getCompound("BabyZonePos1");
            babyZonePos1 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
        }
        if (tag.contains("BabyZonePos2")) {
            CompoundTag p = tag.getCompound("BabyZonePos2");
            babyZonePos2 = new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z"));
        }

        if (tag.contains("RatingSum")) {
            ratingSum = tag.getFloat("RatingSum");
        }
        if (tag.contains("RatingCount")) {
            ratingCount = tag.getInt("RatingCount");
        }
        if (tag.contains("ReputationBonus")) {
            reputationBonus = tag.getFloat("ReputationBonus");
        }
        if (tag.contains("ChairYOffset")) {
            chairYOffset = tag.getFloat("ChairYOffset");
        }

        chairs.clear();
        if (tag.contains("Chairs")) {
            net.minecraft.nbt.ListTag chairsTag = tag.getList("Chairs", 10);
            for (int i = 0; i < chairsTag.size(); i++) {
                CompoundTag p = chairsTag.getCompound(i);
                chairs.add(new BlockPos(p.getInt("X"), p.getInt("Y"), p.getInt("Z")));
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        net.minecraft.nbt.ListTag areasTag = new net.minecraft.nbt.ListTag();
        for (Area area : areas) {
            CompoundTag areaTag = new CompoundTag();
            if (area.pos1 != null) {
                CompoundTag p = new CompoundTag();
                p.putInt("X", area.pos1.getX());
                p.putInt("Y", area.pos1.getY());
                p.putInt("Z", area.pos1.getZ());
                areaTag.put("Pos1", p);
            }
            if (area.pos2 != null) {
                CompoundTag p = new CompoundTag();
                p.putInt("X", area.pos2.getX());
                p.putInt("Y", area.pos2.getY());
                p.putInt("Z", area.pos2.getZ());
                areaTag.put("Pos2", p);
            }
            areasTag.add(areaTag);
        }
        tag.put("Areas", areasTag);

        if (spawn1 != null) {
            CompoundTag spawn1Tag = new CompoundTag();
            spawn1Tag.putInt("X", spawn1.getX());
            spawn1Tag.putInt("Y", spawn1.getY());
            spawn1Tag.putInt("Z", spawn1.getZ());
            tag.put("Spawn1", spawn1Tag);
        }
        if (spawn2 != null) {
            CompoundTag spawn2Tag = new CompoundTag();
            spawn2Tag.putInt("X", spawn2.getX());
            spawn2Tag.putInt("Y", spawn2.getY());
            spawn2Tag.putInt("Z", spawn2.getZ());
            tag.put("Spawn2", spawn2Tag);
        }
        tag.putInt("MaxVisitors", maxVisitors);
        tag.putInt("SpawnInterval", spawnInterval);
        tag.putDouble("EscapeChance", escapeChance);
        tag.putDouble("BabyChance", babyChance);
        // Birthday zone
        if (birthdayPos1 != null) {
            CompoundTag p = new CompoundTag();
            p.putInt("X", birthdayPos1.getX());
            p.putInt("Y", birthdayPos1.getY());
            p.putInt("Z", birthdayPos1.getZ());
            tag.put("BirthdayPos1", p);
        }
        if (birthdayPos2 != null) {
            CompoundTag p = new CompoundTag();
            p.putInt("X", birthdayPos2.getX());
            p.putInt("Y", birthdayPos2.getY());
            p.putInt("Z", birthdayPos2.getZ());
            tag.put("BirthdayPos2", p);
        }
        // Baby zone
        if (babyZonePos1 != null) {
            CompoundTag p = new CompoundTag();
            p.putInt("X", babyZonePos1.getX());
            p.putInt("Y", babyZonePos1.getY());
            p.putInt("Z", babyZonePos1.getZ());
            tag.put("BabyZonePos1", p);
        }
        if (babyZonePos2 != null) {
            CompoundTag p = new CompoundTag();
            p.putInt("X", babyZonePos2.getX());
            p.putInt("Y", babyZonePos2.getY());
            p.putInt("Z", babyZonePos2.getZ());
            tag.put("BabyZonePos2", p);
        }
        tag.putFloat("RatingSum", ratingSum);
        tag.putInt("RatingCount", ratingCount);
        tag.putFloat("ReputationBonus", reputationBonus);
        tag.putFloat("ChairYOffset", chairYOffset);

        net.minecraft.nbt.ListTag chairsTag = new net.minecraft.nbt.ListTag();
        for (BlockPos p : chairs) {
            CompoundTag tp = new CompoundTag();
            tp.putInt("X", p.getX());
            tp.putInt("Y", p.getY());
            tp.putInt("Z", p.getZ());
            chairsTag.add(tp);
        }
        tag.put("Chairs", chairsTag);

        return tag;
    }

    public static class Area {
        public BlockPos pos1;
        public BlockPos pos2;

        public boolean isValid() {
            return pos1 != null && pos2 != null;
        }

        public long getPlaneArea() {
            if (!isValid())
                return 0;
            return (long) (Math.abs(pos1.getX() - pos2.getX()) + 1) * (Math.abs(pos1.getZ() - pos2.getZ()) + 1);
        }

        public net.minecraft.world.phys.AABB getBoxAABB() {
            if (!isValid())
                return new net.minecraft.world.phys.AABB(0, 0, 0, 0, 0, 0);
            return new net.minecraft.world.phys.AABB(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ()),
                    Math.max(pos1.getX(), pos2.getX()) + 1,
                    Math.max(pos1.getY(), pos2.getY()) + 1,
                    Math.max(pos1.getZ(), pos2.getZ()) + 1);
        }
    }

    public java.util.List<Area> getAreas() {
        return areas;
    }

    public Area getArea(int index) {
        while (areas.size() <= index) {
            areas.add(new Area());
        }
        return areas.get(index);
    }

    @Nullable
    public BlockPos getPos1() {
        return getArea(0).pos1;
    }

    public void setPos1(BlockPos pos) {
        getArea(0).pos1 = pos;
        setDirty();
    }

    @Nullable
    public BlockPos getPos2() {
        return getArea(0).pos2;
    }

    public void setPos2(BlockPos pos) {
        getArea(0).pos2 = pos;
        setDirty();
    }

    @Nullable
    public BlockPos getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(BlockPos pos) {
        this.spawn1 = pos;
        setDirty();
    }

    @Nullable
    public BlockPos getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(BlockPos pos) {
        this.spawn2 = pos;
        setDirty();
    }

    public boolean hasValidArea() {
        return areas.stream().anyMatch(Area::isValid);
    }

    public boolean hasValidSpawn() {
        return spawn1 != null && spawn2 != null;
    }

    public boolean isFullyConfigured() {
        return hasValidArea() && hasValidSpawn();
    }

    public int getMaxVisitors() {
        return maxVisitors;
    }

    public void setMaxVisitors(int max) {
        this.maxVisitors = max;
        setDirty();
    }

    public int getSpawnInterval() {
        return spawnInterval;
    }

    public void setSpawnInterval(int interval) {
        this.spawnInterval = interval;
        setDirty();
    }

    // Birthday zone getters/setters
    @Nullable
    public BlockPos getBirthdayPos1() {
        return birthdayPos1;
    }

    public void setBirthdayPos1(BlockPos pos) {
        this.birthdayPos1 = pos;
        setDirty();
    }

    @Nullable
    public BlockPos getBirthdayPos2() {
        return birthdayPos2;
    }

    public void setBirthdayPos2(BlockPos pos) {
        this.birthdayPos2 = pos;
        setDirty();
    }

    public boolean hasValidBirthdayZone() {
        return birthdayPos1 != null && birthdayPos2 != null;
    }

    // Baby zone getters/setters
    @Nullable
    public BlockPos getBabyZonePos1() {
        return babyZonePos1;
    }

    public void setBabyZonePos1(BlockPos pos) {
        this.babyZonePos1 = pos;
        setDirty();
    }

    @Nullable
    public BlockPos getBabyZonePos2() {
        return babyZonePos2;
    }

    public void setBabyZonePos2(BlockPos pos) {
        this.babyZonePos2 = pos;
        setDirty();
    }

    public boolean hasValidBabyZone() {
        return babyZonePos1 != null && babyZonePos2 != null;
    }

    // Escape/baby chance config
    public double getEscapeChance() {
        return escapeChance;
    }

    public void setEscapeChance(double chance) {
        this.escapeChance = chance;
        setDirty();
    }

    public double getBabyChance() {
        return babyChance;
    }

    public void setBabyChance(double chance) {
        this.babyChance = chance;
        setDirty();
    }

    // Rating Logic
    public void addReview(int stars, ServerLevel level) {
        ratingSum += stars;
        ratingCount++;
        setDirty();
        updateScoreboard(level);

        // Broadcast to all clients
        ModMessages.sendToAllClients(new S2CRatingSyncPacket(getRating(), ratingCount));
    }

    public float getRating() {
        return ratingCount == 0 ? 0 : ratingSum / ratingCount;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void resetReviews(ServerLevel level) {
        this.ratingSum = 0;
        this.ratingCount = 0;
        setDirty();
        updateScoreboard(level);
        ModMessages.sendToAllClients(new S2CRatingSyncPacket(0, 0));
    }

    public void updateScoreboard(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        Objective obj = scoreboard.getObjective("pizzeria_rating");
        if (obj == null) {
            obj = scoreboard.addObjective("pizzeria_rating", ObjectiveCriteria.DUMMY, Component.literal("Rating"),
                    ObjectiveCriteria.RenderType.INTEGER);
        }
        // Set score. We multiply by 10 to keep 1 decimal (4.2 -> 42).
        int scoreVal = (int) (getRating() * 10);
        scoreboard.getOrCreatePlayerScore("GlobalRating", obj).setScore(scoreVal);
    }

    public float getReputationBonus() {
        return reputationBonus;
    }

    public void addReputationBonus(float amount) {
        this.reputationBonus = Math.min(1.0f, this.reputationBonus + amount); // Max 1.0 bonus per review
        setDirty();
    }

    public void consumeReputationBonus(float amount) {
        this.reputationBonus = Math.max(0, this.reputationBonus - amount);
        setDirty();
    }

    // Chair Management
    public java.util.List<BlockPos> getChairs() {
        return chairs;
    }

    public void addChair(BlockPos pos) {
        if (!chairs.contains(pos)) {
            chairs.add(pos);
            setDirty();
        }
    }

    public void removeChair(BlockPos pos) {
        chairs.remove(pos);
        occupiedChairs.remove(pos);
        setDirty();
    }

    public boolean isChairOccupied(BlockPos pos) {
        return occupiedChairs.containsKey(pos);
    }

    public void setChairOccupied(BlockPos pos, UUID entityId) {
        occupiedChairs.put(pos, entityId);
    }

    public void releaseChair(BlockPos pos) {
        occupiedChairs.remove(pos);
    }

    public void releaseChairByEntity(UUID entityId) {
        occupiedChairs.values().removeIf(value -> value.equals(entityId));
    }

    public BlockPos getAvailableChair() {
        for (BlockPos chair : chairs) {
            if (!isChairOccupied(chair))
                return chair;
        }
        return null;
    }

    public float getChairYOffset() {
        return chairYOffset;
    }

    public void setChairYOffset(float offset) {
        this.chairYOffset = offset;
        setDirty();
    }
}
