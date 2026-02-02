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
}
