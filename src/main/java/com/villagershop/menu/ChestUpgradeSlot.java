package com.villagershop.menu;

import com.villagershop.util.ItemStackHandler;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.villagershop.util.SlotItemHandler;

/** Slot coffre d'amélioration (coffres uniquement, max 8), masquable selon l'onglet. */
public class ChestUpgradeSlot extends SlotItemHandler {
    private boolean visible = true;

    public ChestUpgradeSlot(ItemStackHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    public void setVisible(boolean v) { this.visible = v; }

    @Override
    public boolean isActive() { return visible; }

    @Override
    public boolean mayPlace(ItemStack stack) { return stack.is(Items.CHEST); }

    @Override
    public int getMaxStackSize() { return ShopBlockEntity.MAX_CHESTS; }

    @Override
    public int getMaxStackSize(ItemStack stack) { return ShopBlockEntity.MAX_CHESTS; }
}
