package com.villagershop;

import com.mojang.logging.LogUtils;
import com.villagershop.registry.ModBlockEntities;
import com.villagershop.registry.ModBlocks;
import com.villagershop.registry.ModCreativeTabs;
import com.villagershop.registry.ModItems;
import com.villagershop.registry.ModMenus;
import com.villagershop.registry.ModVillagers;
import com.villagershop.config.ShopConfigSpec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
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

    public ShopMod(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModVillagers.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(com.villagershop.network.ModNetwork::register);
        // NB : pas de capability item handler exposée -> les hoppers/tuyaux ne peuvent
        // PAS interagir avec le comptoir (problèmes de propriété). Peut-être un jour
        // via un upgrade dédié ; le CappedItemHandler est conservé en sommeil pour ça.

        // Config : config/villagershop-common.toml, recopiée dans VSConfig à chaque (re)chargement.
        container.registerConfig(ModConfig.Type.COMMON, ShopConfigSpec.SPEC, "villagershop-common.toml");
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);

        // Les handlers d'événements de jeu (interaction villageois) vont sur le bus NeoForge.
        NeoForge.EVENT_BUS.register(new com.villagershop.event.VillagerInteractHandler());
        NeoForge.EVENT_BUS.register(new com.villagershop.event.ConfigSyncHandler());
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ShopConfigSpec.SPEC) ShopConfigSpec.applyToRuntime();
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ShopConfigSpec.SPEC) ShopConfigSpec.applyToRuntime();
    }

}
