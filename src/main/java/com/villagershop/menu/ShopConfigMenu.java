package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
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
    public static final int UPGRADE_COUNT = com.villagershop.block.ShopBlockEntity.UPGRADE_SLOTS; // 7 (le 7e = Kit de garde)
    public static final int SAVE_QUILL_INDEX = UPGRADE_START + UPGRADE_COUNT;
    public static final int SAVE_SIGNED_INDEX = SAVE_QUILL_INDEX + 1;
    public static final int TRADE_SLOT_INDEX = SAVE_SIGNED_INDEX + 1;
    public static final int INV_START = TRADE_SLOT_INDEX + 1;
    public static final int INV_END = INV_START + 36;               // 82

    // Grille d'offres : 2 colonnes de 4
    public static final int OFFER_ROWS = 4;
    public static final int OFFER_Y0 = 34, OFFER_DY = 20;
    public static final int COL0_X = 0, COL1_X = 90;
    public static final int OFF_PRICE_A_DX = 8, OFF_PRICE_B_DX = 30, OFF_RESULT_DX = 62;

    public static final int CHEST_SLOT_X = 134, CHEST_SLOT_Y = 40;
    public static final int TRADE_SLOT_X = 8, TRADE_SLOT_Y = 40;
    // rangée de 8 slots (pas de 18) : émeraude(8), 6 upgrades, coffre(134)
    public static final int[] UPGRADE_XS = {26, 44, 62, 80, 98, 116, 26}; // immortalité, déplacement, communication, mémoire, accès, anti-explosion, kit de garde
    public static final int[] UPGRADE_YS = {40, 40, 40, 40, 40, 40, 62};   // le kit de garde est sur une 2e rangée
    public static final int SAVE_QUILL_X = 8, SAVE_SIGNED_X = 30, SAVE_Y = 90;
    public static final int STORAGE_X = 8, STORAGE_Y = 62;
    public static final int PLAYER_INV_X = 8, PLAYER_INV_Y = 138;

    @Nullable
    private final ShopBlockEntity be;
    /**
     * Vrai si le vendeur de ce comptoir est une entité tierce (pillager de
     * PillagerControl...) : conditionne l'affichage du slot Kit de garde.
     */
    private boolean moddedVendor;
    private final Container templates;
    private final IItemHandler chestHandler;
    private final IItemHandler upgradeHandler;
    private final IItemHandler tradeHandler;
    private final BlockPos pos;
    private final String shopName;
    private int scrollOffset = 0;

    /** Constructeur CLIENT. */
    public ShopConfigMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(), buf.readUtf(), new SimpleContainer(GHOST_SLOTS),
                new ItemStackHandler(ShopBlockEntity.MAX_STORAGE), new ItemStackHandler(1),
                new ItemStackHandler(ShopBlockEntity.UPGRADE_SLOTS), new ItemStackHandler(2), new ItemStackHandler(1));
        this.moddedVendor = buf.readBoolean();
    }

    /** Constructeur SERVEUR. */
    public ShopConfigMenu(int id, Inventory inv, ShopBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.getShopName(), buildTemplatesFrom(be), be.getStorage(), be.getChestUpgrade(), be.getUpgrades(), be.getSaveSlot(), be.getTradeUpgrade());
    }

    private ShopConfigMenu(int id, Inventory inv, @Nullable ShopBlockEntity be, BlockPos pos, String shopName,
                           Container templates, IItemHandlerModifiable storage, IItemHandler chestHandler, IItemHandler upgradeHandler, IItemHandler saveHandler, IItemHandler tradeHandler) {
        super(ModMenus.SHOP_CONFIG.get(), id);
        this.be = be;
        if (be != null && be.getLevel() != null) {
            var vendor = be.findShopkeeper();
            this.moddedVendor = vendor != null && !(vendor instanceof net.minecraft.world.entity.npc.Villager);
        }
        this.pos = pos;
        this.shopName = shopName == null ? "" : shopName;
        this.templates = templates;
        this.chestHandler = chestHandler;
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

        // coffre
        addSlot(new ChestUpgradeSlot(chestHandler, 0, CHEST_SLOT_X, CHEST_SLOT_Y));

        // stock visible
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new DynamicStorageSlot(this, storage, row * 9 + col,
                        STORAGE_X + col * 18, STORAGE_Y + row * 18));
            }
        }

        // améliorations (3 slots)
        for (int i = 0; i < UPGRADE_COUNT; i++) {
            addSlot(new UpgradeSlot(upgradeHandler, i, UPGRADE_XS[i], UPGRADE_YS[i]));
        }
        // slot de sauvegarde (livre & plume), onglet Upgrade
        addSlot(new UpgradeSlot(saveHandler, 0, SAVE_QUILL_X, SAVE_Y));
        addSlot(new UpgradeSlot(saveHandler, 1, SAVE_SIGNED_X, SAVE_Y));

        // blocs d'émeraude : débloquent les offres (1 de base + 1 par bloc)
        addSlot(new TradeUpgradeSlot(tradeHandler, 0, TRADE_SLOT_X, TRADE_SLOT_Y));

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
        return Math.min(ShopBlockEntity.MAX_CHESTS, chestHandler.getStackInSlot(0).getCount());
    }

    /** Nombre d'offres actives (1 + blocs d'émeraude), calculable côté client via le slot. */
    public int getTradeCount() {
        return Math.min(ShopBlockEntity.MAX_OFFERS,
                1 + Math.min(ShopBlockEntity.MAX_OFFERS - 1, tradeHandler.getStackInSlot(0).getCount()));
    }

    @Override
    public int getActiveCapacity() {
        int chests = getChestCount();
        return chests == 0 ? ShopBlockEntity.BASE_CAPACITY : ShopBlockEntity.SLOTS_PER_CHEST * chests;
    }

    public int getRows() { return (int) Math.ceil(getActiveCapacity() / 9.0); }

    public int getMaxScroll() { return Math.max(0, getRows() - VISIBLE_ROWS); }

    @Override
    public int getScrollOffset() { return scrollOffset; }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScroll()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        setScrollOffset(id); // défilement du stock
        return true;
    }

    // ----- Offres (édition en place) --------------------------------------

    private boolean isGhost(int slotId) { return slotId >= 0 && slotId < GHOST_SLOTS; }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
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
            be.setOffer(i, new ShopOffer(
                    templates.getItem(i * 3).copy(),
                    templates.getItem(i * 3 + 1).copy(),
                    templates.getItem(i * 3 + 2).copy()));
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
            if (stack.is(Items.CHEST)) moved = moveItemStackTo(stack, CHEST_SLOT_INDEX, CHEST_SLOT_INDEX + 1, false);
            if (!stack.isEmpty() && stack.is(Items.WRITABLE_BOOK)) moved = moveItemStackTo(stack, SAVE_QUILL_INDEX, SAVE_QUILL_INDEX + 1, false) || moved;
            if (!stack.isEmpty() && stack.is(Items.WRITTEN_BOOK)) moved = moveItemStackTo(stack, SAVE_SIGNED_INDEX, SAVE_SIGNED_INDEX + 1, false) || moved;
            if (!stack.isEmpty() && stack.is(Items.EMERALD_BLOCK)) moved = moveItemStackTo(stack, TRADE_SLOT_INDEX, TRADE_SLOT_INDEX + 1, false) || moved;
            // Le slot Kit de garde (dernier) est exclu du shift-clic quand il est
            // masqué (vendeur villageois) : sinon l'item disparaîtrait dans un
            // slot invisible.
            int upgradeEnd = UPGRADE_START + (moddedVendor ? UPGRADE_COUNT : UPGRADE_COUNT - 1);
            if (!stack.isEmpty()) moved = moveItemStackTo(stack, UPGRADE_START, upgradeEnd, false) || moved;
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

    /** Le vendeur est une entité tierce (pillager…) : slot Kit de garde utile. */
    public boolean isModdedVendor() { return moddedVendor; }

    public String getShopName() { return shopName; }
}
