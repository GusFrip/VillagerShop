package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.config.VSConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C : la config effective du serveur, envoyée à chaque joueur à sa connexion. */
public record SyncConfigPacket(byte[] data) implements CustomPacketPayload {
    public static final Type<SyncConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncConfigPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public SyncConfigPacket decode(FriendlyByteBuf buf) { return new SyncConfigPacket(buf.readByteArray()); }
        @Override public void encode(FriendlyByteBuf buf, SyncConfigPacket msg) { buf.writeByteArray(msg.data); }
    };

    /** Côté serveur : capture la config courante. */
    public static SyncConfigPacket capture() {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        VSConfig.encode(buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new SyncConfigPacket(bytes);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Receveur client (thread client). */
    public static void handleClient(SyncConfigPacket msg) {
        VSConfig.decodeAndApply(new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(msg.data)));
        com.villagershop.client.GuideSync.refresh(); // le guide suit la config du serveur
    }
}
