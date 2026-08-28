package com.villagershop.registry;

import com.villagershop.ShopMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShopMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.villagershop"))
                    .icon(() -> new ItemStack(ModItems.SHOP_COUNTER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SHOP_COUNTER_ITEM.get());
                        output.accept(ModItems.SALES_LEDGER_ITEM.get());
                        output.accept(ModItems.GUIDE.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
