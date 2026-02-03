package com.visitors.entity;

import com.visitors.VisitorsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
        public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
                        ForgeRegistries.ENTITY_TYPES,
                        VisitorsMod.MOD_ID);

        public static final RegistryObject<EntityType<VisitorEntity>> VISITOR = ENTITIES.register("visitor",
                        () -> EntityType.Builder.of(VisitorEntity::new, MobCategory.CREATURE)
                                        .sized(0.6F, 1.95F) // Tamaño estándar adulto
                                        .clientTrackingRange(10)
                                        .build(new ResourceLocation(VisitorsMod.MOD_ID, "visitor").toString()));

        public static final RegistryObject<EntityType<TrashEntity>> TRASH = ENTITIES.register("trash",
                        () -> EntityType.Builder.of(TrashEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                                        .sized(0.5F, 0.2F)
                                        .clientTrackingRange(10)
                                        .build(new ResourceLocation(VisitorsMod.MOD_ID, "trash").toString()));

        public static final RegistryObject<EntityType<ContractorEntity>> CONTRACTOR = ENTITIES.register("contractor",
                        () -> EntityType.Builder.of(ContractorEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                                        .sized(0.6F, 1.95F)
                                        .clientTrackingRange(10)
                                        .build(new ResourceLocation(VisitorsMod.MOD_ID, "contractor").toString()));

        public static final RegistryObject<EntityType<InspectorEntity>> INSPECTOR = ENTITIES.register("inspector",
                        () -> EntityType.Builder.of(InspectorEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                                        .sized(0.6F, 1.95F)
                                        .clientTrackingRange(10)
                                        .build(new ResourceLocation(VisitorsMod.MOD_ID, "inspector").toString()));

        public static final RegistryObject<EntityType<CleanerEntity>> CLEANER = ENTITIES.register("cleaner",
                        () -> EntityType.Builder.of(CleanerEntity::new, net.minecraft.world.entity.MobCategory.MISC)
                                        .sized(0.6F, 1.95F)
                                        .clientTrackingRange(10)
                                        .build(new ResourceLocation(VisitorsMod.MOD_ID, "cleaner").toString()));
}
