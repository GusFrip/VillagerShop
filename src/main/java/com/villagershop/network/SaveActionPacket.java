package com.villagershop.network;

import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S : bouton unique du module Sauvegarde. Le serveur décide : si un livre & plume
 * est présent -> export (crée un livre signé dans la case dédiée) ; sinon si un livre
 * signé est présent -> import de sa config.
 */
public class SaveActionPacket {
    private final BlockPos pos;

    public SaveActionPacket(BlockPos pos) { this.pos = pos; }

    public static void encode(SaveActionPacket msg, FriendlyByteBuf buf) { buf.writeBlockPos(msg.pos); }

    public static SaveActionPacket decode(FriendlyByteBuf buf) { return new SaveActionPacket(buf.readBlockPos()); }

    public static void handle(SaveActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be)) return;
            if (!be.canAccess(player) || !be.hasMemory()) return;
            String key = be.doSaveAction();
            player.displayClientMessage(Component.translatable(key), true);
            if ("message.villagershop.import_ok".equals(key)) {
                ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
                if (player.containerMenu instanceof com.villagershop.menu.ShopConfigMenu m
                        && m.getPos().equals(msg.pos)) {
                    m.reloadTemplatesFromBE();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
