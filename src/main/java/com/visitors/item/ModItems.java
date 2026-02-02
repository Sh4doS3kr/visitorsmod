package com.visitors.item;

import com.visitors.VisitorsMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            VisitorsMod.MOD_ID);

    public static final RegistryObject<Item> PLATE_OF_FOOD = ITEMS.register("plate_of_food",
            () -> new PlateOfFoodItem());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
