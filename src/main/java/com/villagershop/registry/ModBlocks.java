package com.villagershop.registry;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, ShopMod.MOD_ID);

    public static final DeferredHolder<Block, Block> SHOP_COUNTER = BLOCKS.register("shop_counter",
            () -> new ShopBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final DeferredHolder<Block, Block> SALES_LEDGER = BLOCKS.register("sales_ledger",
            () -> new com.villagershop.block.LedgerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
