package com.villagershop.network;

import com.villagershop.ShopMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** S2C : pousse la liste des noms autorisés vers le client pour affichage. */
public record SyncAllowedPacket(List<String> names) implements CustomPacketPayload {
    public static final Type<SyncAllowedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "sync_allowed"));

    public static final StreamCodec<FriendlyByteBuf, SyncAllowedPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public SyncAllowedPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<String> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(buf.readUtf());
            return new SyncAllowedPacket(list);
        }
        @Override public void encode(FriendlyByteBuf buf, SyncAllowedPacket msg) {
            buf.writeVarInt(msg.names.size());
            for (String n : msg.names) buf.writeUtf(n);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncAllowedPacket msg, IPayloadContext ctx) {
        // Enregistré en playToClient : ce handler ne tourne que côté client.
        com.villagershop.client.ClientPacketHandler.handleSyncAllowed(msg.names);
    }

    public List<String> getNames() { return names; }
}
