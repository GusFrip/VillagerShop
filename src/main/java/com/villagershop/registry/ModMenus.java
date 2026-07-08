package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.menu.ShopConfigMenu;
import com.villagershop.menu.ShopStockMenu;
import com.villagershop.menu.ShopMerchantMenu;
import com.villagershop.menu.ShopTradeMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static MenuType<ShopConfigMenu> SHOP_CONFIG;
    public static MenuType<ShopTradeMenu> SHOP_TRADE;
    public static MenuType<ShopStockMenu> SHOP_STOCK;
    public static MenuType<ShopMerchantMenu> SHOP_MERCHANT;

    public static void register() {
        SHOP_CONFIG = Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shop_config"),
                new ExtendedScreenHandlerType<>(ShopConfigMenu::new, ShopConfigMenu.OpenData.STREAM_CODEC));
        SHOP_TRADE = Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shop_trade"),
                new ExtendedScreenHandlerType<>(ShopTradeMenu::new, BlockPos.STREAM_CODEC));
        SHOP_STOCK = Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shop_stock"),
                new ExtendedScreenHandlerType<>(ShopStockMenu::new, BlockPos.STREAM_CODEC));
        SHOP_MERCHANT = Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shop_merchant"),
                new MenuType<>((id, inv) -> new ShopMerchantMenu(id, inv), FeatureFlags.DEFAULT_FLAGS));
    }
}
