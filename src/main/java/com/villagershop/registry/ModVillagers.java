package com.villagershop.registry;

import com.google.common.collect.ImmutableSet;
import com.villagershop.ShopMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

/**
 * Enregistre un Point d'Intérêt (POI) sur le bloc Comptoir et une profession
 * de villageois "shopkeeper" associée à ce POI. Comme pour un pupitre qui crée
 * un bibliothécaire, un villageois adulte sans emploi proche d'un comptoir va
 * le réclamer comme poste de travail et devenir la vitrine de la boutique.
 */
public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ShopMod.MOD_ID);

    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, ShopMod.MOD_ID);

    public static final RegistryObject<PoiType> SHOPKEEPER_POI = POI_TYPES.register("shopkeeper",
            () -> new PoiType(blockStates(ModBlocks.SHOP_COUNTER.get()), 1, 1));

    public static final RegistryObject<VillagerProfession> SHOPKEEPER = PROFESSIONS.register("shopkeeper",
            () -> new VillagerProfession(
                    "shopkeeper",
                    holder -> holder.is(SHOPKEEPER_POI.getKey()),   // poste de travail détenu
                    holder -> holder.is(SHOPKEEPER_POI.getKey()),   // poste de travail acquérable
                    ImmutableSet.<Item>of(),
                    ImmutableSet.<Block>of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    private static Set<BlockState> blockStates(Block block) {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
