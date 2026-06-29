package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.item.GuideItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ShopMod.MOD_ID);

    public static final RegistryObject<Item> SHOP_COUNTER_ITEM = ITEMS.register("shop_counter",
            () -> new BlockItem(ModBlocks.SHOP_COUNTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> GUIDE = ITEMS.register("guide",
            () -> new GuideItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
