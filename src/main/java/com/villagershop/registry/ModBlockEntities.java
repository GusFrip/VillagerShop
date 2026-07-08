package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<ShopBlockEntity> SHOP_COUNTER;

    public static void register() {
        SHOP_COUNTER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "shop_counter"),
                BlockEntityType.Builder.of(ShopBlockEntity::new, ModBlocks.SHOP_COUNTER).build(null));
    }
}
