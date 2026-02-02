package com.visitors.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class PlateOfFoodItem extends Item {
    public PlateOfFoodItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }
}
