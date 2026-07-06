package com.villagershop.client;

import com.villagershop.ShopMod;
import com.villagershop.registry.ModMenus;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ShopMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer renderer = (LivingEntityRenderer) event.getRenderer(EntityType.VILLAGER);
        if (renderer != null) {
            renderer.addLayer(new ShopBadgeCoverLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SHOP_CONFIG.get(), ShopConfigScreen::new);
        event.register(ModMenus.SHOP_TRADE.get(), ShopTradeScreen::new);
        event.register(ModMenus.SHOP_STOCK.get(), ShopStockScreen::new);
        event.register(ModMenus.SHOP_MERCHANT.get(), MerchantScreen::new);
    }
}
