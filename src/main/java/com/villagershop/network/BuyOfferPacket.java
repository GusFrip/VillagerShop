package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import com.villagershop.menu.ShopTradeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S : le client demande à acheter l'offre #index de la boutique ouverte. */
public record BuyOfferPacket(int index) implements CustomPacketPayload {
    public static final Type<BuyOfferPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "buy_offer"));

    public static final StreamCodec<FriendlyByteBuf, BuyOfferPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public BuyOfferPacket decode(FriendlyByteBuf buf) { return new BuyOfferPacket(buf.readVarInt()); }
        @Override public void encode(FriendlyByteBuf buf, BuyOfferPacket msg) { buf.writeVarInt(msg.index); }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BuyOfferPacket msg, ServerPlayer player) {
        if (player.containerMenu instanceof ShopTradeMenu menu) {
            BlockPos pos = menu.getShopPos();
            if (player.level().getBlockEntity(pos) instanceof ShopBlockEntity be) {
                boolean ok = be.tryTrade(msg.index, player);
                player.displayClientMessage(Component.translatable(
                        ok ? "message.villagershop.trade_ok" : "message.villagershop.trade_fail"), true);
            }
        }
    }
}
