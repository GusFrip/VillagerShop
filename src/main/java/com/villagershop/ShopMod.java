package com.villagershop;

import com.mojang.logging.LogUtils;
import com.villagershop.event.VillagerInteractHandler;
import com.villagershop.network.ModNetwork;
import com.villagershop.registry.ModBlockEntities;
import com.villagershop.registry.ModBlocks;
import com.villagershop.registry.ModCreativeTabs;
import com.villagershop.registry.ModItems;
import com.villagershop.registry.ModMenus;
import com.villagershop.registry.ModVillagers;
import com.villagershop.config.ShopConfigSpec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;

/**
 * Point d'entrée du mod VillagerShop (Fabric).
 * Même principe que les versions Forge/NeoForge : un bloc "Comptoir de marchand"
 * stocke la config d'une boutique, un villageois le réclame comme poste de travail
 * et sert de vitrine. NB : pas d'accès hopper/tuyau (choix volontaire).
 */
public class ShopMod implements ModInitializer {
    public static final String MOD_ID = "villagershop";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        // Config : config/villagershop-common.json, relue au démarrage de chaque serveur (solo compris).
        ShopConfigSpec.applyToRuntime();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ShopConfigSpec.applyToRuntime());

        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenus.register();
        ModVillagers.register();
        ModCreativeTabs.register();
        ModNetwork.register();
        VillagerInteractHandler.register();
        com.villagershop.event.ConfigSyncHandler.register();
    }
}
