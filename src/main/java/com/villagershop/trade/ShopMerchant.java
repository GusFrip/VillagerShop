package com.villagershop.trade;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.data.ShopOffer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

/**
 * Marchand "boutique" branché sur l'interface de troc native de Minecraft.
 * Les offres du comptoir deviennent des MerchantOffer ; le nombre d'achats possibles
 * (maxUses) reflète le stock disponible à l'ouverture. À chaque transaction, le
 * paiement est déposé dans le stock et la marchandise en est retirée.
 * Pas de niveau / d'XP (showProgressBar = false), titre = nom du shop.
 */
public class ShopMerchant implements Merchant {
    private final ShopBlockEntity be;
    @Nullable
    private Player tradingPlayer;
    private MerchantOffers offers;

    public ShopMerchant(ShopBlockEntity be) {
        this.be = be;
        this.offers = build();
    }

    private MerchantOffers build() {
        MerchantOffers list = new MerchantOffers();
        for (int i = 0; i < ShopBlockEntity.MAX_OFFERS; i++) {
            ShopOffer o = be.getOffer(i);
            if (!o.isValid()) continue;
            int per = o.getResult().getCount();
            int avail = per > 0 ? be.countStock(o.getResult()) / per : 0;
            list.add(new MerchantOffer(
                    o.getPriceA().copy(),
                    o.getPriceB().copy(),
                    o.getResult().copy(),
                    0,                       // uses
                    Math.max(0, avail),      // maxUses = nb d'achats possibles selon le stock
                    0,                       // xp
                    0.0F));                  // priceMultiplier
        }
        return list;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers newOffers) {
        this.offers = newOffers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        // Encaisser le paiement dans le stock
        be.depositToStock(offer.getCostA().copy());
        if (!offer.getCostB().isEmpty()) {
            be.depositToStock(offer.getCostB().copy());
        }
        // Retirer la marchandise du stock
        be.removeFromStock(offer.getResult(), offer.getResult().getCount());
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        // rien
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
        // pas d'XP
    }

    @Override
    public boolean showProgressBar() {
        return false; // pas de barre de niveau (Novice, etc.)
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return be.getLevel() != null && be.getLevel().isClientSide();
    }

    @Override
    public boolean canRestock() {
        return true;
    }
}
