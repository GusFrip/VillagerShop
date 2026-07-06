package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.data.ShopOffer;
import com.villagershop.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Interface client de la boutique (ouverte en cliquant sur le villageois-vitrine).
 * Affiche les offres et l'inventaire du joueur. L'achat se fait via un paquet
 * réseau (BuyOfferPacket) validé côté serveur par le ShopBlockEntity.
 */
public class ShopTradeMenu extends AbstractContainerMenu {
    public static final int OFFERS = ShopBlockEntity.MAX_OFFERS;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 118;

    private final BlockPos shopPos;
    private final ShopOffer[] offers = new ShopOffer[OFFERS];

    /** Constructeur CLIENT. */
    public ShopTradeMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.SHOP_TRADE.get(), id);
        this.shopPos = buf.readBlockPos();
        for (int i = 0; i < OFFERS; i++) {
            ItemStack a = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ItemStack b = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ItemStack r = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            offers[i] = new ShopOffer(a, b, r);
        }
        addPlayerInventory(inv);
    }

    /** Constructeur SERVEUR. */
    public ShopTradeMenu(int id, Inventory inv, ShopBlockEntity be) {
        super(ModMenus.SHOP_TRADE.get(), id);
        this.shopPos = be.getBlockPos();
        for (int i = 0; i < OFFERS; i++) offers[i] = be.getOffer(i).copy();
        addPlayerInventory(inv);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public BlockPos getShopPos() { return shopPos; }

    public ShopOffer getOffer(int i) {
        return (i >= 0 && i < OFFERS) ? offers[i] : new ShopOffer();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // pas de transfert rapide dans l'écran de vente
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
