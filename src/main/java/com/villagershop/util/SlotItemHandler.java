package com.villagershop.util;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot branché sur notre ItemStackHandler (équivalent du SlotItemHandler NeoForge). */
public class SlotItemHandler extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final ItemStackHandler handler;
    private final int index;

    public SlotItemHandler(ItemStackHandler handler, int index, int x, int y) {
        super(EMPTY, index, x, y);
        this.handler = handler;
        this.index = index;
    }

    public ItemStackHandler getItemHandler() { return handler; }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && handler.isItemValid(index, stack);
    }

    @Override
    public ItemStack getItem() {
        return handler.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        handler.setStackInSlot(index, stack);
        setChanged();
    }

    @Override
    public void setChanged() {}

    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(index);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !handler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return handler.extractItem(index, amount, false);
    }
}
