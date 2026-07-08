package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Fabric : fournisseur du menu Stock avec sa donnée d'ouverture (BlockPos). */
public record ShopStockMenuFactory(ShopBlockEntity be) implements ExtendedScreenHandlerFactory<BlockPos> {
    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return be.getBlockPos();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.villagershop.stock");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ShopStockMenu(id, inv, be);
    }
}
