package com.villagershop.menu;

import com.villagershop.util.ItemStackHandler;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Emplacement de stock à index dynamique : position fixe à l'écran, mais l'index
 * de stock représenté dépend du défilement courant du menu hôte.
 */
public class DynamicStorageSlot extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final ScrollableStorageMenu host;
    private final ItemStackHandler handler;
    private final int gridIndex;
    private boolean tabVisible = true;

    public DynamicStorageSlot(ScrollableStorageMenu host, ItemStackHandler handler, int gridIndex, int x, int y) {
        super(EMPTY, gridIndex, x, y);
        this.host = host;
        this.handler = handler;
        this.gridIndex = gridIndex;
    }

    private int idx() {
        return host.getScrollOffset() * 9 + gridIndex;
    }

    private boolean valid() {
        int i = idx();
        return i >= 0 && i < host.getActiveCapacity() && i < handler.getSlots();
    }

    public void setTabVisible(boolean v) { this.tabVisible = v; }

    @Override
    public boolean isActive() {
        return tabVisible && valid();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return valid() && handler.isItemValid(idx(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return valid() && !handler.getStackInSlot(idx()).isEmpty();
    }

    @Override
    public ItemStack getItem() {
        return valid() ? handler.getStackInSlot(idx()) : ItemStack.EMPTY;
    }

    @Override
    public void set(ItemStack stack) {
        if (valid()) {
            handler.setStackInSlot(idx(), stack);
            setChanged();
        }
    }

    @Override
    public int getMaxStackSize() {
        // 1.21 : getSlotLimit() de NeoForge renvoie 99 (nouvelle limite vanilla).
        // On plafonne à 64 pour garder le comportement classique du stock.
        return valid() ? Math.min(64, handler.getSlotLimit(idx())) : 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        // Toujours re-plafonner au max de l'item (16 pour les perles, etc.) :
        // sans ce min, on pouvait empiler jusqu'à la limite du slot (99).
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public ItemStack remove(int amount) {
        if (!valid()) return ItemStack.EMPTY;
        ItemStack current = handler.getStackInSlot(idx());
        if (current.isEmpty()) return ItemStack.EMPTY;
        int taken = Math.min(amount, current.getCount());
        ItemStack out = current.copy();
        out.setCount(taken);
        ItemStack left = current.copy();
        left.shrink(taken);
        handler.setStackInSlot(idx(), left);
        setChanged();
        return out;
    }
}
