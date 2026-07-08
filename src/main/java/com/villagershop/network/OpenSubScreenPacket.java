package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import com.villagershop.menu.ShopStockMenuFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S : bascule entre l'écran Config et l'écran Stock du même comptoir. */
public record OpenSubScreenPacket(BlockPos pos, boolean stock) implements CustomPacketPayload {
    public static final Type<OpenSubScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "open_sub_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenSubScreenPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override public OpenSubScreenPacket decode(FriendlyByteBuf buf) {
            return new OpenSubScreenPacket(buf.readBlockPos(), buf.readBoolean());
        }
        @Override public void encode(FriendlyByteBuf buf, OpenSubScreenPacket msg) {
            buf.writeBlockPos(msg.pos);
            buf.writeBoolean(msg.stock);
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenSubScreenPacket msg, ServerPlayer player) {
        if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be) || !be.canAccess(player)) return;

        if (msg.stock) {
            player.openMenu(new ShopStockMenuFactory(be));
        } else {
            // be est lui-même l'ExtendedScreenHandlerFactory de l'écran de config
            player.openMenu(be);
        }
    }
}
