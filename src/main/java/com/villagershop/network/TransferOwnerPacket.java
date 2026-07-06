package com.villagershop.network;

import com.mojang.authlib.GameProfile;
import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/**
 * C2S : le propriétaire transfère la boutique à un co-propriétaire existant
 * (Maj+clic sur ★ dans la liste). L'ancien proprio devient co-proprio.
 */
public record TransferOwnerPacket(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<TransferOwnerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "transfer_owner"));

    public static final StreamCodec<FriendlyByteBuf, TransferOwnerPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public TransferOwnerPacket decode(FriendlyByteBuf buf) {
            return new TransferOwnerPacket(buf.readBlockPos(), buf.readUtf());
        }
        @Override public void encode(FriendlyByteBuf buf, TransferOwnerPacket msg) {
            buf.writeBlockPos(msg.pos);
            buf.writeUtf(msg.name);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TransferOwnerPacket msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be)) return;
        if (!be.isOwner(player)) {
            player.displayClientMessage(Component.translatable("message.villagershop.not_owner"), true);
            return;
        }
        if (!be.hasAccess()) {
            player.displayClientMessage(Component.translatable("message.villagershop.no_access_module"), true);
            return;
        }
        MinecraftServer server = player.server;
        ServerPlayer online = server.getPlayerList().getPlayerByName(msg.name);
        java.util.UUID uuid = null;
        if (online != null) {
            uuid = online.getUUID();
        } else {
            Optional<GameProfile> profile = server.getProfileCache() == null
                    ? Optional.empty() : server.getProfileCache().get(msg.name);
            if (profile.isPresent()) uuid = profile.get().getId();
        }
        if (uuid == null || !be.transferOwner(uuid)) {
            player.displayClientMessage(Component.translatable("message.villagershop.transfer_fail"), true);
            return;
        }
        player.displayClientMessage(Component.translatable("message.villagershop.transfer_ok", msg.name), false);
        if (online != null) {
            online.displayClientMessage(Component.translatable("message.villagershop.transfer_received",
                    player.getGameProfile().getName(), be.getShopNameOrDefault()), false);
        }
        ModNetwork.sendTo(new SyncAllowedPacket(be.getAllowedNames()), player);
    }
}
