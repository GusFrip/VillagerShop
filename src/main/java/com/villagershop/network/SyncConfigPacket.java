package com.villagershop.network;

import com.villagershop.config.VSConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C : la config effective du serveur, envoyée à chaque joueur à sa connexion. */
public class SyncConfigPacket {
    private final FriendlyByteBuf payload;

    /** Côté serveur : capture la config courante. */
    public SyncConfigPacket() {
        this.payload = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        VSConfig.encode(this.payload);
    }

    private SyncConfigPacket(FriendlyByteBuf payload) {
        this.payload = payload;
    }

    public static void encode(SyncConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBytes(msg.payload.copy());
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        FriendlyByteBuf copy = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        copy.writeBytes(buf.readBytes(buf.readableBytes()));
        return new SyncConfigPacket(copy);
    }

    public static void handle(SyncConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> VSConfig.decodeAndApply(msg.payload));
        ctx.get().setPacketHandled(true);
    }
}
