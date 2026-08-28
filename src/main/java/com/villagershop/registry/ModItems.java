package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.item.GuideItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item SHOP_COUNTER_ITEM = new BlockItem(ModBlocks.SHOP_COUNTER, new Item.Properties());
    public static final Item SALES_LEDGER_ITEM = new BlockItem(ModBlocks.SALES_LEDGER, new Item.Properties());
    public static final Item GUIDE = new GuideItem(new Item.Properties().stacksTo(1));

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shop_counter"), SHOP_COUNTER_ITEM);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "sales_ledger"), SALES_LEDGER_ITEM);
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "guide"), GUIDE);
    }
}
