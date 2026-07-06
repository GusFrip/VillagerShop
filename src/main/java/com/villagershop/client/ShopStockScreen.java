package com.villagershop.client;

import com.villagershop.ShopMod;
import com.villagershop.menu.ShopStockMenu;
import com.villagershop.network.ModNetwork;
import com.villagershop.network.OpenSubScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Écran Stock : coffres d'amélioration + stock défilant + inventaire. */
public class ShopStockScreen extends AbstractContainerScreen<ShopStockMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "textures/gui/shop_stock.png");
    /** Sprites vanilla du curseur de défilement (système de GUI sprites, 1.20.2+). */
    private static final ResourceLocation SCROLLER_SPRITE =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED =
            ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    private static final int SCROLL_X = 174;
    private static final int SCROLL_Y = ShopStockMenu.STORAGE_Y;
    private static final int SCROLL_W = 12;
    private static final int TRACK_H = ShopStockMenu.VISIBLE_ROWS * 18;
    private static final int KNOB_H = 15;

    private boolean scrolling = false;

    public ShopStockScreen(ShopStockMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 190;
        this.imageHeight = 184;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = ShopStockMenu.PLAYER_INV_Y - 12;

        addRenderableWidget(Button.builder(Component.translatable("gui.villagershop.config_btn"),
                        b -> ModNetwork.sendToServer(new OpenSubScreenPacket(menu.getPos(), false)))
                .bounds(leftPos + 118, topPos + 14, 64, 16).build());
    }

    private void applyScroll(int offset) {
        menu.setScrollOffset(offset);
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, menu.getScrollOffset());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        menu.setScrollOffset(menu.getScrollOffset());
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        gg.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gg.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        gg.drawString(this.font, Component.translatable("gui.villagershop.capacity",
                menu.getChestCount(), menu.getActiveCapacity()), 30, 22, 0x404040, false);
        gg.drawString(this.font, Component.translatable("gui.villagershop.stock"),
                8, ShopStockMenu.STORAGE_Y - 11, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // 1.21 : renderBackground est appele par super.render
        super.render(gg, mouseX, mouseY, partialTick);
        renderScrollbar(gg);
        renderLockedStock(gg, mouseX, mouseY);
        renderTooltip(gg, mouseX, mouseY);
    }

    /** Voile sur les emplacements de stock verrouillés (coffres manquants). */
    private void renderLockedStock(GuiGraphics gg, int mouseX, int mouseY) {
        Component tip = null;
        int cap = menu.getActiveCapacity();
        for (int row = 0; row < ShopStockMenu.VISIBLE_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = (menu.getScrollOffset() + row) * 9 + col;
                if (idx < cap) continue;
                int x = leftPos + ShopStockMenu.STORAGE_X + col * 18;
                int y = topPos + ShopStockMenu.STORAGE_Y + row * 18;
                gg.fill(x - 1, y - 1, x + 17, y + 17, 0x55313131);
                if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
                    tip = Component.translatable("gui.villagershop.locked_stock");
            }
        }
        if (tip != null) gg.renderTooltip(font, tip, mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphics gg) {
        int x = leftPos + SCROLL_X, y = topPos + SCROLL_Y;
        gg.fill(x, y, x + SCROLL_W, y + TRACK_H, 0xFF373737);
        int max = menu.getMaxScroll();
        int knobY = (max == 0) ? y : y + (int) ((TRACK_H - KNOB_H) * (menu.getScrollOffset() / (double) max));
        gg.blitSprite(max == 0 ? SCROLLER_DISABLED : SCROLLER_SPRITE, x, knobY, SCROLL_W, KNOB_H);
    }

    private boolean overScrollbar(double mx, double my) {
        int x = leftPos + SCROLL_X, y = topPos + SCROLL_Y;
        return mx >= x && mx < x + SCROLL_W && my >= y && my < y + TRACK_H;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double deltaX, double delta) {
        if (menu.getMaxScroll() > 0) {
            applyScroll(menu.getScrollOffset() - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mx, my, deltaX, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (menu.getMaxScroll() > 0 && overScrollbar(mx, my)) {
            scrolling = true;
            dragScrollTo(my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (scrolling) {
            dragScrollTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        scrolling = false;
        return super.mouseReleased(mx, my, button);
    }

    private void dragScrollTo(double my) {
        int max = menu.getMaxScroll();
        if (max <= 0) return;
        double rel = (my - (topPos + SCROLL_Y + KNOB_H / 2.0)) / (TRACK_H - KNOB_H);
        rel = Math.max(0.0, Math.min(1.0, rel));
        applyScroll((int) Math.round(rel * max));
    }
}
