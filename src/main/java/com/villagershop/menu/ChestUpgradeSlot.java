package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.world.item.ItemStack;
import com.villagershop.config.VSConfig;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Slot coffre d'amélioration (coffres uniquement, max 8), masquable selon l'onglet. */
public class ChestUpgradeSlot extends SlotItemHandler {
    private boolean visible = true;

    public ChestUpgradeSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public boolean isActive() { return visible; }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return VSConfig.isEnabled(VSConfig.Upgrade.STORAGE) && stack.is(VSConfig.item(VSConfig.Upgrade.STORAGE));
    }

    @Override
    public int getMaxStackSize() { return Math.max(1, VSConfig.maxChests()); }

    @Override
    public int getMaxStackSize(ItemStack stack) { return getMaxStackSize(); }
}
