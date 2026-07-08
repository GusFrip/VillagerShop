package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S : le propriétaire (ou un joueur autorisé) définit le nom du shop. */
public record SetShopNamePacket(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<SetShopNamePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "set_shop_name"));

    public static final StreamCodec<FriendlyByteBuf, SetShopNamePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public SetShopNamePacket decode(FriendlyByteBuf buf) {
            return new SetShopNamePacket(buf.readBlockPos(), buf.readUtf(256));
        }
        @Override public void encode(FriendlyByteBuf buf, SetShopNamePacket msg) {
            buf.writeBlockPos(msg.pos);
            buf.writeUtf(msg.name);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetShopNamePacket msg, ServerPlayer player) {
        if (player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be && be.canAccess(player)) {
            String clean = msg.name.length() > 32 ? msg.name.substring(0, 32) : msg.name;
            be.setShopName(clean.trim());
        }
    }
}
