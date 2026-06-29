package com.villagershop.network;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.menu.ShopStockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

/** C2S : bascule entre l'écran Config et l'écran Stock du même comptoir. */
public class OpenSubScreenPacket {
    private final BlockPos pos;
    private final boolean stock; // true = Stock, false = Config

    public OpenSubScreenPacket(BlockPos pos, boolean stock) {
        this.pos = pos;
        this.stock = stock;
    }

    public static void encode(OpenSubScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.stock);
    }

    public static OpenSubScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenSubScreenPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(OpenSubScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(msg.pos) instanceof ShopBlockEntity be) || !be.canAccess(player)) return;

            if (msg.stock) {
                NetworkHooks.openScreen(player,
                        new SimpleMenuProvider((id, inv, p) -> new ShopStockMenu(id, inv, be),
                                Component.translatable("container.villagershop.stock")),
                        buf -> buf.writeBlockPos(msg.pos));
            } else {
                // be est lui-même le MenuProvider de l'écran de config
                NetworkHooks.openScreen(player, be, buf -> {
                    buf.writeBlockPos(msg.pos);
                    buf.writeUtf(be.getShopName());
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
