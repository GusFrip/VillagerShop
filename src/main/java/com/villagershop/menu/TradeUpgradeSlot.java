package com.villagershop.menu;

import com.villagershop.util.ItemStackHandler;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.world.item.ItemStack;
import com.villagershop.config.VSConfig;
import com.villagershop.util.ItemStackHandler;
import com.villagershop.util.SlotItemHandler;

/** Slot blocs d'émeraude (max 7) : chaque bloc débloque une offre supplémentaire. */
public class TradeUpgradeSlot extends SlotItemHandler {
    private boolean visible = true;

    public TradeUpgradeSlot(ItemStackHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public boolean isActive() { return visible; }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return VSConfig.maxTradeBlocks() > 0 && stack.is(VSConfig.item(VSConfig.Upgrade.TRADES));
    }

    @Override
    public int getMaxStackSize() { return Math.max(1, VSConfig.maxTradeBlocks()); }

    @Override
    public int getMaxStackSize(ItemStack stack) { return getMaxStackSize(); }
}
