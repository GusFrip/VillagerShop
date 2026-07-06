package com.villagershop.client;

import com.villagershop.ShopMod;
import com.villagershop.data.ShopOffer;
import com.villagershop.menu.ChestUpgradeSlot;
import com.villagershop.menu.DynamicStorageSlot;
import com.villagershop.menu.ShopConfigMenu;
import com.villagershop.menu.ToggleGhostSlot;
import com.villagershop.menu.TradeUpgradeSlot;
import com.villagershop.menu.UpgradeSlot;
import com.villagershop.network.LocateVillagerPacket;
import com.villagershop.network.SaveActionPacket;
import com.villagershop.network.ManageAllowedPacket;
import com.villagershop.network.ModNetwork;
import com.villagershop.network.RequestAllowedPacket;
import com.villagershop.network.SetShopNamePacket;
import com.villagershop.network.TransferOwnerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Écran à onglets Shop (📖) / Trade (💚) / Stock (🧰). */
public class ShopConfigScreen extends AbstractContainerScreen<ShopConfigMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShopMod.MOD_ID, "textures/gui/shop_config.png");
    /** Texture vanilla : on y prend le curseur de défilement (u=232, 12x15). */
    private static final ResourceLocation VANILLA_TABS =
            new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final ResourceLocation TAB_TEX =
            new ResourceLocation(ShopMod.MOD_ID, "textures/gui/tab.png");

    private static final int TAB_SHOP = 0, TAB_TRADE = 1, TAB_STOCK = 2, TAB_UPGRADE = 3;
    private static final int PANEL_TOP = 20;
    private static final int TAB_X = 0, TAB_Y = 0, TAB_W = 28, TAB_H = 20, TAB_GAP = 29;
    private static final ItemStack[] TAB_ICONS = {
            new ItemStack(Items.BOOK), new ItemStack(Items.EMERALD), new ItemStack(Items.CHEST), new ItemStack(Items.SPECTRAL_ARROW)};
    private static final String[] TAB_KEYS = {"gui.villagershop.tab_shop", "gui.villagershop.tab_trade", "gui.villagershop.tab_stock", "gui.villagershop.tab_upgrade"};

    // Copropriétaires (onglet Shop)
    private static final int CO_X = 8, CO_Y = 92, CO_ROW_H = 11, CO_VISIBLE = 3, CO_W = 158, CO_SB_X = 168;
    private static final int CO_REMOVE_W = 12; // largeur de la zone cliquable du ✕

    private int currentTab = TAB_SHOP;

    private EditBox shopNameField, coownerField;
    private Button addCoownerBtn, locateBtn, saveBtn;
    private String savedShopName = null, savedCoowner = "";

    private List<String> coownerNames = new ArrayList<>();
    private int coownerScroll = 0;
    private boolean storageScrolling = false, coownerScrolling = false;

    public ShopConfigScreen(ShopConfigMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 190;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 8;
        this.inventoryLabelY = ShopConfigMenu.PLAYER_INV_Y - 12;

        shopNameField = new EditBox(font, leftPos + 8, topPos + 46, 168, 14,
                Component.translatable("gui.villagershop.shop_name"));
        shopNameField.setMaxLength(32);
        shopNameField.setHint(Component.translatable("gui.villagershop.shop_name"));
        shopNameField.setValue(savedShopName != null ? savedShopName : menu.getShopName());
        shopNameField.setResponder(t -> ModNetwork.sendToServer(new SetShopNamePacket(menu.getPos(), t)));
        addRenderableWidget(shopNameField);

        coownerField = new EditBox(font, leftPos + 8, topPos + 76, 118, 14,
                Component.translatable("gui.villagershop.player_name"));
        coownerField.setHint(Component.translatable("gui.villagershop.player_name"));
        coownerField.setValue(savedCoowner);
        addRenderableWidget(coownerField);

        addCoownerBtn = addRenderableWidget(Button.builder(Component.translatable("gui.villagershop.add"), b -> addCoowner())
                .bounds(leftPos + 130, topPos + 75, 46, 16).build());

        locateBtn = addRenderableWidget(Button.builder(Component.translatable("gui.villagershop.locate"),
                        b -> ModNetwork.sendToServer(new LocateVillagerPacket(menu.getPos())))
                .bounds(leftPos + 8, topPos + 60, 160, 16).build());

        saveBtn = addRenderableWidget(Button.builder(Component.translatable("gui.villagershop.save_action"),
                        b -> ModNetwork.sendToServer(new SaveActionPacket(menu.getPos())))
                .bounds(leftPos + 54, topPos + 90, 98, 16).build());

        setActiveTab(currentTab);
        ModNetwork.sendToServer(new RequestAllowedPacket(menu.getPos()));
    }

    private void setActiveTab(int tab) {
        this.currentTab = tab;
        updateSlotVisibility();
    }

    private boolean upgradePresent(int i) {
        int idx = ShopConfigMenu.UPGRADE_START + i;
        return idx < menu.slots.size() && menu.slots.get(idx).hasItem();
    }
    private boolean hasCommClient()   { return upgradePresent(2); }
    private boolean hasMemoryClient() { return upgradePresent(3); }
    private boolean hasAccessClient() { return upgradePresent(4); }

    private void addCoowner() {
        String name = coownerField.getValue().trim();
        if (!name.isEmpty()) ModNetwork.sendToServer(new ManageAllowedPacket(menu.getPos(), name, true));
    }

    public void setAllowedNames(List<String> names) {
        this.coownerNames = names;
        int max = Math.max(0, names.size() - CO_VISIBLE);
        if (coownerScroll > max) coownerScroll = max;
    }

    /** Conservé pour compat réseau (non utilisé : les offres se synchronisent via les slots). */
    public void setOffers(List<ShopOffer> offers, int selected) {}

    @Override
    public void resize(Minecraft mc, int w, int h) {
        if (shopNameField != null) savedShopName = shopNameField.getValue();
        if (coownerField != null) savedCoowner = coownerField.getValue();
        super.resize(mc, w, h);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        menu.setScrollOffset(menu.getScrollOffset());
    }

    private void updateSlotVisibility() {
        boolean trade = currentTab == TAB_TRADE, stock = currentTab == TAB_STOCK;
        for (int i = 0; i < ShopConfigMenu.GHOST_SLOTS; i++)
            ((ToggleGhostSlot) menu.slots.get(i)).setVisible(trade && (i / ShopConfigMenu.GHOST_PER_OFFER) < menu.getTradeCount());
        ((ChestUpgradeSlot) menu.slots.get(ShopConfigMenu.CHEST_SLOT_INDEX)).setVisible(currentTab == TAB_UPGRADE);
        ((TradeUpgradeSlot) menu.slots.get(ShopConfigMenu.TRADE_SLOT_INDEX)).setVisible(currentTab == TAB_UPGRADE);
        for (int i = ShopConfigMenu.STORAGE_START; i < ShopConfigMenu.STORAGE_END; i++)
            ((DynamicStorageSlot) menu.slots.get(i)).setTabVisible(stock);
        boolean up = currentTab == TAB_UPGRADE;
        for (int i = ShopConfigMenu.UPGRADE_START; i < ShopConfigMenu.UPGRADE_START + ShopConfigMenu.UPGRADE_COUNT; i++)
            ((UpgradeSlot) menu.slots.get(i)).setVisible(up);

        // Modules conditionnels (visibles seulement si l'upgrade correspondant est posé)
        boolean shop = currentTab == TAB_SHOP;
        if (shopNameField != null) shopNameField.visible = shop;
        if (locateBtn != null)     locateBtn.visible = (currentTab == TAB_UPGRADE) && hasCommClient();
        boolean access = shop && hasAccessClient();
        if (coownerField != null)  coownerField.visible = access;
        if (addCoownerBtn != null) addCoownerBtn.visible = access;
        boolean save = (currentTab == TAB_UPGRADE) && hasMemoryClient();
        if (saveBtn != null) saveBtn.visible = save;
        ((UpgradeSlot) menu.slots.get(ShopConfigMenu.SAVE_QUILL_INDEX)).setVisible(save);
        ((UpgradeSlot) menu.slots.get(ShopConfigMenu.SAVE_SIGNED_INDEX)).setVisible(save);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        gg.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight);
        for (int i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            if (slot.isActive()) slotBg(gg, leftPos + slot.x, topPos + slot.y);
        }
    }

    private void slotBg(GuiGraphics gg, int x, int y) {
        // slot encastré façon inventaire vanilla : intérieur gris, ombre haut/gauche, lumière bas/droite
        gg.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        gg.fill(x - 1, y - 1, x + 17, y, 0xFF373737);       // haut (ombre)
        gg.fill(x - 1, y - 1, x, y + 17, 0xFF373737);       // gauche (ombre)
        gg.fill(x, y + 16, x + 17, y + 17, 0xFFFFFFFF);     // bas (lumière)
        gg.fill(x + 16, y - 1, x + 17, y + 16, 0xFFFFFFFF); // droite (lumière)
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        Component tabTitle = Component.translatable(TAB_KEYS[currentTab]);
        gg.drawString(font, tabTitle, (imageWidth - font.width(tabTitle)) / 2, 24, 0x404040, false);

        if (currentTab == TAB_SHOP) {
            gg.drawString(font, Component.translatable("gui.villagershop.shop_name"), 8, 36, 0x404040, false);
            if (hasAccessClient())
                gg.drawString(font, Component.translatable("gui.villagershop.coowners"), 8, 66, 0x404040, false);
        } else if (currentTab == TAB_TRADE) {
            for (int i = 0; i < menu.getTradeCount(); i++) {
                int base = (i / ShopConfigMenu.OFFER_ROWS == 0) ? ShopConfigMenu.COL0_X : ShopConfigMenu.COL1_X;
                int y = ShopConfigMenu.OFFER_Y0 + (i % ShopConfigMenu.OFFER_ROWS) * ShopConfigMenu.OFFER_DY + 4;
                gg.drawString(font, "→", base + 50, y, 0x555555, false);
            }
            Component cnt = Component.translatable("gui.villagershop.offers_count",
                    menu.getTradeCount(), ShopConfigMenu.OFFERS);
            gg.drawString(font, cnt, imageWidth - font.width(cnt) - 8, 24, 0x404040, false);
            // séparateur vertical central
            gg.fill(87, 32, 88, 112, 0xFF555555);
            gg.fill(88, 32, 89, 112, 0xFFFFFFFF);
        } else if (currentTab == TAB_STOCK) {
            gg.drawString(font, Component.translatable("gui.villagershop.capacity",
                    menu.getChestCount(), menu.getActiveCapacity()), 8, 40, 0x404040, false);
            gg.drawString(font, Component.translatable("gui.villagershop.stock"), 8, ShopConfigMenu.STORAGE_Y - 11, 0x404040, false);
        } else if (currentTab == TAB_UPGRADE) {
            if (hasMemoryClient())
                gg.drawString(font, Component.translatable("gui.villagershop.save_module"), 8, 80, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        updateSlotVisibility();
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partialTick);
        renderTabs(gg);
        if (currentTab == TAB_SHOP && hasAccessClient()) renderCoownerList(gg, mouseX, mouseY);
        else if (currentTab == TAB_STOCK) { renderStorageScrollbar(gg); renderLockedStock(gg, mouseX, mouseY); }
        else if (currentTab == TAB_UPGRADE) { renderGhostUpgrades(gg); renderGhostChest(gg); renderGhostTrade(gg); renderGhostSave(gg); }
        else if (currentTab == TAB_TRADE) renderLockedOffers(gg, mouseX, mouseY);
        renderTabTooltips(gg, mouseX, mouseY);
        renderTooltip(gg, mouseX, mouseY);
    }

    private void renderTabs(GuiGraphics gg) {
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int x = leftPos + TAB_X + i * TAB_GAP;
            gg.blit(TAB_TEX, x, topPos, 0f, 0f, TAB_W, TAB_H, TAB_W, TAB_H);
            if (i == currentTab) {
                // fusionne l'onglet actif au panneau en effaçant le bord supérieur dessous
                gg.fill(x + 1, topPos + PANEL_TOP, x + TAB_W - 1, topPos + PANEL_TOP + 1, 0xFFC6C6C6);
            }
            gg.renderItem(TAB_ICONS[i], x + 6, topPos + 2);
        }
    }

    private void renderTabTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int x = leftPos + TAB_X + i * TAB_GAP, y = topPos + TAB_Y;
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + TAB_H)
                gg.renderTooltip(font, Component.translatable(TAB_KEYS[i]), mouseX, mouseY);
        }
    }

    /** Bloc d'émeraude grisé dans le slot de déblocage des offres. */
    private void renderGhostTrade(GuiGraphics gg) {
        if (menu.slots.get(ShopConfigMenu.TRADE_SLOT_INDEX).hasItem()) return;
        ghostIcon(gg, new ItemStack(Items.EMERALD_BLOCK),
                leftPos + ShopConfigMenu.TRADE_SLOT_X, topPos + ShopConfigMenu.TRADE_SLOT_Y);
    }

    /** Voile sur les offres verrouillées + tooltip explicatif. */
    private void renderLockedOffers(GuiGraphics gg, int mouseX, int mouseY) {
        Component tip = null;
        for (int i = menu.getTradeCount(); i < ShopConfigMenu.OFFERS; i++) {
            int base = leftPos + ((i / ShopConfigMenu.OFFER_ROWS == 0) ? ShopConfigMenu.COL0_X : ShopConfigMenu.COL1_X);
            int y = topPos + ShopConfigMenu.OFFER_Y0 + (i % ShopConfigMenu.OFFER_ROWS) * ShopConfigMenu.OFFER_DY;
            int x0 = base + ShopConfigMenu.OFF_PRICE_A_DX - 1, x1 = base + ShopConfigMenu.OFF_RESULT_DX + 17;
            gg.fill(x0, y - 1, x1, y + 17, 0x55313131);
            if (mouseX >= x0 && mouseX < x1 && mouseY >= y - 1 && mouseY < y + 17)
                tip = Component.translatable("gui.villagershop.locked_offer");
        }
        if (tip != null) gg.renderTooltip(font, tip, mouseX, mouseY);
    }

    /** Voile sur les emplacements de stock verrouillés (coffres manquants). */
    private void renderLockedStock(GuiGraphics gg, int mouseX, int mouseY) {
        Component tip = null;
        int cap = menu.getActiveCapacity();
        for (int row = 0; row < ShopConfigMenu.VISIBLE_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = (menu.getScrollOffset() + row) * 9 + col;
                if (idx < cap) continue;
                int x = leftPos + ShopConfigMenu.STORAGE_X + col * 18;
                int y = topPos + ShopConfigMenu.STORAGE_Y + row * 18;
                slotBg(gg, x, y);
                gg.fill(x - 1, y - 1, x + 17, y + 17, 0x55313131);
                if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
                    tip = Component.translatable("gui.villagershop.locked_stock");
            }
        }
        if (tip != null) gg.renderTooltip(font, tip, mouseX, mouseY);
    }

    private void renderCoownerList(GuiGraphics gg, int mouseX, int mouseY) {
        for (int r = 0; r < CO_VISIBLE; r++) {
            int idx = coownerScroll + r;
            if (idx >= coownerNames.size()) break;
            int rx = leftPos + CO_X, ry = topPos + CO_Y + r * CO_ROW_H;
            boolean removable = idx > 0; // index 0 = proprio, jamais supprimable
            // suppression uniquement via le ✕ (zone de 12 px à gauche de la ligne)
            boolean hoverX = removable && mouseX >= rx && mouseX < rx + CO_REMOVE_W
                    && mouseY >= ry && mouseY < ry + CO_ROW_H;
            if (hoverX) gg.fill(rx, ry, rx + CO_W, ry + CO_ROW_H, 0x66FF5555);
            gg.drawString(font, (removable ? "✕ " : "• ") + coownerNames.get(idx),
                    rx + 2, ry + 1, hoverX ? 0xFFFFFF : 0x404040, false);
            if (removable) {
                // ★ à droite : Maj+clic = transfert de la boutique à ce co-proprio
                boolean hoverStar = mouseX >= rx + CO_W - CO_REMOVE_W && mouseX < rx + CO_W
                        && mouseY >= ry && mouseY < ry + CO_ROW_H;
                gg.drawString(font, "★", rx + CO_W - 10, ry + 1, hoverStar ? 0xFFAA00 : 0x9A9A9A, false);
            }
        }
        int max = Math.max(0, coownerNames.size() - CO_VISIBLE);
        drawScroller(gg, leftPos + CO_SB_X, topPos + CO_Y, CO_VISIBLE * CO_ROW_H, coownerScroll, max);
    }

    /** Coffre grisé indiquant ce qu'on peut déposer (façon lapis de la table d'enchantement). */
    private static final ItemStack[] UPGRADE_HINTS = {
            new ItemStack(Items.NETHER_STAR), new ItemStack(Items.PRISMARINE_SHARD), new ItemStack(Items.LIGHTNING_ROD),
            new ItemStack(Items.BOOKSHELF), new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.OBSIDIAN)};

    private void renderGhostUpgrades(GuiGraphics gg) {
        for (int i = 0; i < ShopConfigMenu.UPGRADE_COUNT; i++) {
            if (menu.slots.get(ShopConfigMenu.UPGRADE_START + i).hasItem()) continue;
            int x = leftPos + ShopConfigMenu.UPGRADE_XS[i];
            int y = topPos + ShopConfigMenu.UPGRADE_YS[i];
            gg.renderItem(UPGRADE_HINTS[i], x, y);
            gg.pose().pushPose();
            gg.pose().translate(0, 0, 200);
            gg.fill(x, y, x + 16, y + 16, 0xAAC6C6C6);
            gg.pose().popPose();
        }
    }

    private void renderGhostChest(GuiGraphics gg) {
        if (menu.slots.get(ShopConfigMenu.CHEST_SLOT_INDEX).hasItem()) return;
        int x = leftPos + ShopConfigMenu.CHEST_SLOT_X, y = topPos + ShopConfigMenu.CHEST_SLOT_Y;
        gg.renderItem(new ItemStack(Items.CHEST), x, y);
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 200);
        gg.fill(x, y, x + 16, y + 16, 0xAAC6C6C6); // voile pour griser
        gg.pose().popPose();
    }

    private void renderGhostSave(GuiGraphics gg) {
        if (!hasMemoryClient()) return;
        if (!menu.slots.get(ShopConfigMenu.SAVE_QUILL_INDEX).hasItem())
            ghostIcon(gg, new ItemStack(Items.WRITABLE_BOOK), leftPos + ShopConfigMenu.SAVE_QUILL_X, topPos + ShopConfigMenu.SAVE_Y);
        if (!menu.slots.get(ShopConfigMenu.SAVE_SIGNED_INDEX).hasItem())
            ghostIcon(gg, new ItemStack(Items.WRITTEN_BOOK), leftPos + ShopConfigMenu.SAVE_SIGNED_X, topPos + ShopConfigMenu.SAVE_Y);
    }

    private void ghostIcon(GuiGraphics gg, ItemStack icon, int x, int y) {
        gg.renderItem(icon, x, y);
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 200);
        gg.fill(x, y, x + 16, y + 16, 0xAAC6C6C6);
        gg.pose().popPose();
    }

    private void renderStorageScrollbar(GuiGraphics gg) {
        int track = ShopConfigMenu.VISIBLE_ROWS * 18;
        drawScroller(gg, leftPos + 172, topPos + ShopConfigMenu.STORAGE_Y, track, menu.getScrollOffset(), menu.getMaxScroll());
    }

    /** Curseur de défilement façon vanilla (groove sombre + curseur de la texture créative). */
    private void drawScroller(GuiGraphics gg, int x, int y, int trackH, int scroll, int max) {
        if (max <= 0) return; // pas de scroller si rien à faire défiler
        gg.fill(x, y, x + 14, y + trackH, 0xFF373737);
        int knobY = (max == 0) ? y : y + (int) ((trackH - 15) * (scroll / (double) max));
        gg.blit(VANILLA_TABS, x + 1, knobY, 232, 0, 12, 15);
    }

    private void drawItem(GuiGraphics gg, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        gg.renderItem(stack, x, y);
        gg.renderItemDecorations(font, stack, x, y);
    }

    // ----- Souris ----------------------------------------------------------

    private int tabAt(double mx, double my) {
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int x = leftPos + TAB_X + i * TAB_GAP, y = topPos + TAB_Y;
            if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) return i;
        }
        return -1;
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentTab == TAB_SHOP && hasAccessClient() && over(mx, my, leftPos + CO_X, topPos + CO_Y, CO_W, CO_VISIBLE * CO_ROW_H)) {
            int max = Math.max(0, coownerNames.size() - CO_VISIBLE);
            if (max > 0) { coownerScroll = Math.max(0, Math.min(coownerScroll - (int) Math.signum(delta), max)); return true; }
        }
        if (currentTab == TAB_STOCK && menu.getMaxScroll() > 0) {
            applyStorageScroll(menu.getScrollOffset() - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void applyStorageScroll(int offset) {
        menu.setScrollOffset(offset);
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, menu.getScrollOffset());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int t = tabAt(mx, my);
        if (t >= 0) { setActiveTab(t); return true; }

        if (currentTab == TAB_SHOP && hasAccessClient() && over(mx, my, leftPos + CO_X, topPos + CO_Y, CO_W, CO_VISIBLE * CO_ROW_H)) {
            int r = (int) ((my - (topPos + CO_Y)) / CO_ROW_H);
            int idx = coownerScroll + r;
            if (r >= 0 && r < CO_VISIBLE && idx < coownerNames.size()) {
                // suppression uniquement au clic sur le ✕ (12 px à gauche), jamais le proprio
                if (idx > 0 && mx < leftPos + CO_X + CO_REMOVE_W) {
                    ModNetwork.sendToServer(new ManageAllowedPacket(menu.getPos(), coownerNames.get(idx), false));
                } else if (idx > 0 && mx >= leftPos + CO_X + CO_W - CO_REMOVE_W) {
                    // transfert de propriété : Maj+clic sur ★ (le serveur vérifie que l'émetteur est proprio)
                    if (hasShiftDown()) {
                        ModNetwork.sendToServer(new TransferOwnerPacket(menu.getPos(), coownerNames.get(idx)));
                    } else {
                        Minecraft mcl = Minecraft.getInstance();
                        if (mcl.player != null) mcl.player.displayClientMessage(
                                Component.translatable("gui.villagershop.transfer_hint"), true);
                    }
                }
                return true;
            }
        }
        if (currentTab == TAB_SHOP && hasAccessClient()) {
            int track = CO_VISIBLE * CO_ROW_H, max = Math.max(0, coownerNames.size() - CO_VISIBLE);
            if (max > 0 && over(mx, my, leftPos + CO_SB_X, topPos + CO_Y, 14, track)) {
                coownerScrolling = true; dragCoowner(my); return true;
            }
        }
        if (currentTab == TAB_STOCK) {
            int track = ShopConfigMenu.VISIBLE_ROWS * 18;
            if (menu.getMaxScroll() > 0 && over(mx, my, leftPos + 172, topPos + ShopConfigMenu.STORAGE_Y, 14, track)) {
                storageScrolling = true; dragStorage(my); return true;
            }
        }
        // Slots fantômes : clic traité dès l'APPUI (un micro-drag devenait un
        // QUICK_CRAFT avalé par le menu -> il fallait cliquer plusieurs fois).
        if (currentTab == TAB_TRADE) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.player != null) {
                for (int i = 0; i < ShopConfigMenu.GHOST_SLOTS; i++) {
                    Slot slot = menu.slots.get(i);
                    if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mx, my)) {
                        mc.gameMode.handleInventoryMouseClick(menu.containerId, i, button, ClickType.PICKUP, mc.player);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (storageScrolling) { dragStorage(my); return true; }
        if (coownerScrolling) { dragCoowner(my); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        storageScrolling = false;
        coownerScrolling = false;
        return super.mouseReleased(mx, my, button);
    }

    private void dragStorage(double my) {
        int max = menu.getMaxScroll();
        if (max <= 0) return;
        int y = topPos + ShopConfigMenu.STORAGE_Y, track = ShopConfigMenu.VISIBLE_ROWS * 18;
        double rel = (my - (y + 7.5)) / (track - 15);
        rel = Math.max(0.0, Math.min(1.0, rel));
        applyStorageScroll((int) Math.round(rel * max));
    }

    private void dragCoowner(double my) {
        int max = Math.max(0, coownerNames.size() - CO_VISIBLE);
        if (max <= 0) return;
        int y = topPos + CO_Y, track = CO_VISIBLE * CO_ROW_H;
        double rel = (my - (y + 7.5)) / (track - 15);
        rel = Math.max(0.0, Math.min(1.0, rel));
        coownerScroll = (int) Math.round(rel * max);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != 256 && currentTab == TAB_SHOP) {
            if (shopNameField.isFocused())
                return shopNameField.keyPressed(key, scan, mods) || shopNameField.canConsumeInput() || super.keyPressed(key, scan, mods);
            if (coownerField.isFocused())
                return coownerField.keyPressed(key, scan, mods) || coownerField.canConsumeInput() || super.keyPressed(key, scan, mods);
        }
        return super.keyPressed(key, scan, mods);
    }
}
