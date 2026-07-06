package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.menu.ShopConfigMenu;
import com.villagershop.menu.ShopStockMenu;
import com.villagershop.menu.ShopMerchantMenu;
import com.villagershop.menu.ShopTradeMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, ShopMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShopConfigMenu>> SHOP_CONFIG =
            MENUS.register("shop_config", () -> IMenuTypeExtension.create(ShopConfigMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ShopTradeMenu>> SHOP_TRADE =
            MENUS.register("shop_trade", () -> IMenuTypeExtension.create(ShopTradeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ShopStockMenu>> SHOP_STOCK =
            MENUS.register("shop_stock", () -> IMenuTypeExtension.create(ShopStockMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ShopMerchantMenu>> SHOP_MERCHANT =
            MENUS.register("shop_merchant", () -> new MenuType<>(
                    (id, inv) -> new ShopMerchantMenu(id, inv), FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
