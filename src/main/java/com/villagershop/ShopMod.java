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
import net.fabricmc.api.ModInitializer;
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
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenus.register();
        ModVillagers.register();
        ModCreativeTabs.register();
        ModNetwork.register();
        VillagerInteractHandler.register();
    }
}
