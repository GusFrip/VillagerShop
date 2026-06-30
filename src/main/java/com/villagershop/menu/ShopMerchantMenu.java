package com.villagershop.menu;

import com.villagershop.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Menu marchand du shop. Trois ajustements par rapport au marchand vanilla :
 * 1) quickMoveStack reecrit sans playTradeSound (privee) qui caste le marchand en Entity
 *    -> plantait avec notre ShopMerchant non-entite au shift-clic (dupe). Son rejoue dans notifyTrade.
 * 2) shift-clic sur le resultat plafonne a un stack max.
 * 3) offres GRATUITES (cout vide) gerees a la main : vanilla n'assemble pas le resultat
 *    quand les cases de paiement sont vides, donc on affiche/encaisse nous-memes (serveur
 *    autoritaire), sans mixin. getType() = SHOP_MERCHANT : le client utilise CE menu.
 */
public class ShopMerchantMenu extends MerchantMenu {

    private static final int RESULT_SLOT = 2;
    private static final int INV_FIRST = 3, INV_LAST = 39;

    private final Merchant shopMerchant; // non-null cote serveur
    private int hint = -1;

    public ShopMerchantMenu(int containerId, Inventory inventory) {
        super(containerId, inventory);
        this.shopMerchant = null;
    }

    public ShopMerchantMenu(int containerId, Inventory inventory, Merchant merchant) {
        super(containerId, inventory, merchant);
        this.shopMerchant = merchant;
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.SHOP_MERCHANT.get();
    }

    @Override
    public void setSelectionHint(int h) {
        super.setSelectionHint(h);
        this.hint = h;
        showFreeResult();
    }

    @Override
    public void tryMoveItems(int h) {
        super.tryMoveItems(h);
        this.hint = h;
        showFreeResult();
    }

    @Override
    public void setItem(int slotId, int stateId, ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        showFreeResult();
    }

    @Override
    public void initializeContents(int stateId, java.util.List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
        showFreeResult();
    }

    @Override
    public void broadcastChanges() {
        // Filet de securite : a chaque synchro, on re-pose le resultat d'une offre gratuite
        // selectionnee (vanilla l'efface des que le paiement est vide). Garantit l'affichage.
        showFreeResult();
        super.broadcastChanges();
    }

    /** Renvoie l'offre selectionnee si elle est gratuite (aucun cout), sinon null. */
    private MerchantOffer selectedFreeOffer() {
        MerchantOffers offers = this.getOffers();
        if (offers == null || hint < 0 || hint >= offers.size()) return null;
        MerchantOffer o = offers.get(hint);
        return (o != null && o.getCostA().isEmpty() && o.getCostB().isEmpty()) ? o : null;
    }

    private void showFreeResult() {
        MerchantOffer free = selectedFreeOffer();
        if (free != null && !free.isOutOfStock()) {
            // re-pose le resultat a chaque synchro (sans condition isEmpty) pour eviter
            // tout decalage d'affichage du a updateSellItem qui revide la case.
            setResultDirect(free.assemble());
        }
    }

    /** Ecrit dans la case resultat SANS passer par Slot.set (qui declenche
     *  MerchantContainer.setChanged -> updateSellItem, lequel reviderait la case). */
    private void setResultDirect(ItemStack stack) {
        Slot rs = this.slots.get(RESULT_SLOT);
        rs.container.setItem(rs.getContainerSlot(), stack);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == RESULT_SLOT) {
            MerchantOffer free = selectedFreeOffer();
            if (free != null) {
                if (!player.level().isClientSide && shopMerchant != null
                        && (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP)) {
                    handleFreeTake(clickType, player, free);
                }
                return; // resultat gratuit : on gere (ou on ignore) nous-memes, jamais vanilla
            }
            // offre payante : on plafonne le shift-clic a un stack
            if (clickType == ClickType.QUICK_MOVE) {
                Slot slot = this.slots.get(RESULT_SLOT);
                if (slot.hasItem()) {
                    int per = Math.max(1, slot.getItem().getCount());
                    int maxTrades = Math.max(1, slot.getItem().getMaxStackSize() / per);
                    for (int i = 0; i < maxTrades && slot.hasItem(); i++) {
                        if (this.quickMoveStack(player, RESULT_SLOT).isEmpty()) break;
                    }
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Don gratuit, cote serveur uniquement. */
    private void handleFreeTake(ClickType clickType, Player player, MerchantOffer offer) {
        ItemStack r = offer.getResult();
        if (clickType == ClickType.QUICK_MOVE) {
            int per = Math.max(1, r.getCount());
            int maxTrades = Math.max(1, r.getMaxStackSize() / per);
            for (int i = 0; i < maxTrades; i++) {
                if (offer.isOutOfStock() || !hasRoomFor(player, r)) break;
                ItemStack give = r.copy();
                player.getInventory().add(give);
                shopMerchant.notifyTrade(offer);
            }
        } else { // PICKUP -> sur le curseur
            if (!offer.isOutOfStock()) {
                ItemStack carried = this.getCarried();
                if (carried.isEmpty()) {
                    this.setCarried(r.copy());
                    shopMerchant.notifyTrade(offer);
                } else if (ItemStack.isSameItemSameTags(carried, r)
                        && carried.getCount() + r.getCount() <= carried.getMaxStackSize()) {
                    carried.grow(r.getCount());
                    shopMerchant.notifyTrade(offer);
                }
            }
        }
        // rafraichir l'affichage du resultat
        setResultDirect(offer.isOutOfStock() ? ItemStack.EMPTY : offer.assemble());
        // Re-synchronise les offres au client : sinon son etat (usages/rupture) reste fige
        // -> la fleche ne passe pas rouge et le resultat reste affiche a tort.
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendMerchantOffers(this.containerId, this.getOffers(), 0,
                    shopMerchant.getVillagerXp(), shopMerchant.showProgressBar(), shopMerchant.canRestock());
        }
        this.broadcastChanges();
    }

    /** Vrai si l'inventaire principal peut accueillir entierement 'stack'. */
    private boolean hasRoomFor(Player player, ItemStack stack) {
        int need = stack.getCount();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size() && need > 0; i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) need -= stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(s, stack)) need -= (s.getMaxStackSize() - s.getCount());
        }
        return need <= 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, INV_FIRST, INV_LAST, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (index != 0 && index != 1) {
                if (index >= INV_FIRST && index < 30) {
                    if (!this.moveItemStackTo(stack, 30, INV_LAST, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 30 && index < INV_LAST && !this.moveItemStackTo(stack, INV_FIRST, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_FIRST, INV_LAST, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            this.broadcastChanges();
        }
        return result;
    }
}
