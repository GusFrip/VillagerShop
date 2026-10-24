package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.config.VSConfig;
import com.villagershop.data.ShopOffer;
import com.villagershop.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu unique à onglets (Shop / Trade / Stock).
 * - 8 offres éditables en place (3 slots fantômes chacune : prixA, prixB, résultat) -> indices 0..23
 * - 1 slot coffre -> 24
 * - 18 slots de stock défilant -> 25..42
 * - inventaire joueur -> 43..78
 */
public class ShopConfigMenu extends AbstractContainerMenu implements ScrollableStorageMenu {
    public static final int OFFERS = ShopBlockEntity.MAX_OFFERS; // 8
    public static final int GHOST_PER_OFFER = 3;
    public static final int GHOST_SLOTS = OFFERS * GHOST_PER_OFFER; // 24
    public static final int CHEST_SLOT_INDEX = GHOST_SLOTS;         // 24
    public static final int STORAGE_START = GHOST_SLOTS + 1;        // 25
    public static final int VISIBLE_ROWS = 3;
    public static final int STORAGE_VISIBLE = VISIBLE_ROWS * 9;     // 27
    public static final int STORAGE_END = STORAGE_START + STORAGE_VISIBLE; // 52
    public static final int UPGRADE_START = STORAGE_END;            // 43 (3 slots)
    public static final int UPGRADE_COUNT = ShopBlockEntity.UPGRADE_SLOTS; // 7 (dont le slot admin caché)
    public static final int ADMIN_SLOT_INDEX = UPGRADE_START + ShopBlockEntity.ADMIN_SLOT;
    public static final int SAVE_QUILL_INDEX = UPGRADE_START + UPGRADE_COUNT;
    public static final int SAVE_SIGNED_INDEX = SAVE_QUILL_INDEX + 1;
    public static final int TRADE_SLOT_INDEX = SAVE_SIGNED_INDEX + 1;
    public static final int INV_START = TRADE_SLOT_INDEX + 1;
    public static final int INV_END = INV_START + 36;               // 82

    // Grille d'offres : 2 colonnes de 4
    public static final int OFFER_ROWS = 4;
    public static final int OFFER_Y0 = 34, OFFER_DY = 20;
    public static final int COL0_X = 0, COL1_X = 90;
    // Layout compact : bouton ∞ (mode admin) à gauche, puis prixA, prixB, flèche, résultat.
    public static final int OFF_TOGGLE_DX = 4, OFF_PRICE_A_DX = 20, OFF_PRICE_B_DX = 40, OFF_RESULT_DX = 68;
    public static final int OFF_ARROW_DX = 58;

    public static final int CHEST_SLOT_X = 134, CHEST_SLOT_Y = 40;
    public static final int TRADE_SLOT_X = 8, TRADE_SLOT_Y = 40;
    // rangée de 8 slots (pas de 18) : émeraude(8), 6 upgrades, coffre(134)
    public static final int[] UPGRADE_XS = {26, 44, 62, 80, 98, 116, 152}; // immortalité, déplacement, communication, mémoire, accès, anti-explosion, admin (caché)
    public static final int[] UPGRADE_YS = {40, 40, 40, 40, 40, 40, 40};
    /** Rangée compactée : émeraude, 6 upgrades, coffre, admin ; une upgrade désactivée par la config ne laisse pas de trou. */
    public static final int ROW_X0 = 8, ROW_DX = 18;
    public static final int ROW_TRADE = 0, ROW_STORAGE = 7, ROW_ADMIN = 8, ROW_SIZE = 9;

    /** Abscisses de la rangée selon la config (les slots désactivés, masqués, prennent la colonne suivante). */
    public static int[] rowXs() {
        boolean[] on = new boolean[ROW_SIZE];
        on[ROW_TRADE] = VSConfig.maxTradeBlocks() > 0;
        for (int i = 0; i < ShopBlockEntity.ADMIN_SLOT; i++) on[1 + i] = VSConfig.isSlotEnabled(i);
        on[ROW_STORAGE] = VSConfig.isEnabled(VSConfig.Upgrade.STORAGE);
        on[ROW_ADMIN] = VSConfig.isEnabled(VSConfig.Upgrade.ADMIN);
        int[] xs = new int[ROW_SIZE];
        int col = 0;
        for (int k = 0; k < ROW_SIZE; k++) {
            xs[k] = ROW_X0 + Math.min(col, ROW_SIZE - 1) * ROW_DX;
            if (on[k]) col++;
        }
        return xs;
    }
    public static final int SAVE_QUILL_X = 8, SAVE_SIGNED_X = 30, SAVE_Y = 90;
    public static final int STORAGE_X = 8, STORAGE_Y = 62;
    public static final int PLAYER_INV_X = 8, PLAYER_INV_Y = 138;

    @Nullable
    private final ShopBlockEntity be;
    private final Container templates;
    private final IItemHandler chestHandler;
    private final IItemHandler storageHandler;
    private final IItemHandler upgradeHandler;
    private final IItemHandler tradeHandler;
    private final BlockPos pos;
    private final String shopName;
    private int scrollOffset = 0;
    /** Drapeaux "illimité" par offre, synchronisés serveur -> client via DataSlot. */
    private final int[] infiniteFlags = new int[OFFERS];
    /** Id de bouton (clickMenuButton) : bascule le mode illimité de l'offre (id - TOGGLE_BUTTON_BASE). */
    public static final int TOGGLE_BUTTON_BASE = 1000;

    /** Constructeur CLIENT. */
    public ShopConfigMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), buf.readUtf(), new SimpleContainer(GHOST_SLOTS),
                new ItemStackHandler(ShopBlockEntity.MAX_STORAGE), new ItemStackHandler(1), new ItemStackHandler(ShopBlockEntity.UPGRADE_SLOTS), new ItemStackHandler(2), new ItemStackHandler(1));
    }

    /** Constructeur SERVEUR. */
    public ShopConfigMenu(int id, Inventory inv, ShopBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.getShopName(), buildTemplatesFrom(be), be.getStorage(), be.getChestUpgrade(), be.getUpgrades(), be.getSaveSlot(), be.getTradeUpgrade());
    }

    private ShopConfigMenu(int id, Inventory inv, @Nullable ShopBlockEntity be, BlockPos pos, String shopName,
                           Container templates, IItemHandlerModifiable storage, IItemHandler chestHandler, IItemHandler upgradeHandler, IItemHandler saveHandler, IItemHandler tradeHandler) {
        super(ModMenus.SHOP_CONFIG.get(), id);
        this.be = be;
        this.pos = pos;
        this.shopName = shopName == null ? "" : shopName;
        this.templates = templates;
        this.chestHandler = chestHandler;
        this.storageHandler = storage;
        this.upgradeHandler = upgradeHandler;
        this.tradeHandler = tradeHandler;

        // 8 offres : prixA, prixB, résultat
        for (int i = 0; i < OFFERS; i++) {
            int base = (i / OFFER_ROWS == 0) ? COL0_X : COL1_X;
            int y = OFFER_Y0 + (i % OFFER_ROWS) * OFFER_DY;
            addSlot(new ToggleGhostSlot(templates, i * 3, base + OFF_PRICE_A_DX, y));
            addSlot(new ToggleGhostSlot(templates, i * 3 + 1, base + OFF_PRICE_B_DX, y));
            addSlot(new ToggleGhostSlot(templates, i * 3 + 2, base + OFF_RESULT_DX, y));
        }

        final int[] rowX = rowXs();

        // coffre
        addSlot(new ChestUpgradeSlot(chestHandler, 0, rowX[ROW_STORAGE], CHEST_SLOT_Y));

        // stock visible
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new DynamicStorageSlot(this, storage, row * 9 + col,
                        STORAGE_X + col * 18, STORAGE_Y + row * 18));
            }
        }

        // améliorations (3 slots)
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            int x = (i == ShopBlockEntity.ADMIN_SLOT) ? rowX[ROW_ADMIN] : rowX[1 + i];
            addSlot(new UpgradeSlot(upgradeHandler, i, x, UPGRADE_YS[i]));
        }
        // slot de sauvegarde (livre & plume), onglet Upgrade
        addSlot(new UpgradeSlot(saveHandler, 0, SAVE_QUILL_X, SAVE_Y));
        addSlot(new UpgradeSlot(saveHandler, 1, SAVE_SIGNED_X, SAVE_Y));

        // blocs d'émeraude : débloquent les offres (1 de base + 1 par bloc)
        addSlot(new TradeUpgradeSlot(tradeHandler, 0, rowX[ROW_TRADE], TRADE_SLOT_Y));

        // Sync des drapeaux illimités (un DataSlot par offre)
        for (int i = 0; i < OFFERS; i++) {
            final int idx = i;
            addDataSlot(new net.minecraft.world.inventory.DataSlot() {
                @Override public int get() {
                    return ShopConfigMenu.this.be != null
                            ? (ShopConfigMenu.this.be.getOffer(idx).isInfinite() ? 1 : 0)
                            : infiniteFlags[idx];
                }
                @Override public void set(int v) { infiniteFlags[idx] = v; }
            });
        }

        // Capacité affichée synchronisée serveur -> client : le client ne connaît que le contenu des
        // lignes visibles, il ne peut donc pas voir seul les items conservés hors capacité.
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return getDisplayCapacity(); }
            @Override public void set(int v) { syncedDisplayCap = v; }
        });

        // inventaire
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    private static Container buildTemplatesFrom(ShopBlockEntity be) {
        SimpleContainer c = new SimpleContainer(GHOST_SLOTS);
        for (int i = 0; i < OFFERS; i++) {
            ShopOffer o = be.getOffer(i);
            c.setItem(i * 3, o.getPriceA().copy());
            c.setItem(i * 3 + 1, o.getPriceB().copy());
            c.setItem(i * 3 + 2, o.getResult().copy());
        }
        return c;
    }

    // ----- Stock / capacité -----------------------------------------------

    public int getChestCount() {
        return Math.min(VSConfig.maxChests(), chestHandler.getStackInSlot(0).getCount());
    }

    /** Nombre d'offres actives (1 + blocs d'émeraude), calculable côté client via le slot. */
    public int getTradeCount() {
        return Math.min(VSConfig.maxOffers(), 1 + tradeHandler.getStackInSlot(0).getCount());
    }

    @Override
    public int getActiveCapacity() {
        return VSConfig.capacityFor(getChestCount(), ShopBlockEntity.BASE_CAPACITY);
    }

    /** Côté client : capacité affichée reçue du serveur (voir le DataSlot du constructeur). */
    private int syncedDisplayCap = 0;

    @Override
    public int getDisplayCapacity() {
        if (be == null) return Math.max(getActiveCapacity(), syncedDisplayCap);
        return ScrollableStorageMenu.displayCapacity(getActiveCapacity(), getChestCount(), storageHandler);
    }

    public int getRows() { return (int) Math.ceil(getDisplayCapacity() / 9.0); }

    public int getMaxScroll() { return Math.max(0, getRows() - VISIBLE_ROWS); }

    @Override
    public int getScrollOffset() { return scrollOffset; }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScroll()));
    }

    /** Version du stock déjà envoyée au client. */
    private int sentStorageVersion = Integer.MIN_VALUE;

    @Override
    public void broadcastChanges() {
        // Le stock se tasse et défile : un slot dont l'item « ne change pas » (bûches -> bûches) peut
        // pourtant représenter un autre emplacement côté client. Dès que le stock bouge, on renvoie tout.
        if (be != null && be.getStorageVersion() != sentStorageVersion) {
            sentStorageVersion = be.getStorageVersion();
            sendAllDataToRemote();
            return;
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= TOGGLE_BUTTON_BASE) {
            // bascule du mode illimité (serveur : upgrade Admin requise, vérifiée par le BE)
            if (be != null) be.toggleInfinite(id - TOGGLE_BUTTON_BASE);
            return true;
        }
        setScrollOffset(id);
        // Renvoi complet : un slot dont l'item ne change pas (ex. bûches 64 -> bûches 64) n'est pas
        // resynchronisé, et le client n'aurait pas le contenu de la nouvelle ligne.
        if (be != null) sendAllDataToRemote(); // défilement du stock
        return true;
    }

    /** Côté client : l'offre est-elle en mode illimité ? */
    public boolean isInfinite(int offer) {
        return offer >= 0 && offer < OFFERS && infiniteFlags[offer] != 0;
    }

    /** L'upgrade Admin (bloc de commande) est-elle posée ? (lisible côté client via le slot) */
    public boolean hasAdmin() {
        return slots.get(ADMIN_SLOT_INDEX).hasItem();
    }

    // ----- Offres (édition en place) --------------------------------------

    private boolean isGhost(int slotId) { return slotId >= 0 && slotId < GHOST_SLOTS; }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (DynamicStorageSlot.handleLockedClick(this, slotId, clickType, player)) {
            if (be != null) be.compactStorage();
            return;
        }
        if (isGhost(slotId)) {
            Slot slot = slots.get(slotId);
            ItemStack carried = getCarried();
            if (clickType == ClickType.PICKUP || clickType == ClickType.PICKUP_ALL) {
                if (dragType == 1) slot.set(ItemStack.EMPTY);
                else if (carried.isEmpty()) slot.set(ItemStack.EMPTY);
                else slot.set(carried.copy());
                pushTemplatesToBE();
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
        if (be != null) be.compactStorage();
    }


    /** Recharge les slots d'offres depuis le BE (après un import de sauvegarde),
     *  sinon le menu ouvert garde ses modèles périmés et les écrase au prochain clic. */
    public void reloadTemplatesFromBE() {
        if (be == null) return;
        for (int i = 0; i < OFFERS; i++) {
            ShopOffer o = be.getOffer(i);
            templates.setItem(i * 3, o.getPriceA().copy());
            templates.setItem(i * 3 + 1, o.getPriceB().copy());
            templates.setItem(i * 3 + 2, o.getResult().copy());
        }
        broadcastChanges();
    }

    private void pushTemplatesToBE() {
        if (be == null) return;
        for (int i = 0; i < OFFERS; i++) {
            ShopOffer o = new ShopOffer(
                    templates.getItem(i * 3).copy(),
                    templates.getItem(i * 3 + 1).copy(),
                    templates.getItem(i * 3 + 2).copy());
            o.setInfinite(be.getOffer(i).isInfinite()); // le drapeau ∞ survit à l'édition des items
            be.setOffer(i, o);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem() || isGhost(index)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index == CHEST_SLOT_INDEX || index == SAVE_QUILL_INDEX || index == SAVE_SIGNED_INDEX || index == TRADE_SLOT_INDEX || (index >= STORAGE_START && index < STORAGE_END)
                || (index >= UPGRADE_START && index < UPGRADE_START + UPGRADE_COUNT)) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (stack.is(VSConfig.item(VSConfig.Upgrade.STORAGE))) moved = moveItemStackTo(stack, CHEST_SLOT_INDEX, CHEST_SLOT_INDEX + 1, false);
            if (!stack.isEmpty() && stack.is(Items.WRITABLE_BOOK)) moved = moveItemStackTo(stack, SAVE_QUILL_INDEX, SAVE_QUILL_INDEX + 1, false) || moved;
            if (!stack.isEmpty() && stack.is(Items.WRITTEN_BOOK)) moved = moveItemStackTo(stack, SAVE_SIGNED_INDEX, SAVE_SIGNED_INDEX + 1, false) || moved;
            if (!stack.isEmpty() && stack.is(VSConfig.item(VSConfig.Upgrade.TRADES))) moved = moveItemStackTo(stack, TRADE_SLOT_INDEX, TRADE_SLOT_INDEX + 1, false) || moved;
            if (!stack.isEmpty()) moved = moveItemStackTo(stack, UPGRADE_START, UPGRADE_START + UPGRADE_COUNT, false) || moved;
            if (!stack.isEmpty()) moved = moveItemStackTo(stack, STORAGE_START, STORAGE_END, false) || moved;
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null) return true;
        return be.canAccess(player) && player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    public BlockPos getPos() { return pos; }

    public String getShopName() { return shopName; }
}
