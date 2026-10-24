package com.villagershop.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Emplacement de stock à index dynamique : position fixe à l'écran, mais l'index
 * de stock représenté dépend du défilement courant du menu hôte.
 */
public class DynamicStorageSlot extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final ScrollableStorageMenu host;
    private final IItemHandlerModifiable handler;
    private final int gridIndex;
    private boolean tabVisible = true;

    public DynamicStorageSlot(ScrollableStorageMenu host, IItemHandlerModifiable handler, int gridIndex, int x, int y) {
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
        return i >= 0 && i < host.getDisplayCapacity() && i < handler.getSlots();
    }

    /** Dépôt possible : uniquement dans la capacité active (au-delà = retrait seulement). */
    private boolean placeable() {
        int i = idx();
        return i >= 0 && i < host.getActiveCapacity() && i < handler.getSlots();
    }

    public void setTabVisible(boolean v) { this.tabVisible = v; }

    /** Slot hors capacité (items conservés après une baisse de config) : verrouillé, clic = récupération. */
    public boolean isLocked() {
        int i = idx();
        return valid() && i >= host.getActiveCapacity();
    }

    /** Envoie l'item du slot verrouillé dans l'inventaire du joueur, ou à ses pieds s'il n'a pas de place. */
    public void ejectTo(Player player) {
        ItemStack s = getItem();
        if (s.isEmpty()) return;
        handler.setStackInSlot(idx(), ItemStack.EMPTY);
        setChanged();
        player.getInventory().placeItemBackInInventory(s.copy());
    }

    /**
     * Intercepte un clic sur un slot de stock verrouillé (à appeler en tête de {@code clicked}).
     * Renvoie true si le clic a été consommé.
     */
    public static boolean handleLockedClick(AbstractContainerMenu menu, int slotId, ClickType type, Player player) {
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        if (!(menu.slots.get(slotId) instanceof DynamicStorageSlot ds) || !ds.isLocked()) return false;
        if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE && type != ClickType.SWAP && type != ClickType.THROW)
            return false;
        if (!player.level().isClientSide) ds.ejectTo(player);
        return true;
    }

    @Override
    public boolean isActive() {
        // Un slot hors capacité vide n'a rien à récupérer : on ne l'affiche pas.
        return tabVisible && valid() && !(isLocked() && handler.getStackInSlot(idx()).isEmpty());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return placeable() && handler.isItemValid(idx(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return valid() && !isLocked() && !handler.getStackInSlot(idx()).isEmpty();
    }

    @Override
    public ItemStack getItem() {
        return valid() ? handler.getStackInSlot(idx()) : ItemStack.EMPTY;
    }

    @Override
    public void set(ItemStack stack) {
        // Pas de test valid() : à l'ouverture, le contenu arrive avant la capacité affichée
        // (DataSlot) ; le client jetterait sinon les items hors capacité.
        int i = idx();
        if (i >= 0 && i < handler.getSlots()) {
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
