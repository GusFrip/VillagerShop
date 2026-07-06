package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Slot blocs d'émeraude (max 7) : chaque bloc débloque une offre supplémentaire. */
public class TradeUpgradeSlot extends SlotItemHandler {
    private boolean visible = true;

    public TradeUpgradeSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public boolean isActive() { return visible; }

    @Override
    public boolean mayPlace(ItemStack stack) { return stack.is(Items.EMERALD_BLOCK); }

    @Override
    public int getMaxStackSize() { return ShopBlockEntity.MAX_OFFERS - 1; }

    @Override
    public int getMaxStackSize(ItemStack stack) { return ShopBlockEntity.MAX_OFFERS - 1; }
}
