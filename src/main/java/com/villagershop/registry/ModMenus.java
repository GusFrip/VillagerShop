package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.menu.ShopConfigMenu;
import com.villagershop.menu.ShopStockMenu;
import com.villagershop.menu.ShopTradeMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ShopMod.MOD_ID);

    public static final RegistryObject<MenuType<ShopConfigMenu>> SHOP_CONFIG =
            MENUS.register("shop_config", () -> IForgeMenuType.create(ShopConfigMenu::new));

    public static final RegistryObject<MenuType<ShopTradeMenu>> SHOP_TRADE =
            MENUS.register("shop_trade", () -> IForgeMenuType.create(ShopTradeMenu::new));

    public static final RegistryObject<MenuType<ShopStockMenu>> SHOP_STOCK =
            MENUS.register("shop_stock", () -> IForgeMenuType.create(ShopStockMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
