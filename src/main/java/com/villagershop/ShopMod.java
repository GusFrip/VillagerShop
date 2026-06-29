package com.villagershop;

import com.mojang.logging.LogUtils;
import com.villagershop.network.ModNetwork;
import com.villagershop.registry.ModBlockEntities;
import com.villagershop.registry.ModBlocks;
import com.villagershop.registry.ModCreativeTabs;
import com.villagershop.registry.ModItems;
import com.villagershop.registry.ModMenus;
import com.villagershop.registry.ModVillagers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Point d'entrée du mod VillagerShop.
 *
 * Principe : un bloc "Comptoir de marchand" (ShopBlock) stocke la config d'une
 * boutique dans son BlockEntity (propriétaire, joueurs autorisés, offres, stock).
 * Un villageois sans emploi proche du comptoir le réclame comme poste de travail
 * (POI + profession custom) et devient la "vitrine" : les clients cliquent dessus
 * pour acheter/vendre selon les offres définies. Seul le propriétaire (ou les
 * joueurs qu'il autorise) peut ouvrir l'interface de configuration du comptoir.
 */
@Mod(ShopMod.MOD_ID)
public class ShopMod {
    public static final String MOD_ID = "villagershop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ShopMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModVillagers.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(this::commonSetup);

        // Les handlers d'événements de jeu (interaction villageois) vont sur le bus Forge.
        MinecraftForge.EVENT_BUS.register(new com.villagershop.event.VillagerInteractHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
