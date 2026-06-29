package com.villagershop.network;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.menu.ShopTradeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S : le client demande à acheter l'offre #index de la boutique ouverte. */
public class BuyOfferPacket {
    private final int index;

    public BuyOfferPacket(int index) { this.index = index; }

    public static void encode(BuyOfferPacket msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.index); }

    public static BuyOfferPacket decode(FriendlyByteBuf buf) { return new BuyOfferPacket(buf.readVarInt()); }

    public static void handle(BuyOfferPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof ShopTradeMenu menu) {
                BlockPos pos = menu.getShopPos();
                if (player.level().getBlockEntity(pos) instanceof ShopBlockEntity be) {
                    boolean ok = be.tryTrade(msg.index, player);
                    player.displayClientMessage(Component.translatable(
                            ok ? "message.villagershop.trade_ok" : "message.villagershop.trade_fail"), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
