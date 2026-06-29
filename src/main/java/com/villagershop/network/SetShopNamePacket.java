package com.villagershop.network;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S : le propriétaire (ou un joueur autorisé) définit le nom du shop. */
public class SetShopNamePacket {
    private final BlockPos pos;
    private final String name;

    public SetShopNamePacket(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name;
    }

    public static void encode(SetShopNamePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.name);
    }

    public static SetShopNamePacket decode(FriendlyByteBuf buf) {
        return new SetShopNamePacket(buf.readBlockPos(), buf.readUtf(256));
    }

    public static void handle(SetShopNamePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be && be.canAccess(player)) {
                String clean = msg.name.length() > 32 ? msg.name.substring(0, 32) : msg.name;
                be.setShopName(clean.trim());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
