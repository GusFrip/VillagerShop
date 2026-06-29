package com.villagershop.block;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Vue capability du stock qui ne révèle que les emplacements actifs (selon le
 * nombre de coffres installés). Empêche hoppers/tuyaux de remplir au-delà de la
 * capacité réelle.
 */
public class CappedItemHandler implements IItemHandler {
    private final ShopBlockEntity be;
    private final ItemStackHandler delegate;

    public CappedItemHandler(ShopBlockEntity be, ItemStackHandler delegate) {
        this.be = be;
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return be.getActiveCapacity();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return slot < be.getActiveCapacity() ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot >= be.getActiveCapacity()) return stack;
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= be.getActiveCapacity()) return ItemStack.EMPTY;
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot < be.getActiveCapacity() && delegate.isItemValid(slot, stack);
    }
}
