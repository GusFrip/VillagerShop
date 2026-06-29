package com.villagershop.network;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S : le client demande la liste à jour des joueurs autorisés (à l'ouverture du GUI). */
public class RequestAllowedPacket {
    private final BlockPos pos;

    public RequestAllowedPacket(BlockPos pos) { this.pos = pos; }

    public static void encode(RequestAllowedPacket msg, FriendlyByteBuf buf) { buf.writeBlockPos(msg.pos); }

    public static RequestAllowedPacket decode(FriendlyByteBuf buf) { return new RequestAllowedPacket(buf.readBlockPos()); }

    public static void handle(RequestAllowedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be && be.canAccess(player)) {
                ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
