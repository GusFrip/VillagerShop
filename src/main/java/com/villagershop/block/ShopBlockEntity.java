package com.villagershop.block;

import com.villagershop.data.ShopOffer;
import com.villagershop.menu.ShopConfigMenu;
import com.villagershop.registry.ModVillagers;
import com.villagershop.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Le "cerveau" d'une boutique. Persiste : propriétaire, joueurs autorisés, offres,
 * un slot d'amélioration "coffres" (0 à 8) et un stock dont la capacité dépend du
 * nombre de coffres : 0 coffre = 3 emplacements, sinon 27 × (nb coffres).
 */
public class ShopBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_OFFERS = 8;
    public static final int BASE_CAPACITY = 3;
    public static final int SLOTS_PER_CHEST = 27;
    public static final int MAX_CHESTS = 8;
    public static final int MAX_STORAGE = SLOTS_PER_CHEST * MAX_CHESTS; // 216

    private final ItemStackHandler storage = new ItemStackHandler(MAX_STORAGE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Slot d'amélioration : contient les coffres installés (1 à 8). */
    private final ItemStackHandler chestUpgrade = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_CHESTS;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(Items.CHEST);
        }

        @Override
        protected void onContentsChanged(int slot) {
            onUpgradeChanged();
        }
    };

    /** Améliorations : 0=Nether Star (invincible), 1=Prismarine(tether)/Amethyst(statique). */
    private final ItemStackHandler upgrades = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.is(Items.NETHER_STAR);
                case 1 -> stack.is(Items.PRISMARINE_SHARD) || stack.is(Items.AMETHYST_SHARD);
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public enum MovementMode { FREE, TETHER, STATIC }

    private final LazyOptional<IItemHandler> storageCap =
            LazyOptional.of(() -> new CappedItemHandler(this, storage));

    @Nullable
    private UUID owner;
    private final Set<UUID> allowed = new HashSet<>();
    private final Map<UUID, String> nameCache = new HashMap<>();
    private final ShopOffer[] offers = new ShopOffer[MAX_OFFERS];
    private String shopName = "";

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOP_COUNTER.get(), pos, state);
        for (int i = 0; i < MAX_OFFERS; i++) offers[i] = new ShopOffer();
    }

    // ----- Capacité / coffres ---------------------------------------------

    public ItemStackHandler getChestUpgrade() { return chestUpgrade; }

    public ItemStackHandler getUpgrades() { return upgrades; }

    public boolean isImmortal() { return !upgrades.getStackInSlot(0).isEmpty(); }

    public MovementMode getMovementMode() {
        ItemStack s = upgrades.getStackInSlot(1);
        if (s.is(Items.AMETHYST_SHARD)) return MovementMode.STATIC;
        if (s.is(Items.PRISMARINE_SHARD)) return MovementMode.TETHER;
        return MovementMode.FREE;
    }

    public int getChestCount() {
        return Math.min(MAX_CHESTS, chestUpgrade.getStackInSlot(0).getCount());
    }

    public int getActiveCapacity() {
        int chests = getChestCount();
        return chests == 0 ? BASE_CAPACITY : SLOTS_PER_CHEST * chests;
    }

    private void onUpgradeChanged() {
        if (level != null && !level.isClientSide) ejectOverflow();
        setChanged();
    }

    /** Éjecte (lâche au sol) tout item situé au-delà de la capacité active. */
    private void ejectOverflow() {
        if (level == null) return;
        int cap = getActiveCapacity();
        for (int i = cap; i < MAX_STORAGE; i++) {
            ItemStack s = storage.getStackInSlot(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level,
                        getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.5,
                        s.copy());
                storage.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // ----- Propriété / permissions ----------------------------------------

    public void setOwner(UUID uuid, String name) {
        this.owner = uuid;
        if (name != null) nameCache.put(uuid, name);
        setChanged();
    }

    @Nullable
    public UUID getOwner() { return owner; }

    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public boolean canAccess(Player player) {
        if (owner == null) return true;
        return isOwner(player) || allowed.contains(player.getUUID());
    }

    public void addAllowed(UUID uuid, String name) {
        if (owner != null && owner.equals(uuid)) return;
        allowed.add(uuid);
        if (name != null) nameCache.put(uuid, name);
        setChanged();
    }

    public void removeAllowed(UUID uuid) {
        allowed.remove(uuid);
        setChanged();
    }

    public List<String> getAllowedNames() {
        List<String> names = new ArrayList<>();
        for (UUID id : allowed) names.add(nameCache.getOrDefault(id, id.toString()));
        return names;
    }

    public Set<UUID> getAllowed() { return allowed; }

    // ----- Offres ----------------------------------------------------------

    public ShopOffer getOffer(int index) {
        if (index < 0 || index >= MAX_OFFERS) return new ShopOffer();
        return offers[index];
    }

    public void setOffer(int index, ShopOffer offer) {
        if (index < 0 || index >= MAX_OFFERS) return;
        offers[index] = offer == null ? new ShopOffer() : offer;
        setChanged();
    }

    public ShopOffer[] getOffers() { return offers; }

    // ----- Nom du shop -----------------------------------------------------

    public String getShopName() { return shopName == null ? "" : shopName; }

    public void setShopName(String name) {
        this.shopName = name == null ? "" : name;
        setChanged();
    }

    // ----- Helpers de stock exposés (pour le marchand) ---------------------

    public int countStock(ItemStack like) {
        return countInStorage(like);
    }

    /** Dépose un paiement dans le stock ; lâche au sol le surplus si le stock est plein. */
    public void depositToStock(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remaining = stack.copy();
        int cap = getActiveCapacity();
        for (int i = 0; i < cap && !remaining.isEmpty(); i++) {
            remaining = storage.insertItem(i, remaining, false);
        }
        if (!remaining.isEmpty() && level != null && !level.isClientSide) {
            net.minecraft.world.Containers.dropItemStack(level,
                    getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.5,
                    remaining);
        }
        setChanged();
    }

    public void removeFromStock(ItemStack like, int amount) {
        extractFromStorage(like, amount);
    }

    // ----- Stock -----------------------------------------------------------

    public ItemStackHandler getStorage() { return storage; }

    private int countInStorage(ItemStack like) {
        int total = 0;
        int cap = getActiveCapacity();
        for (int i = 0; i < cap; i++) {
            ItemStack s = storage.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, like)) total += s.getCount();
        }
        return total;
    }

    private boolean storageCanAccept(ItemStack stack) {
        if (stack.isEmpty()) return true;
        int cap = getActiveCapacity();
        ItemStack remaining = stack.copy();
        for (int i = 0; i < cap && !remaining.isEmpty(); i++) {
            ItemStack slot = storage.getStackInSlot(i);
            if (slot.isEmpty()) {
                int max = Math.min(remaining.getMaxStackSize(), storage.getSlotLimit(i));
                remaining.shrink(Math.min(remaining.getCount(), max));
            } else if (ItemStack.isSameItemSameTags(slot, remaining)) {
                int space = Math.min(remaining.getMaxStackSize(), storage.getSlotLimit(i)) - slot.getCount();
                if (space > 0) remaining.shrink(Math.min(remaining.getCount(), space));
            }
        }
        return remaining.isEmpty();
    }

    private void insertIntoStorage(ItemStack stack) {
        int cap = getActiveCapacity();
        for (int i = 0; i < cap && !stack.isEmpty(); i++) {
            stack = storage.insertItem(i, stack, false);
        }
    }

    private void extractFromStorage(ItemStack like, int amount) {
        int remaining = amount;
        int cap = getActiveCapacity();
        for (int i = 0; i < cap && remaining > 0; i++) {
            ItemStack s = storage.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, like)) {
                int take = Math.min(remaining, s.getCount());
                storage.extractItem(i, take, false);
                remaining -= take;
            }
        }
    }

    // ----- Transaction (serveur) ------------------------------------------

    public boolean canFulfill(ShopOffer offer) {
        if (!offer.isValid()) return false;
        if (countInStorage(offer.getResult()) < offer.getResult().getCount()) return false;
        if (!storageCanAccept(offer.getPriceA())) return false;
        if (!offer.getPriceB().isEmpty() && !storageCanAccept(offer.getPriceB())) return false;
        return true;
    }

    private int countInPlayer(Player player, ItemStack like) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, like)) total += s.getCount();
        }
        return total;
    }

    private void removeFromPlayer(Player player, ItemStack like, int amount) {
        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, like)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    public boolean tryTrade(int offerIndex, Player player) {
        if (level == null || level.isClientSide) return false;
        if (offerIndex < 0 || offerIndex >= MAX_OFFERS) return false;
        ShopOffer offer = offers[offerIndex];
        if (!canFulfill(offer)) return false;

        if (countInPlayer(player, offer.getPriceA()) < offer.getPriceA().getCount()) return false;
        if (!offer.getPriceB().isEmpty()
                && countInPlayer(player, offer.getPriceB()) < offer.getPriceB().getCount()) return false;

        removeFromPlayer(player, offer.getPriceA(), offer.getPriceA().getCount());
        insertIntoStorage(offer.getPriceA().copy());
        if (!offer.getPriceB().isEmpty()) {
            removeFromPlayer(player, offer.getPriceB(), offer.getPriceB().getCount());
            insertIntoStorage(offer.getPriceB().copy());
        }

        extractFromStorage(offer.getResult(), offer.getResult().getCount());
        ItemStack reward = offer.getResult().copy();
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        setChanged();
        return true;
    }

    // ----- NBT -------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Storage", storage.serializeNBT());
        tag.put("ChestUpgrade", chestUpgrade.serializeNBT());
        tag.put("Upgrades", upgrades.serializeNBT());
        if (owner != null) tag.putUUID("Owner", owner);

        ListTag allowedList = new ListTag();
        for (UUID id : allowed) {
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", id);
            e.putString("Name", nameCache.getOrDefault(id, ""));
            allowedList.add(e);
        }
        tag.put("Allowed", allowedList);
        if (owner != null && nameCache.containsKey(owner)) tag.putString("OwnerName", nameCache.get(owner));

        ListTag offersList = new ListTag();
        for (int i = 0; i < MAX_OFFERS; i++) {
            CompoundTag e = offers[i].save();
            e.putInt("Index", i);
            offersList.add(e);
        }
        tag.put("Offers", offersList);
        tag.putString("ShopName", shopName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Storage")) storage.deserializeNBT(tag.getCompound("Storage"));
        if (tag.contains("ChestUpgrade")) chestUpgrade.deserializeNBT(tag.getCompound("ChestUpgrade"));
        if (tag.contains("Upgrades")) upgrades.deserializeNBT(tag.getCompound("Upgrades"));
        shopName = tag.getString("ShopName");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;

        allowed.clear();
        nameCache.clear();
        ListTag allowedList = tag.getList("Allowed", Tag.TAG_COMPOUND);
        for (int i = 0; i < allowedList.size(); i++) {
            CompoundTag e = allowedList.getCompound(i);
            UUID id = e.getUUID("Id");
            allowed.add(id);
            nameCache.put(id, e.getString("Name"));
        }
        if (owner != null && tag.contains("OwnerName")) nameCache.put(owner, tag.getString("OwnerName"));

        for (int i = 0; i < MAX_OFFERS; i++) offers[i] = new ShopOffer();
        ListTag offersList = tag.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < offersList.size(); i++) {
            CompoundTag e = offersList.getCompound(i);
            int idx = e.getInt("Index");
            if (idx >= 0 && idx < MAX_OFFERS) offers[idx] = ShopOffer.load(e);
        }
    }

    /** Lâche tout (stock + coffres installés) quand le bloc est cassé. */
    public void dropContents() {
        if (level == null) return;
        SimpleContainer c = new SimpleContainer(MAX_STORAGE + 4);
        for (int i = 0; i < MAX_STORAGE; i++) c.setItem(i, storage.getStackInSlot(i));
        c.setItem(MAX_STORAGE, chestUpgrade.getStackInSlot(0));
        for (int i = 0; i < upgrades.getSlots(); i++) c.setItem(MAX_STORAGE + 1 + i, upgrades.getStackInSlot(i));
        Containers.dropContents(level, getBlockPos(), c);
    }

    // ----- Propriétaires (pour le message de mort) ------------------------

    public List<UUID> getOwnersAndCoowners() {
        List<UUID> list = new ArrayList<>();
        if (owner != null) list.add(owner);
        list.addAll(allowed);
        return list;
    }

    public String getShopNameOrDefault() {
        String n = getShopName();
        return (n == null || n.isBlank()) ? "Shop" : n;
    }

    // ----- Application des upgrades au villageois --------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShopBlockEntity be) {
        if (level.getGameTime() % 20L != 0L) return;
        be.applyToVillager(level, pos, state);
    }

    private void applyToVillager(Level level, BlockPos pos, BlockState state) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(16.0);
        for (Villager v : level.getEntitiesOfClass(Villager.class, box)) {
            if (!isThisShopkeeper(v, level, pos)) continue;
            v.setInvulnerable(isImmortal());
            switch (getMovementMode()) {
                case STATIC -> {
                    net.minecraft.core.Direction facing = state.getValue(ShopBlock.FACING);
                    net.minecraft.core.BlockPos front = pos.relative(facing);
                    double tx = front.getX() + 0.5D, tz = front.getZ() + 0.5D;
                    if (v.distanceToSqr(tx, v.getY(), tz) > 2.0D) {
                        // pas encore devant le comptoir : il s'y rend
                        if (v.isNoAi()) v.setNoAi(false);
                        if (v.isPassenger()) v.stopRiding();
                        v.clearRestriction();
                        v.getNavigation().moveTo(tx, front.getY(), tz, 0.5D);
                    } else {
                        // arrivé : se cale devant, face au livre, puis se fige
                        float yaw = facing.getOpposite().toYRot();
                        v.getNavigation().stop();
                        v.moveTo(tx, front.getY(), tz, yaw, 0.0F);
                        v.setYBodyRot(yaw);
                        v.setYHeadRot(yaw);
                        v.setNoAi(true);
                    }
                }
                case TETHER -> {
                    if (v.isNoAi()) v.setNoAi(false);
                    v.restrictTo(pos, 3);
                    double cx = pos.getX() + 0.5D, cz = pos.getZ() + 0.5D;
                    if (v.distanceToSqr(cx, v.getY(), cz) > 9.0D) {
                        if (v.isPassenger()) v.stopRiding();
                        v.getNavigation().moveTo(cx, pos.getY(), cz, 0.6D);
                    }
                }
                case FREE -> { if (v.isNoAi()) v.setNoAi(false); v.clearRestriction(); }
            }
        }
    }

    /** Rend le villageois à son état normal (à la casse du comptoir). */
    public void revertVillager(Level level, BlockPos pos) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(16.0);
        for (Villager v : level.getEntitiesOfClass(Villager.class, box)) {
            if (!isThisShopkeeper(v, level, pos)) continue;
            v.setInvulnerable(false);
            if (v.isNoAi()) v.setNoAi(false);
            v.clearRestriction();
            // libère le villageois : son comptoir n'existe plus
            v.setVillagerData(v.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.NONE));
            v.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        }
    }

    private static boolean isThisShopkeeper(Villager v, Level level, BlockPos pos) {
        if (v.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER.get()) return false;
        var js = v.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return js.isPresent() && js.get().pos().equals(pos) && js.get().dimension().equals(level.dimension());
    }

    // ----- Capabilities ----------------------------------------------------

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return storageCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        storageCap.invalidate();
    }

    // ----- MenuProvider ----------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.villagershop.config");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ShopConfigMenu(id, inv, this);
    }
}
