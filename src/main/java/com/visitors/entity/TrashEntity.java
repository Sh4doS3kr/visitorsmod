package com.visitors.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class TrashEntity extends Entity {
    private static final EntityDataAccessor<Integer> CLICKS = SynchedEntityData.defineId(TrashEntity.class,
            EntityDataSerializers.INT);

    public TrashEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CLICKS, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setClicks(tag.getInt("Clicks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Clicks", getClicks());
    }

    public int getClicks() {
        return this.entityData.get(CLICKS);
    }

    public void setClicks(int clicks) {
        this.entityData.set(CLICKS, clicks);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            int currentClicks = getClicks() + 1;
            setClicks(currentClicks);

            player.displayClientMessage(Component.literal("§7Limpiando basura... §e" + currentClicks + "/10"), true);

            if (currentClicks >= 10) {
                if (this.level() instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) this.level();
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.2, this.getZ(),
                            15, 0.2, 0.2, 0.2, 0.05);
                    serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.2, this.getZ(), 5, 0.1,
                            0.1, 0.1, 0.02);
                }
                this.discard();
                player.displayClientMessage(Component.literal("§a¡Área limpia!"), true);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
