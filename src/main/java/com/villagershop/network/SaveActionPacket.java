package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S : bouton unique du module Sauvegarde. Le serveur décide : si un livre & plume
 * est présent -> export (crée un livre signé dans la case dédiée) ; sinon si un livre
 * signé est présent -> import de sa config.
 */
public record SaveActionPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<SaveActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "save_action"));

    public static final StreamCodec<FriendlyByteBuf, SaveActionPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public SaveActionPacket decode(FriendlyByteBuf buf) { return new SaveActionPacket(buf.readBlockPos()); }
        @Override public void encode(FriendlyByteBuf buf, SaveActionPacket msg) { buf.writeBlockPos(msg.pos); }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SaveActionPacket msg, ServerPlayer player) {
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
    }
}
