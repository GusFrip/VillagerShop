package com.villagershop.block;

import com.villagershop.util.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * EN SOMMEIL (non branché) : vue du stock limitée aux emplacements actifs.
 * L'accès hopper/tuyau au comptoir est volontairement désactivé (problèmes de
 * propriété) — à réactiver un jour via un upgrade dédié si la communauté le
 * demande (côté Fabric, il faudra alors l'adapter à la Transfer API).
 */
public class CappedItemHandler {
    private final ShopBlockEntity be;
    private final ItemStackHandler delegate;

    public CappedItemHandler(ShopBlockEntity be, ItemStackHandler delegate) {
        this.be = be;
        this.delegate = delegate;
    }

    public int getSlots() {
        return be.getActiveCapacity();
    }

    public @NotNull ItemStack getStackInSlot(int slot) {
        return slot < be.getActiveCapacity() ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot >= be.getActiveCapacity()) return stack;
        return delegate.insertItem(slot, stack, simulate);
    }

    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= be.getActiveCapacity()) return ItemStack.EMPTY;
        return delegate.extractItem(slot, amount, simulate);
    }

    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot < be.getActiveCapacity() && delegate.isItemValid(slot, stack);
    }
}
