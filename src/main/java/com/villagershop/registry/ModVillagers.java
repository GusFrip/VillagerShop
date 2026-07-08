package com.villagershop.registry;

import com.google.common.collect.ImmutableSet;
import com.villagershop.ShopMod;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * POI sur le bloc Comptoir + profession "shopkeeper" associée. Fabric :
 * PointOfInterestHelper gère l'enregistrement du mapping blockstate -> POI.
 */
public class ModVillagers {
    public static final ResourceLocation SHOPKEEPER_POI_ID =
            ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shopkeeper");
    public static final ResourceKey<PoiType> SHOPKEEPER_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, SHOPKEEPER_POI_ID);

    public static PoiType SHOPKEEPER_POI;
    public static VillagerProfession SHOPKEEPER;

    public static void register() {
        SHOPKEEPER_POI = PointOfInterestHelper.register(SHOPKEEPER_POI_ID, 1, 1, ModBlocks.SHOP_COUNTER);
        SHOPKEEPER = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shopkeeper"),
                new VillagerProfession(
                        "shopkeeper",
                        holder -> holder.is(SHOPKEEPER_POI_KEY),
                        holder -> holder.is(SHOPKEEPER_POI_KEY),
                        ImmutableSet.<Item>of(),
                        ImmutableSet.<Block>of(),
                        SoundEvents.VILLAGER_WORK_CARTOGRAPHER));
    }
}
