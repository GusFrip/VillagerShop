package com.villagershop.client;

import com.villagershop.ShopMod;
import com.villagershop.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ShopMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SHOP_CONFIG.get(), ShopConfigScreen::new);
            MenuScreens.register(ModMenus.SHOP_TRADE.get(), ShopTradeScreen::new);
            MenuScreens.register(ModMenus.SHOP_STOCK.get(), ShopStockScreen::new);
        });
    }
}
