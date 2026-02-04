package com.visitors.entity;

import com.visitors.VisitorsMod;
import com.visitors.data.VisitorsSavedData;
import com.visitors.entity.TrashEntity;
import com.visitors.entity.ai.*;
import com.visitors.item.ModItems;
import com.visitors.data.ReviewTextPool;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

public class VisitorEntity extends PathfinderMob {

    public enum VisitorState {
        ENTERING,
        WANDERING,
        LEAVING,
        ESCAPING,
        GOING_TO_BIRTHDAY,
        GOING_TO_BABY_ZONE,
        SITTING
    }

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.INT);

    // Physical Order Queue at X=-506, Y=68, Z=546-551
    private static final java.util.List<VisitorEntity> orderQueue = new java.util.ArrayList<>();

    public void addToQueue() {
        if (!orderQueue.contains(this)) {
            orderQueue.add(this);
        }
    }

    public void removeFromQueue() {
        orderQueue.remove(this);
    }

    public int getQueuePosition() {
        return orderQueue.indexOf(this);
    }

    private static final EntityDataAccessor<Integer> TARGET_AREA = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_ESCAPING = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_BABY_VISITOR = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_BIRTHDAY_PARTY = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_HUNGRY = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_THIEF = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_KILLER = SynchedEntityData.defineId(VisitorEntity.class,
            EntityDataSerializers.BOOLEAN);

    private int hungerTimer = 0;
    private int deliveryTimer = 0;
    private int trashCooldown = 0;
    private int lowLightTicks = 0;
    private int satisfactionScore = 5; // BUFF: Subida base de 4 a 5 (Máximo inicial)
    private boolean wasFed = false; // Track if visitor was fed - guarantees minimum 4 stars

    private static final int MIN_STAY_TIME = 60 * 20;
    private static final int MAX_STAY_TIME = 300 * 20;

    private int stayTime;
    private int ticksInArea;
    private boolean shouldLeave = false;
    private boolean hasGivenReward = false;
    private int killerClicks = 0;
    private int sittingDismountTicks = 0;

    private BlockPos areaMin;
    private BlockPos areaMax;
    private AABB cachedAreaAABB;

    public VisitorEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.stayTime = this.getRandom().nextInt(MAX_STAY_TIME - MIN_STAY_TIME) + MIN_STAY_TIME;
        this.ticksInArea = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public boolean isBaby() {
        return this.entityData.get(IS_BABY_VISITOR);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, VisitorState.ENTERING.ordinal());
        this.entityData.define(VARIANT, this.getRandom().nextInt(3));
        this.entityData.define(TARGET_AREA, 0);
        this.entityData.define(IS_ESCAPING, false);
        this.entityData.define(IS_BABY_VISITOR, false);
        this.entityData.define(IS_BIRTHDAY_PARTY, false);
        this.entityData.define(IS_HUNGRY, false);
        this.entityData.define(IS_THIEF, false);
        this.entityData.define(IS_KILLER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Killer AI
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false) {
            @Override
            public boolean canUse() {
                return isKiller() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isKiller() && super.canUse();
            }
        });

        this.goalSelector.addGoal(1, new EscapeGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LeaveAreaGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new SitInChairGoal(this, 1.0D)); // New goal!
        this.goalSelector.addGoal(3, new FindEntranceGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new GoToBirthdayZoneGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new GoToBabyZoneGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new WanderInBabyZoneGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new WanderInBirthdayZoneGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new WanderInAreaGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new WaitInQueueGoal(this, 1.0D)); // Priority goal for hungry customers
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (getVisitorState() == VisitorState.SITTING) {
            this.setXRot(0);
            this.setYRot(this.yRotO);
            this.setPose(net.minecraft.world.entity.Pose.SITTING); // Visual sitting

            if (!this.level().isClientSide && !this.isPassenger()) {
                sittingDismountTicks++;
                if (sittingDismountTicks > 10) { // 0.5s buffer
                    setVisitorState(VisitorState.WANDERING);
                    sittingDismountTicks = 0;
                }
            } else {
                sittingDismountTicks = 0;
            }
        } else if (!this.level().isClientSide && this.isPassenger() && this.getVehicle() instanceof ArmorStand) {
            Entity seat = this.getVehicle();
            this.stopRiding();
            seat.discard();
        }

        if (!this.level().isClientSide) {
            if (isKiller()) {
                this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                if (this.getMainHandItem().isEmpty()) {
                    this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                }
            }

            if (areaMin == null || areaMax == null) {
                updateAreaBounds();
            }

            // High complexity checks every 5 seconds (Reduced from 2s)
            if (this.tickCount % 100 == 0) {
                int light = this.level().getMaxLocalRawBrightness(this.blockPosition());
                if (light < 7) {
                    lowLightTicks += 100;
                    if (lowLightTicks > 1200) { // Duplicado tiempo antes de penalizar (de 600 a 1200)
                        satisfactionScore = Math.max(0, satisfactionScore - 1);
                        lowLightTicks = 0;
                    }
                } else {
                    lowLightTicks = 0;
                }

                // Littering logic
                if (trashCooldown > 0) {
                    trashCooldown -= 100;
                } else if (getVisitorState() == VisitorState.WANDERING && this.random.nextFloat() < 0.05f) {
                    // Check height Y=68 (+/- 1.0 buffer for safety)
                    if (Math.abs(this.getY() - 68.0) < 1.0) {
                        // 5% chance every 5 seconds if wandering
                        if (this.level() instanceof ServerLevel) {
                            ServerLevel serverLevel = (ServerLevel) this.level();
                            TrashEntity trash = new TrashEntity(ModEntities.TRASH.get(), serverLevel);
                            trash.setPos(this.getX(), this.getY(), this.getZ());
                            if (serverLevel.addFreshEntity(trash)) {
                                trashCooldown = 12000; // 10 minutes approx between littering
                            }
                        }
                    }
                }

                AABB nearby = this.getBoundingBox().inflate(3.0D);
                java.util.List<VisitorEntity> othersNearby = this.level().getEntitiesOfClass(VisitorEntity.class,
                        nearby);
                if (othersNearby.size() > 8) { // Subido límite de aglomeración de 5 a 8
                    satisfactionScore = Math.max(0, satisfactionScore - 1);
                }

                if (getTargetArea() >= 0) {
                    VisitorsSavedData data = VisitorsSavedData.get((ServerLevel) this.level());
                    VisitorsSavedData.Area currentArea = data.getArea(getTargetArea());
                    if (currentArea.isValid()) {
                        long areaSize = currentArea.getPlaneArea();
                        if (cachedAreaAABB == null)
                            updateAreaBounds();

                        // Area Factors (Strict & Just)
                        java.util.List<VisitorEntity> allInArea = this.level().getEntitiesOfClass(VisitorEntity.class,
                                cachedAreaAABB);
                        double spacePerPerson = (double) areaSize / Math.max(1, allInArea.size());

                        // Height Factor
                        double height = Math.abs(currentArea.pos1.getY() - currentArea.pos2.getY()) + 1;
                        if (height < 3.0) {
                            satisfactionScore = Math.max(0, satisfactionScore - 1); // Penalize low ceiling
                        } else if (height > 5.0) {
                            if (this.random.nextFloat() < 0.05f)
                                satisfactionScore = Math.min(5, satisfactionScore + 1);
                        }

                        // Cleanliness Factor (Trash nearby)
                        AABB trashCheck = this.getBoundingBox().inflate(5.0D);
                        java.util.List<TrashEntity> trashNearby = this.level().getEntitiesOfClass(TrashEntity.class,
                                trashCheck);
                        if (!trashNearby.isEmpty()) {
                            satisfactionScore = Math.max(0, satisfactionScore - 2); // Heavy penalty for trash
                        }

                        // Space Factor
                        if (spacePerPerson < 5.0) {
                            if (this.random.nextFloat() < 0.15f)
                                satisfactionScore = Math.max(0, satisfactionScore - 1);
                        } else if (spacePerPerson > 20.0) {
                            if (this.random.nextFloat() < 0.1f)
                                satisfactionScore = Math.min(5, satisfactionScore + 1);
                        }

                        // Comfort Bonus (Sitting)
                        if (getVisitorState() == VisitorState.SITTING) {
                            if (this.random.nextFloat() < 0.2f) { // 20% chance every 5s
                                satisfactionScore = Math.min(5, satisfactionScore + 1);
                            }
                        }
                    }
                }
            }

            // Hunger Logic
            if ((getVisitorState() == VisitorState.WANDERING || getVisitorState() == VisitorState.SITTING)
                    && !isEscaping()) {
                if (!isHungry()) {
                    if (this.random.nextFloat() < 0.0005f) {
                        setHungry(true);
                        hungerTimer = 0;
                        deliveryTimer = 0;
                    }
                } else {
                    hungerTimer++;
                    deliveryTimer++;
                    if (hungerTimer > 1800) {
                        setHungry(false);
                        satisfactionScore = 0;
                        if (this.level() instanceof ServerLevel) {
                            ServerLevel serverLevel = (ServerLevel) this.level();
                            serverLevel.getServer().getPlayerList()
                                    .broadcastSystemMessage(Component.literal(getDisplayName().getString() + ": ")
                                            .append(Component
                                                    .literal("¡Qué servicio más LAMENTABLE! ¡Me voy a otra pizzería!")
                                                    .withStyle(ChatFormatting.RED)),
                                            false);
                        }
                        forceLeave();
                    }
                }
            }

            if (getVisitorState() == VisitorState.WANDERING || getVisitorState() == VisitorState.SITTING) {
                ticksInArea++;
                if (ticksInArea >= stayTime) {
                    setShouldLeave(true);
                    setVisitorState(VisitorState.LEAVING);
                }
            }

            long dayTime = this.level().getDayTime() % 24000;
            if (dayTime >= 12500 && dayTime <= 23500) {
                if (getVisitorState() != VisitorState.LEAVING) {
                    setShouldLeave(true);
                    setVisitorState(VisitorState.LEAVING);
                }
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isEscaping())
            return false;
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isEscaping() && source.getEntity() instanceof Player && !hasGivenReward) {
            giveRewardAndRemove((Player) source.getEntity());
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isKiller()) {
            killerClicks++;
            int remaining = 3 - killerClicks;

            if (remaining > 0) {
                player.displayClientMessage(
                        Component.literal(
                                "§c§l¡KILLER DETECTADO! §fTe quedan §e" + remaining + " §fclics para derrotarlo."),
                        true);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                        SoundSource.NEUTRAL, 1.0f, 1.5f);
            } else {
                player.displayClientMessage(
                        Component.literal("§a§l¡KILLER DERROTADO! §fHas ganado §e10 Estrellas §fy §6Reputación."),
                        true);
                if (this.level() instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) this.level();
                    VisitorsSavedData data = VisitorsSavedData.get(serverLevel);

                    // Add 10 stars (ratingSum += 10, ratingCount++)
                    data.addReview(10, serverLevel);
                    // Add massive reputation bonus
                    data.addReputationBonus(0.5f);

                    // Reward sound and particles
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, this.getX(),
                            this.getY() + 1, this.getZ(), 50, 0.5, 0.5, 0.5, 0.1);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP,
                            SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }

        if (isEscaping() && !hasGivenReward) {
            giveRewardAndRemove(player);
            return InteractionResult.SUCCESS;
        }

        if (isHungry()) {
            if (player.getItemInHand(hand).getItem() == ModItems.PLATE_OF_FOOD.get()) {
                if (!this.level().isClientSide) {
                    if (!player.isCreative())
                        player.getItemInHand(hand).shrink(1);
                    // MASSIVE satisfaction boost when fed - guarantees good review
                    satisfactionScore = 5; // Always max satisfaction when fed!
                    wasFed = true; // This ensures minimum 4 stars on final review
                    setHungry(false);
                    hungerTimer = 0;
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EAT,
                            SoundSource.NEUTRAL, 1.0f, 1.0f);
                    giveTip(player);
                    player.sendSystemMessage(Component.literal("§a¡Gracias! ¡Estaba delicioso!"));
                    // After eating, try to sit again
                    setVisitorState(VisitorState.WANDERING);
                }
                return InteractionResult.SUCCESS;
            }
        } else if (player.getItemInHand(hand).getItem() == ModItems.PLATE_OF_FOOD.get()) {
            if (!this.level().isClientSide) {
                satisfactionScore = Math.max(0, satisfactionScore - 1);
                player.sendSystemMessage(Component.literal("§c¡No tengo hambre! No me molestes con comida."));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void giveRewardAndRemove(Player player) {
        if (this.level().isClientSide)
            return;
        hasGivenReward = true;
        if (this.level() instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            String command = "give " + player.getName().getString() + " management_wanted:faz_buck 5";
            serverLevel.getServer().getCommands().performPrefixedCommand(
                    serverLevel.getServer().createCommandSourceStack().withSuppressedOutput(), command);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                serverPlayer.sendSystemMessage(Component.literal("§a¡Capturaste a un escapado! +5 FazBucks"));
                serverPlayer.sendSystemMessage(
                        Component.literal("§6¡Tu reputación ha subido! Los próximos clientes serán más generosos."));
            }
            VisitorsSavedData.get(serverLevel).addReputationBonus(0.25f);
        }
        this.discard();
    }

    private void giveTip(Player player) {
        if (this.level() instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            String command = "give " + player.getName().getString() + " management_wanted:faz_buck 1";
            serverLevel.getServer().getCommands().performPrefixedCommand(
                    serverLevel.getServer().createCommandSourceStack().withSuppressedOutput(), command);
        }
    }

    private void updateAreaBounds() {
        if (this.level() instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            VisitorsSavedData data = VisitorsSavedData.get(serverLevel);
            VisitorsSavedData.Area area = data.getArea(getTargetArea());
            if (area.isValid()) {
                BlockPos pos1 = area.pos1;
                BlockPos pos2 = area.pos2;
                areaMin = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()),
                        Math.min(pos1.getZ(), pos2.getZ()));
                areaMax = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()),
                        Math.max(pos1.getZ(), pos2.getZ()));
                cachedAreaAABB = new AABB(areaMin, areaMax.offset(1, 1, 1));
            }
        }
    }

    public VisitorState getVisitorState() {
        return VisitorState.values()[this.entityData.get(STATE)];
    }

    public void setVisitorState(VisitorState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public int getTargetArea() {
        return this.entityData.get(TARGET_AREA);
    }

    public void setTargetArea(int index) {
        this.entityData.set(TARGET_AREA, index);
        this.areaMin = null;
        this.areaMax = null;
    }

    public boolean isInArea() {
        if (cachedAreaAABB == null)
            updateAreaBounds();
        return cachedAreaAABB != null && cachedAreaAABB.contains(this.position());
    }

    public boolean shouldLeave() {
        return shouldLeave;
    }

    public void setShouldLeave(boolean leave) {
        this.shouldLeave = leave;
    }

    public BlockPos getAreaMin() {
        return areaMin;
    }

    public BlockPos getAreaMax() {
        return areaMax;
    }

    public BlockPos getRandomPositionInArea() {
        if (areaMin == null || areaMax == null)
            return null;
        int x = this.getRandom().nextInt(areaMax.getX() - areaMin.getX() + 1) + areaMin.getX();
        int z = this.getRandom().nextInt(areaMax.getZ() - areaMin.getZ() + 1) + areaMin.getZ();
        int y = areaMin.getY();
        for (int checkY = areaMax.getY(); checkY >= areaMin.getY(); checkY--) {
            BlockPos checkPos = new BlockPos(x, checkY, z);
            if (!this.level().getBlockState(checkPos).isAir() && this.level().getBlockState(checkPos.above()).isAir()) {
                y = checkY + 1;
                break;
            }
        }
        return new BlockPos(x, y, z);
    }

    public void forceLeave() {
        setShouldLeave(true);
        setVisitorState(VisitorState.LEAVING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VisitorState", getVisitorState().ordinal());
        tag.putInt("StayTime", stayTime);
        tag.putInt("TicksInArea", ticksInArea);
        tag.putBoolean("ShouldLeave", shouldLeave);
        tag.putInt("Variant", getVariant());
        tag.putInt("TargetArea", getTargetArea());
        tag.putBoolean("IsEscaping", isEscaping());
        tag.putBoolean("IsBabyVisitor", isBaby());
        tag.putBoolean("IsBirthdayParty", isBirthdayParty());
        tag.putBoolean("HasGivenReward", hasGivenReward);
        tag.putBoolean("IsHungry", isHungry());
        tag.putInt("HungerTimer", hungerTimer);
        tag.putInt("SatisfactionScore", satisfactionScore);
        tag.putBoolean("IsThief", isThief());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVisitorState(VisitorState.values()[tag.getInt("VisitorState")]);
        stayTime = tag.getInt("StayTime");
        ticksInArea = tag.getInt("TicksInArea");
        shouldLeave = tag.getBoolean("ShouldLeave");
        if (tag.contains("Variant"))
            this.entityData.set(VARIANT, tag.getInt("Variant"));
        if (tag.contains("TargetArea"))
            this.entityData.set(TARGET_AREA, tag.getInt("TargetArea"));
        if (tag.contains("IsEscaping"))
            this.entityData.set(IS_ESCAPING, tag.getBoolean("IsEscaping"));
        if (tag.contains("IsBabyVisitor"))
            this.entityData.set(IS_BABY_VISITOR, tag.getBoolean("IsBabyVisitor"));
        if (tag.contains("IsBirthdayParty"))
            this.entityData.set(IS_BIRTHDAY_PARTY, tag.getBoolean("IsBirthdayParty"));
        if (tag.contains("HasGivenReward"))
            this.hasGivenReward = tag.getBoolean("HasGivenReward");
        if (tag.contains("IsHungry"))
            setHungry(tag.getBoolean("IsHungry"));
        if (tag.contains("HungerTimer"))
            hungerTimer = tag.getInt("HungerTimer");
        if (tag.contains("SatisfactionScore"))
            satisfactionScore = tag.getInt("SatisfactionScore");
        if (tag.contains("IsThief"))
            setThief(tag.getBoolean("IsThief"));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public boolean isEscaping() {
        return this.entityData.get(IS_ESCAPING);
    }

    public void setEscaping(boolean escaping) {
        this.entityData.set(IS_ESCAPING, escaping);
    }

    public void setBaby(boolean baby) {
        this.entityData.set(IS_BABY_VISITOR, baby);
    }

    public boolean isBirthdayParty() {
        return this.entityData.get(IS_BIRTHDAY_PARTY);
    }

    public void setBirthdayParty(boolean birthdayParty) {
        this.entityData.set(IS_BIRTHDAY_PARTY, birthdayParty);
    }

    public void startEscaping() {
        setHungry(false);
        setEscaping(true);
        setVisitorState(VisitorState.ESCAPING);
    }

    public boolean isHungry() {
        return this.entityData.get(IS_HUNGRY);
    }

    public void setHungry(boolean hungry) {
        this.entityData.set(IS_HUNGRY, hungry);
    }

    public boolean isThief() {
        return this.entityData.get(IS_THIEF);
    }

    public void setThief(boolean thief) {
        this.entityData.set(IS_THIEF, thief);
    }

    public boolean isKiller() {
        return this.entityData.get(IS_KILLER);
    }

    public void setKiller(boolean killer) {
        this.entityData.set(IS_KILLER, killer);
        if (killer)
            this.setHungry(false);
    }

    public int getSatisfactionScore() {
        return satisfactionScore;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.isPassenger() && this.getVehicle() instanceof ArmorStand) {
            this.getVehicle().discard();
        }
        if (!this.level().isClientSide && reason == RemovalReason.DISCARDED && !isEscaping() && !isKiller()) {
            if (this.level() instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel) this.level();
                int finalScore = Math.max(0, Math.min(5, satisfactionScore));
                // If visitor was fed, guarantee minimum 4 stars
                if (wasFed && finalScore < 4) {
                    finalScore = 4;
                }
                float bonus = VisitorsSavedData.get(serverLevel).getReputationBonus();
                if (bonus >= 0.1f) {
                    if (finalScore < 5 && this.random.nextFloat() < bonus) {
                        finalScore++;
                        VisitorsSavedData.get(serverLevel).consumeReputationBonus(0.15f);
                    }
                }
                VisitorsSavedData.get(serverLevel).addReview(finalScore, serverLevel);
                if (finalScore <= 2 || finalScore >= 4) {
                    String msg = ReviewTextPool.getReview(finalScore);
                    ChatFormatting color = finalScore <= 2 ? ChatFormatting.RED : ChatFormatting.GREEN;
                    if (bonus >= 0.5f && finalScore >= 4)
                        msg = "§6[Seguridad] §f" + msg;
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§7[Review] ")
                                    .append(Component.literal(getDisplayName().getString() + ": " + msg)
                                            .withStyle(color))
                                    .append(Component.literal(" (" + finalScore + "★)")),
                            false);
                }
            }
        }
        super.remove(reason);
    }

    public void doJump() {
        super.jumpFromGround();
    }
}
