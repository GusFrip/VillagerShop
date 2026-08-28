package com.villagershop.client;

import com.villagershop.stats.ShopStatSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Écran du Registre des ventes, calqué sur l'écran Statistiques vanilla :
 * fond dirt, liste scrollable native (ObjectSelectionList), lignes alternées
 * blanc/gris, icônes de colonnes dans des cadres de slot (stats_icons.png),
 * onglets = boutons vanilla en bas (l'onglet courant est grisé) + bouton OK.
 *
 * Onglets : Résumé (style "General"), Par objet (style "Items", agrégé),
 * Par boutique (liste cliquable → détail par objet de la boutique).
 */
public class LedgerScreen extends Screen {

    /** Feuille d'icônes du screen Statistics vanilla (cadre de slot 18x18 en 0,0). */
    private static final ResourceLocation STATS_ICONS = new ResourceLocation("textures/gui/container/stats_icons.png");

    private enum Mode { SUMMARY, BY_ITEM, BY_SHOP, SHOP_DETAIL }

    private final List<ShopStatSnapshot> shops;
    private Mode mode = Mode.SUMMARY;
    private ShopStatSnapshot detailShop = null;

    private LedgerList list;

    public LedgerScreen(List<ShopStatSnapshot> shops) {
        super(Component.translatable("gui.villagershop.ledger.title"));
        // Boutiques actives d'abord, retirées à la fin.
        this.shops = new ArrayList<>(shops);
        this.shops.sort(Comparator.comparing(s -> s.removed));
    }

    @Override
    protected void init() {
        list = new LedgerList(minecraft);
        buildEntries();
        addRenderableWidget(list);

        // Rangée d'onglets, façon General / Items / Mobs.
        int bw = 92;
        int rowX = width / 2 - (bw * 3 + 8) / 2;
        int rowY = height - 52;
        Button summary = Button.builder(Component.translatable("gui.villagershop.ledger.tab_summary"),
                b -> switchMode(Mode.SUMMARY)).bounds(rowX, rowY, bw, 20).build();
        Button byItem = Button.builder(Component.translatable("gui.villagershop.ledger.tab_items"),
                b -> switchMode(Mode.BY_ITEM)).bounds(rowX + bw + 4, rowY, bw, 20).build();
        Button byShop = Button.builder(Component.translatable("gui.villagershop.ledger.tab_shops"),
                b -> switchMode(Mode.BY_SHOP)).bounds(rowX + (bw + 4) * 2, rowY, bw, 20).build();
        summary.active = mode != Mode.SUMMARY;
        byItem.active = mode != Mode.BY_ITEM;
        byShop.active = mode != Mode.BY_SHOP && mode != Mode.SHOP_DETAIL;
        addRenderableWidget(summary);
        addRenderableWidget(byItem);
        addRenderableWidget(byShop);

        // Bouton OK (+ Retour en mode détail), façon "Done".
        if (mode == Mode.SHOP_DETAIL) {
            addRenderableWidget(Button.builder(Component.translatable("gui.villagershop.ledger.back"),
                            b -> switchMode(Mode.BY_SHOP))
                    .bounds(width / 2 - 102, height - 28, 100, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 + 2, height - 28, 100, 20).build());
        } else {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 - 100, height - 28, 200, 20).build());
        }
    }

    private void switchMode(Mode m) {
        mode = m;
        if (m != Mode.SHOP_DETAIL) detailShop = null;
        rebuildWidgets();
    }

    private void openDetail(ShopStatSnapshot shop) {
        detailShop = shop;
        mode = Mode.SHOP_DETAIL;
        rebuildWidgets();
    }

    // ----- Construction des lignes ------------------------------------------

    private void buildEntries() {
        switch (mode) {
            case SUMMARY -> {
                long sold = 0, received = 0;
                int active = 0;
                for (ShopStatSnapshot s : shops) {
                    if (!s.removed) active++;
                    for (long[] c : s.lifetime.values()) { sold += c[0]; received += c[1]; }
                }
                list.addGeneral(Component.translatable("gui.villagershop.ledger.sum_shops"), String.valueOf(shops.size()));
                list.addGeneral(Component.translatable("gui.villagershop.ledger.sum_active"), String.valueOf(active));
                list.addGeneral(Component.translatable("gui.villagershop.ledger.sum_sold"), String.valueOf(sold));
                list.addGeneral(Component.translatable("gui.villagershop.ledger.sum_received"), String.valueOf(received));
                if (shops.isEmpty()) {
                    list.addGeneral(Component.translatable("gui.villagershop.ledger.empty_hint"), "");
                }
            }
            case BY_ITEM -> fillItemRows(aggregated());
            case BY_SHOP -> {
                for (ShopStatSnapshot s : shops) list.addShop(s);
                if (shops.isEmpty()) {
                    list.addGeneral(Component.translatable("gui.villagershop.ledger.empty"), "");
                }
            }
            case SHOP_DETAIL -> {
                if (detailShop != null) fillItemRows(detailShop.lifetime);
            }
        }
    }

    private void fillItemRows(Map<String, long[]> counts) {
        List<Map.Entry<String, long[]>> rows = new ArrayList<>(counts.entrySet());
        rows.sort(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[0]).reversed());
        for (Map.Entry<String, long[]> e : rows) {
            list.addItem(stackOf(e.getKey()), e.getKey(), e.getValue()[0], e.getValue()[1]);
        }
        if (rows.isEmpty()) {
            list.addGeneral(Component.translatable("gui.villagershop.ledger.no_sales"), "");
        }
    }

    private Map<String, long[]> aggregated() {
        Map<String, long[]> map = new LinkedHashMap<>();
        for (ShopStatSnapshot s : shops) {
            for (Map.Entry<String, long[]> e : s.lifetime.entrySet()) {
                long[] c = map.computeIfAbsent(e.getKey(), k -> new long[2]);
                c[0] += e.getValue()[0];
                c[1] += e.getValue()[1];
            }
        }
        return map;
    }

    private static ItemStack stackOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ----- Rendu --------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Component heading = (mode == Mode.SHOP_DETAIL && detailShop != null)
                ? Component.literal(detailShop.name)
                : title;
        g.drawCenteredString(font, heading, width / 2, 15, 0xFFFFFF);

        if (list != null) {
            List<Component> tooltip = list.headerTooltipAt(mouseX, mouseY);
            if (tooltip != null) {
                g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ----- Liste façon StatsScreen ---------------------------------------------

    private class LedgerList extends ObjectSelectionList<LedgerList.Row> {

        LedgerList(Minecraft mc) {
            super(mc, LedgerScreen.this.width, LedgerScreen.this.height, 32,
                    LedgerScreen.this.height - 60,
                    mode == Mode.SUMMARY ? 12 : 18);
            if (mode == Mode.BY_ITEM || mode == Mode.SHOP_DETAIL || mode == Mode.BY_SHOP) {
                setRenderHeader(true, 20);
            }
        }

        void addGeneral(Component label, String value) {
            addEntry(new GeneralRow(label, value));
        }

        void addItem(ItemStack stack, String id, long sold, long received) {
            addEntry(new ItemRow(stack, id, sold, received));
        }

        void addShop(ShopStatSnapshot shop) {
            addEntry(new ShopRow(shop));
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + 158;
        }

        int headerSoldX = Integer.MIN_VALUE;
        int headerRecvX = Integer.MIN_VALUE;
        int headerY = Integer.MIN_VALUE;

        @Override
        protected void renderHeader(GuiGraphics g, int x, int y) {
            if (mode == Mode.SUMMARY) return;
            // Colonnes : émeraude = vendus, coffre = encaissés (cadres de slot vanilla).
            headerSoldX = x + getRowWidth() - 78;
            headerRecvX = x + getRowWidth() - 38;
            headerY = y;
            blitSlot(g, headerSoldX, y, new ItemStack(Items.EMERALD));
            blitSlot(g, headerRecvX, y, new ItemStack(Items.CHEST));
        }

        /** Tooltip d'en-tête si la souris survole une des deux icônes de colonnes. */
        List<Component> headerTooltipAt(int mouseX, int mouseY) {
            if (mode == Mode.SUMMARY || headerY == Integer.MIN_VALUE) return null;
            // Le header scrolle avec le contenu : ignorer s'il est sorti de la zone visible.
            if (mouseY < getTop() || mouseY > getBottom()) return null;
            if (mouseY < headerY || mouseY > headerY + 18) return null;
            if (mouseX >= headerSoldX && mouseX <= headerSoldX + 18) {
                return List.of(
                        Component.translatable("gui.villagershop.ledger.col_sold"),
                        Component.translatable("gui.villagershop.ledger.col_sold_desc"));
            }
            if (mouseX >= headerRecvX && mouseX <= headerRecvX + 18) {
                return List.of(
                        Component.translatable("gui.villagershop.ledger.col_received"),
                        Component.translatable("gui.villagershop.ledger.col_received_desc"));
            }
            return null;
        }

        private void blitSlot(GuiGraphics g, int x, int y, ItemStack icon) {
            g.blit(STATS_ICONS, x, y, 0, 0.0F, 0.0F, 18, 18, 128, 128);
            g.renderFakeItem(icon, x + 1, y + 1);
        }

        // ----- Lignes -----------------------------------------------------------

        abstract class Row extends ObjectSelectionList.Entry<Row> {
        }

        /** Ligne "label ..... valeur", façon onglet General. */
        class GeneralRow extends Row {
            private final Component label;
            private final String value;

            GeneralRow(Component label, String value) {
                this.label = label;
                this.value = value;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                int color = index % 2 == 0 ? 0xFFFFFF : 0x909090;
                g.drawString(font, label, left + 2, top + 1, color);
                if (!value.isEmpty()) {
                    g.drawString(font, value, left + rowWidth - 8 - font.width(value), top + 1, color);
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(label.getString() + " " + value);
            }
        }

        /** Ligne item : icône + nom + colonnes vendus / encaissés. */
        class ItemRow extends Row {
            private final ItemStack stack;
            private final String id;
            private final long sold;
            private final long received;

            ItemRow(ItemStack stack, String id, long sold, long received) {
                this.stack = stack;
                this.id = id;
                this.sold = sold;
                this.received = received;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                int color = index % 2 == 0 ? 0xFFFFFF : 0x909090;
                g.blit(STATS_ICONS, left, top, 0, 0.0F, 0.0F, 18, 18, 128, 128);
                if (!stack.isEmpty()) g.renderFakeItem(stack, left + 1, top + 1);
                String label = stack.isEmpty() ? id : stack.getHoverName().getString();
                g.drawString(font, font.plainSubstrByWidth(label, rowWidth - 110), left + 24, top + 5, color);
                drawCol(g, String.valueOf(sold), left + rowWidth - 78, top, color);
                drawCol(g, String.valueOf(received), left + rowWidth - 38, top, color);
            }

            private void drawCol(GuiGraphics g, String s, int colX, int top, int color) {
                // Centré sous l'icône de colonne (cadre de 18px).
                g.drawString(font, s, colX + 9 - font.width(s) / 2, top + 5, color);
            }

            @Override
            public Component getNarration() {
                return Component.literal((stack.isEmpty() ? id : stack.getHoverName().getString())
                        + " " + sold + " / " + received);
            }
        }

        /** Ligne boutique : nom + coordonnées + totaux ; clic → détail. */
        class ShopRow extends Row {
            private final ShopStatSnapshot shop;
            private final long sold;
            private final long received;

            ShopRow(ShopStatSnapshot shop) {
                this.shop = shop;
                long s = 0, r = 0;
                for (long[] c : shop.lifetime.values()) { s += c[0]; r += c[1]; }
                this.sold = s;
                this.received = r;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered) g.fill(left - 2, top - 2, left + rowWidth + 2, top + rowHeight, 0x30FFFFFF);
                int color = shop.removed ? 0x606060 : (index % 2 == 0 ? 0xFFFFFF : 0x909090);
                String name = shop.name + (shop.removed
                        ? " (" + Component.translatable("gui.villagershop.ledger.removed").getString() + ")" : "");
                g.drawString(font, font.plainSubstrByWidth(name, rowWidth - 160), left + 2, top + 1, color);
                String coords = shop.pos.getX() + " " + shop.pos.getY() + " " + shop.pos.getZ();
                g.drawString(font, coords, left + 2, top + 10, 0x606060);
                g.drawString(font, String.valueOf(sold),
                        left + rowWidth - 78 + 9 - font.width(String.valueOf(sold)) / 2, top + 5, color);
                g.drawString(font, String.valueOf(received),
                        left + rowWidth - 38 + 9 - font.width(String.valueOf(received)) / 2, top + 5, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    openDetail(shop);
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal(shop.name);
            }
        }
    }

}
