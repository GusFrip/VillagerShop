package com.villagershop.client;

import com.villagershop.ShopMod;
import com.villagershop.data.ShopOffer;
import com.villagershop.menu.ShopTradeMenu;
import com.villagershop.network.BuyOfferPacket;
import com.villagershop.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Écran de vente vu par le client (ouvert via le villageois-vitrine). */
public class ShopTradeScreen extends AbstractContainerScreen<ShopTradeMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "textures/gui/shop_trade.png");

    private static final int ROW_Y0 = 20;
    private static final int ROW_DY = 20;
    private static final int PRICE_A_X = 10;
    private static final int RESULT_X = 46;

    public ShopTradeScreen(ShopTradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = ShopTradeMenu.PLAYER_INV_Y - 12;

        for (int i = 0; i < ShopTradeMenu.OFFERS; i++) {
            ShopOffer offer = menu.getOffer(i);
            if (!offer.isValid()) continue;
            final int index = i;
            int y = ROW_Y0 + i * ROW_DY;
            addRenderableWidget(Button.builder(Component.translatable("gui.villagershop.buy"),
                            b -> ModNetwork.sendToServer(new BuyOfferPacket(index)))
                    .bounds(leftPos + 78, topPos + y - 1, 88, 18).build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        gg.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        gg.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        for (int i = 0; i < ShopTradeMenu.OFFERS; i++) {
            if (!menu.getOffer(i).isValid()) continue;
            int y = ROW_Y0 + i * ROW_DY + 4;
            gg.drawString(this.font, "→", 32, y, 0x555555, false);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // 1.21 : renderBackground est appele par super.render
        super.render(gg, mouseX, mouseY, partialTick);
        // Icônes des offres (prix + marchandise)
        for (int i = 0; i < ShopTradeMenu.OFFERS; i++) {
            ShopOffer offer = menu.getOffer(i);
            if (!offer.isValid()) continue;
            int y = ROW_Y0 + i * ROW_DY;
            drawItem(gg, offer.getPriceA(), leftPos + PRICE_A_X, topPos + y);
            drawItem(gg, offer.getResult(), leftPos + RESULT_X, topPos + y);
        }
        // Tooltips au survol
        for (int i = 0; i < ShopTradeMenu.OFFERS; i++) {
            ShopOffer offer = menu.getOffer(i);
            if (!offer.isValid()) continue;
            int y = ROW_Y0 + i * ROW_DY;
            if (hovering(mouseX, mouseY, leftPos + PRICE_A_X, topPos + y)) {
                gg.renderTooltip(font, offer.getPriceA(), mouseX, mouseY);
            } else if (hovering(mouseX, mouseY, leftPos + RESULT_X, topPos + y)) {
                gg.renderTooltip(font, offer.getResult(), mouseX, mouseY);
            }
        }
        renderTooltip(gg, mouseX, mouseY);
    }

    private void drawItem(GuiGraphics gg, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        gg.renderItem(stack, x, y);
        gg.renderItemDecorations(this.font, stack, x, y);
    }

    private boolean hovering(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
