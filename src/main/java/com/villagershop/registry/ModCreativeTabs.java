package com.villagershop.registry;

import com.villagershop.ShopMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static void register() {
        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.villagershop"))
                .icon(() -> new ItemStack(ModItems.SHOP_COUNTER_ITEM))
                .displayItems((params, output) -> {
                    output.accept(ModItems.SHOP_COUNTER_ITEM);
                    output.accept(ModItems.SALES_LEDGER_ITEM);
                    output.accept(ModItems.GUIDE);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "main"), tab);
    }
}
