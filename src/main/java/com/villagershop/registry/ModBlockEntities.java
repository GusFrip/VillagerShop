package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ShopMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ShopBlockEntity>> SHOP_COUNTER =
            BLOCK_ENTITIES.register("shop_counter",
                    () -> BlockEntityType.Builder.of(ShopBlockEntity::new, ModBlocks.SHOP_COUNTER.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
