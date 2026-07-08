package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S : le client demande la liste à jour des joueurs autorisés (à l'ouverture du GUI). */
public record RequestAllowedPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RequestAllowedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "request_allowed"));

    public static final StreamCodec<FriendlyByteBuf, RequestAllowedPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public RequestAllowedPacket decode(FriendlyByteBuf buf) { return new RequestAllowedPacket(buf.readBlockPos()); }
        @Override public void encode(FriendlyByteBuf buf, RequestAllowedPacket msg) { buf.writeBlockPos(msg.pos); }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestAllowedPacket msg, ServerPlayer player) {
        if (player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be && be.canAccess(player)) {
            ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
        }
    }
}
