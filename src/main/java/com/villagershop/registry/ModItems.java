package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.item.GuideItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, ShopMod.MOD_ID);

    public static final DeferredHolder<Item, Item> SHOP_COUNTER_ITEM = ITEMS.register("shop_counter",
            () -> new BlockItem(ModBlocks.SHOP_COUNTER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> GUIDE = ITEMS.register("guide",
            () -> new GuideItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> SALES_LEDGER_ITEM = ITEMS.register("sales_ledger",
            () -> new BlockItem(ModBlocks.SALES_LEDGER.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
