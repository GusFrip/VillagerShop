package com.villagershop.network;

import com.villagershop.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S2C : pousse la liste des noms autorisés vers le client pour affichage. */
public class SyncAllowedPacket {
    private final List<String> names;

    public SyncAllowedPacket(List<String> names) { this.names = names; }

    public static void encode(SyncAllowedPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.names.size());
        for (String n : msg.names) buf.writeUtf(n);
    }

    public static SyncAllowedPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readUtf());
        return new SyncAllowedPacket(list);
    }

    public static void handle(SyncAllowedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSyncAllowed(msg.names)));
        ctx.get().setPacketHandled(true);
    }

    public List<String> getNames() { return names; }
}
