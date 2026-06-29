package com.villagershop.menu;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Écran Stock : slot coffres d'amélioration + stock défilant (2 lignes visibles) +
 * inventaire joueur. Séparé de l'écran de config pour garder les deux compacts.
 */
public class ShopStockMenu extends AbstractContainerMenu implements ScrollableStorageMenu {
    public static final int CHEST_SLOT_INDEX = 0;
    public static final int STORAGE_START = 1;
    public static final int VISIBLE_ROWS = 2;
    public static final int STORAGE_VISIBLE = VISIBLE_ROWS * 9; // 18
    public static final int STORAGE_END = STORAGE_START + STORAGE_VISIBLE; // 19
    public static final int INV_START = STORAGE_END;            // 19
    public static final int INV_END = INV_START + 36;           // 55

    public static final int CHEST_SLOT_X = 8;
    public static final int CHEST_SLOT_Y = 18;
    public static final int STORAGE_X = 8;
    public static final int STORAGE_Y = 50;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 100;

    @Nullable
    private final ShopBlockEntity be;
    private final IItemHandler chestHandler;
    private final BlockPos pos;
    private int scrollOffset = 0;

    /** Constructeur CLIENT. */
    public ShopStockMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, null, buf.readBlockPos(),
                new ItemStackHandler(ShopBlockEntity.MAX_STORAGE), new ItemStackHandler(1));
    }

    /** Constructeur SERVEUR. */
    public ShopStockMenu(int id, Inventory inv, ShopBlockEntity be) {
        this(id, inv, be, be.getBlockPos(), be.getStorage(), be.getChestUpgrade());
    }

    private ShopStockMenu(int id, Inventory inv, @Nullable ShopBlockEntity be, BlockPos pos,
                          IItemHandlerModifiable storage, IItemHandler chestHandler) {
        super(ModMenus.SHOP_STOCK.get(), id);
        this.be = be;
        this.pos = pos;
        this.chestHandler = chestHandler;

        addSlot(new SlotItemHandler(chestHandler, 0, CHEST_SLOT_X, CHEST_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.CHEST);
            }

            @Override
            public int getMaxStackSize() {
                return ShopBlockEntity.MAX_CHESTS;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return ShopBlockEntity.MAX_CHESTS;
            }
        });

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int grid = row * 9 + col;
                addSlot(new DynamicStorageSlot(this, storage, grid,
                        STORAGE_X + col * 18, STORAGE_Y + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public int getChestCount() {
        return Math.min(ShopBlockEntity.MAX_CHESTS, chestHandler.getStackInSlot(0).getCount());
    }

    @Override
    public int getActiveCapacity() {
        int chests = getChestCount();
        return chests == 0 ? ShopBlockEntity.BASE_CAPACITY : ShopBlockEntity.SLOTS_PER_CHEST * chests;
    }

    public int getRows() {
        return (int) Math.ceil(getActiveCapacity() / 9.0);
    }

    public int getMaxScroll() {
        return Math.max(0, getRows() - VISIBLE_ROWS);
    }

    @Override
    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScroll()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        setScrollOffset(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index == CHEST_SLOT_INDEX || (index >= STORAGE_START && index < STORAGE_END)) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (stack.is(Items.CHEST)) {
                moved = moveItemStackTo(stack, CHEST_SLOT_INDEX, CHEST_SLOT_INDEX + 1, false);
            }
            if (!stack.isEmpty()) {
                moved = moveItemStackTo(stack, STORAGE_START, STORAGE_END, false) || moved;
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null) return true;
        return be.canAccess(player) && player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    public BlockPos getPos() { return pos; }
}
