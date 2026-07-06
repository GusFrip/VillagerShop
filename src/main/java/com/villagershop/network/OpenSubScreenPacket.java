package com.villagershop.network;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import com.villagershop.menu.ShopStockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public static void handle(OpenSubScreenPacket msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be) || !be.canAccess(player)) return;

        if (msg.stock) {
            player.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new ShopStockMenu(id, inv, be),
                            Component.translatable("container.villagershop.stock")),
                    buf -> buf.writeBlockPos(msg.pos));
        } else {
            // be est lui-même le MenuProvider de l'écran de config
            player.openMenu(be, buf -> {
                buf.writeBlockPos(msg.pos);
                buf.writeUtf(be.getShopName());
            });
        }
    }
}
