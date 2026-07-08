package com.villagershop.client;

import com.villagershop.network.SyncAllowedPacket;
import com.villagershop.network.SyncOffersPacket;
import com.villagershop.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.EntityType;

public class ClientSetup implements ClientModInitializer {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.SHOP_CONFIG, ShopConfigScreen::new);
        MenuScreens.register(ModMenus.SHOP_TRADE, ShopTradeScreen::new);
        MenuScreens.register(ModMenus.SHOP_STOCK, ShopStockScreen::new);
        MenuScreens.register(ModMenus.SHOP_MERCHANT, MerchantScreen::new);

        // Receveurs S2C : les handlers Fabric tournent sur le thread client.
        ClientPlayNetworking.registerGlobalReceiver(SyncAllowedPacket.TYPE, (msg, ctx) -> SyncAllowedPacket.handleClient(msg));
        ClientPlayNetworking.registerGlobalReceiver(SyncOffersPacket.TYPE, (msg, ctx) -> SyncOffersPacket.handleClient(msg));

        // Couche qui masque le badge de niveau du villageois "shopkeeper"
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (entityType == EntityType.VILLAGER && renderer instanceof LivingEntityRenderer living) {
                helper.register(new ShopBadgeCoverLayer((RenderLayerParent) living));
            }
        });
    }
}
