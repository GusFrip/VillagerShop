package com.villagershop.block;

import com.villagershop.data.ShopOffer;
import com.villagershop.menu.ShopConfigMenu;
import com.villagershop.registry.ModVillagers;
import com.villagershop.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
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

    /** Slot d'amélioration : blocs d'émeraude (0 à 7), chacun débloque une offre supplémentaire. */
    private final ItemStackHandler tradeUpgrade = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return MAX_OFFERS - 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(Items.EMERALD_BLOCK);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Améliorations : 0=Nether Star, 1=déplacement, 2=comm, 3=mémoire, 4=accès, 5=anti-explosion. */
    private final ItemStackHandler upgrades = new ItemStackHandler(6) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.is(Items.NETHER_STAR);
                case 1 -> stack.is(Items.PRISMARINE_SHARD) || stack.is(Items.AMETHYST_SHARD);
                case 2 -> stack.is(Items.LIGHTNING_ROD);
                case 3 -> stack.is(Items.BOOKSHELF);
                case 4 -> stack.is(Items.GOLD_BLOCK);
                case 5 -> stack.is(Items.OBSIDIAN);
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            // Taille fixe (6) : on ignore le "Size" d'anciennes sauvegardes
            // pour ne pas rétrécir le handler et planter à l'ajout des slots récents.
            setSize(6);
            ListTag items = nbt.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag it = items.getCompound(i);
                int slot = it.getInt("Slot");
                if (slot >= 0 && slot < 6) setStackInSlot(slot, ItemStack.parseOptional(provider, it));
            }
        }
    };

    private final ItemStackHandler saveSlot = new ItemStackHandler(2) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 ? stack.is(Items.WRITABLE_BOOK) : stack.is(Items.WRITTEN_BOOK);
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            setSize(2); // ignore un éventuel Size=1 d'anciennes sauvegardes dev
            ListTag items = nbt.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag it = items.getCompound(i);
                int slot = it.getInt("Slot");
                if (slot >= 0 && slot < 2) setStackInSlot(slot, ItemStack.parseOptional(provider, it));
            }
        }
    };

    public ItemStackHandler getSaveSlot() { return saveSlot; }

    public enum MovementMode { FREE, TETHER, STATIC }

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

    public ItemStackHandler getTradeUpgrade() { return tradeUpgrade; }

    /** Nombre d'offres actives : 1 de base + 1 par bloc d'émeraude (max 8). */
    public int getActiveOfferCount() {
        return Math.min(MAX_OFFERS, 1 + Math.min(MAX_OFFERS - 1, tradeUpgrade.getStackInSlot(0).getCount()));
    }

    public ItemStackHandler getUpgrades() { return upgrades; }

    public boolean isImmortal() { return !upgrades.getStackInSlot(0).isEmpty(); }

    public MovementMode getMovementMode() {
        ItemStack s = upgrades.getStackInSlot(1);
        if (s.is(Items.AMETHYST_SHARD)) return MovementMode.STATIC;
        if (s.is(Items.PRISMARINE_SHARD)) return MovementMode.TETHER;
        return MovementMode.FREE;
    }

    /** Paratonnerre installé : le propriétaire reçoit les notifications. */
    public boolean hasNotifier() { return !upgrades.getStackInSlot(2).isEmpty(); }

    /** Bibliothèque : module de sauvegarde (export/import livre) accessible. */
    public boolean hasMemory() { return !upgrades.getStackInSlot(3).isEmpty(); }

    /** Bloc d'or : gestion des co-propriétaires accessible. */
    public boolean hasAccess() { return !upgrades.getStackInSlot(4).isEmpty(); }

    /** Obsidienne : le comptoir résiste aux explosions. */
    public boolean hasBlastProtection() { return !upgrades.getStackInSlot(5).isEmpty(); }

    /** Casse autorisée : proprio/co-proprios (mêmes règles que l'accès config) ou joueur en créatif. */
    public boolean canBreak(Player player) {
        return player.isCreative() || canAccess(player);
    }

    /** Sérialise la config (nom, offres, co-proprios) — pour le livre de sauvegarde. */
    public CompoundTag exportConfig(HolderLookup.Provider registries) {
        CompoundTag cfg = new CompoundTag();
        cfg.putString("ShopName", shopName);
        ListTag off = new ListTag();
        for (int i = 0; i < MAX_OFFERS; i++) { CompoundTag e = offers[i].save(registries); e.putInt("Index", i); off.add(e); }
        cfg.put("Offers", off);
        ListTag al = new ListTag();
        for (UUID id : allowed) { CompoundTag e = new CompoundTag(); e.putUUID("Id", id); e.putString("Name", nameCache.getOrDefault(id, "")); al.add(e); }
        cfg.put("Allowed", al);
        return cfg;
    }

    /** Applique une config importée (n'écrase pas le propriétaire). */
    public void importConfig(HolderLookup.Provider registries, CompoundTag cfg) {
        if (cfg.contains("ShopName")) shopName = cfg.getString("ShopName");
        for (int i = 0; i < MAX_OFFERS; i++) offers[i] = new ShopOffer();
        ListTag off = cfg.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < off.size(); i++) { CompoundTag e = off.getCompound(i); int idx = e.getInt("Index"); if (idx >= 0 && idx < MAX_OFFERS) offers[idx] = ShopOffer.load(registries, e); }
        allowed.clear();
        ListTag al = cfg.getList("Allowed", Tag.TAG_COMPOUND);
        for (int i = 0; i < al.size(); i++) { CompoundTag e = al.getCompound(i); UUID id = e.getUUID("Id"); allowed.add(id); nameCache.put(id, e.getString("Name")); }
        setChanged();
    }

    private static final String PAGE_MARKER = "VILLAGERSHOP:";

    /**
     * Construit un LIVRE SIGNÉ unique contenant la config.
     * 1.21 : les données custom vivent dans DataComponents.CUSTOM_DATA (accès rapide)
     * et en secours dans une page du livre (survit à la copie vanilla, qui ne
     * conserve que WRITTEN_BOOK_CONTENT).
     */
    private ItemStack buildSignedBook(HolderLookup.Provider registries) {
        CompoundTag cfg = exportConfig(registries);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        CompoundTag custom = new CompoundTag();
        custom.put("ShopConfig", cfg);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));

        String title = "Save: " + getShopNameOrDefault();
        if (title.length() > 32) title = title.substring(0, 32);
        List<Filterable<Component>> pages = List.of(
                Filterable.passThrough(Component.literal(
                        "VillagerShop — " + getShopNameOrDefault() + "\n\nLivre de sauvegarde.\nDépose-le dans le slot livre signé d'un comptoir (upgrade mémoire) puis clique Save / Load.")),
                Filterable.passThrough(Component.literal(PAGE_MARKER + cfg)));
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(title), getShopNameOrDefault(), 0, pages, true));
        return book;
    }

    /** Lit la config depuis un livre signé : custom data direct, sinon depuis les pages (copie). */
    private boolean readConfigFrom(HolderLookup.Provider registries, ItemStack book) {
        if (book.isEmpty()) return false;
        CustomData cd = book.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag t = cd.copyTag();
            if (t.contains("ShopConfig")) { importConfig(registries, t.getCompound("ShopConfig")); return true; }
        }
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content != null) {
            for (Component page : content.getPages(false)) {
                String text = page.getString();
                int idx = text.indexOf(PAGE_MARKER);
                if (idx >= 0) {
                    try {
                        importConfig(registries, net.minecraft.nbt.TagParser.parseTag(text.substring(idx + PAGE_MARKER.length())));
                        return true;
                    } catch (Exception e) { return false; }
                }
            }
        }
        return false;
    }

    /** Bouton unique : exporte (livre & plume -> livre signé dans le 2e slot) ou importe (livre signé). */
    public String doSaveAction() {
        if (level == null) return "message.villagershop.save_empty";
        HolderLookup.Provider registries = level.registryAccess();
        ItemStack quill = saveSlot.getStackInSlot(0);
        ItemStack signed = saveSlot.getStackInSlot(1);
        if (quill.is(Items.WRITABLE_BOOK)) {
            if (!signed.isEmpty()) return "message.villagershop.save_slot_busy";
            saveSlot.setStackInSlot(1, buildSignedBook(registries));
            saveSlot.setStackInSlot(0, ItemStack.EMPTY);
            setChanged();
            return "message.villagershop.export_ok";
        }
        if (signed.is(Items.WRITTEN_BOOK)) {
            return readConfigFrom(registries, signed) ? "message.villagershop.import_ok" : "message.villagershop.import_fail";
        }
        return "message.villagershop.save_empty";
    }

    // Drapeaux anti-spam (non persistés) : état "déjà notifié" par type de blocage.
    private boolean supplyFlagged = false;
    private boolean stockFlagged = false;

    /** Une vente est bloquée par manque de MARCHANDISE (result indisponible). */
    public boolean hasSupplyProblem() {
        for (int i = 0; i < getActiveOfferCount(); i++) {
            ShopOffer o = offers[i];
            if (!o.isValid()) continue;
            if (countInStorage(o.getResult()) < o.getResult().getCount()) return true;
        }
        return false;
    }

    /** Une vente est bloquée par manque de PLACE pour encaisser le paiement (stock plein). */
    public boolean hasStockProblem() {
        for (int i = 0; i < getActiveOfferCount(); i++) {
            ShopOffer o = offers[i];
            if (!o.isValid()) continue;
            if (countInStorage(o.getResult()) >= o.getResult().getCount() && maxTrades(o) == 0) return true;
        }
        return false;
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
        if (isOwner(player)) return true;
        return hasAccess() && allowed.contains(player.getUUID());
    }

    public void addAllowed(UUID uuid, String name) {
        if (owner != null && owner.equals(uuid)) return;
        allowed.add(uuid);
        if (name != null) nameCache.put(uuid, name);
        setChanged();
    }


    /** Transfère la propriété à un co-proprio existant ; l'ancien proprio devient co-proprio. */
    public boolean transferOwner(UUID newOwner) {
        if (newOwner == null || !allowed.contains(newOwner)) return false;
        allowed.remove(newOwner);
        if (owner != null) allowed.add(owner);
        owner = newOwner;
        setChanged();
        return true;
    }

    public void removeAllowed(UUID uuid) {
        allowed.remove(uuid);
        setChanged();
    }

    public List<String> getAllowedNames() {
        List<String> names = new ArrayList<>();
        if (owner != null) names.add(nameCache.getOrDefault(owner, "Owner")); // owner en tête, non supprimable
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
        // Deux passes (comme simInsert) : compléter les piles existantes d'abord,
        // sinon le dernier paiement atterrit dans un slot fraîchement vidé au lieu
        // de compléter la pile en cours (ex. 63+1 au lieu de 64).
        ItemStack remaining = insertStacked(stack.copy());
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
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, like)) total += s.getCount();
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
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = Math.min(remaining.getMaxStackSize(), storage.getSlotLimit(i)) - slot.getCount();
                if (space > 0) remaining.shrink(Math.min(remaining.getCount(), space));
            }
        }
        return remaining.isEmpty();
    }

    private void insertIntoStorage(ItemStack stack) {
        insertStacked(stack);
    }

    /** Insertion en deux passes : piles existantes du même item, puis slots vides.
     *  Renvoie ce qui n'a pas trouvé de place. Même ordre que la simulation simInsert. */
    private ItemStack insertStacked(ItemStack stack) {
        int cap = getActiveCapacity();
        for (int i = 0; i < cap && !stack.isEmpty(); i++) {
            ItemStack s = storage.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                stack = storage.insertItem(i, stack, false);
            }
        }
        for (int i = 0; i < cap && !stack.isEmpty(); i++) {
            if (storage.getStackInSlot(i).isEmpty()) {
                stack = storage.insertItem(i, stack, false);
            }
        }
        return stack;
    }

    private void extractFromStorage(ItemStack like, int amount) {
        int remaining = amount;
        int cap = getActiveCapacity();
        for (int i = 0; i < cap && remaining > 0; i++) {
            ItemStack s = storage.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, like)) {
                int take = Math.min(remaining, s.getCount());
                storage.extractItem(i, take, false);
                remaining -= take;
            }
        }
    }

    /**
     * Nombre maximum d'achats possibles pour cette offre SANS aucune perte : il faut
     * à la fois assez de marchandise en stock ET assez de place pour encaisser le(s)
     * paiement(s). On simule les transactions successives sur une copie du stock.
     */
    public int maxTrades(ShopOffer offer) {
        if (!offer.isValid()) return 0;
        int cap = getActiveCapacity();
        int resultPer = offer.getResult().getCount();
        if (resultPer <= 0) return 0;

        ItemStack[] sim = new ItemStack[cap];
        int[] limit = new int[cap];
        for (int i = 0; i < cap; i++) {
            sim[i] = storage.getStackInSlot(i).copy();
            limit[i] = storage.getSlotLimit(i);
        }

        ItemStack priceA = offer.getPriceA();
        ItemStack priceB = offer.getPriceB();

        int trades = 0;
        int safety = cap * 64 + 1;
        while (trades < safety) {
            if (simCount(sim, offer.getResult()) < resultPer) break;
            // copie de l'itération : on n'applique que si tout passe
            ItemStack[] next = new ItemStack[cap];
            int[] lim = new int[cap];
            for (int i = 0; i < cap; i++) { next[i] = sim[i].copy(); lim[i] = limit[i]; }
            simRemove(next, offer.getResult(), resultPer);
            if (!simInsert(next, lim, priceA)) break;
            if (!priceB.isEmpty() && !simInsert(next, lim, priceB)) break;
            sim = next; limit = lim;
            trades++;
        }
        return trades;
    }

    private int simCount(ItemStack[] sim, ItemStack like) {
        int t = 0;
        for (ItemStack s : sim) if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, like)) t += s.getCount();
        return t;
    }

    private void simRemove(ItemStack[] sim, ItemStack like, int amount) {
        for (int i = 0; i < sim.length && amount > 0; i++) {
            ItemStack s = sim[i];
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, like)) {
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                if (s.isEmpty()) sim[i] = ItemStack.EMPTY;
                amount -= take;
            }
        }
    }

    /** Simule une insertion (piles existantes puis slots vides) ; true si tout rentre. */
    private boolean simInsert(ItemStack[] sim, int[] limit, ItemStack stack) {
        if (stack.isEmpty()) return true;
        int remaining = stack.getCount();
        int maxStack = stack.getMaxStackSize();
        for (int i = 0; i < sim.length && remaining > 0; i++) {
            ItemStack s = sim[i];
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                int space = Math.min(maxStack, limit[i]) - s.getCount();
                if (space > 0) { int add = Math.min(space, remaining); s.grow(add); remaining -= add; }
            }
        }
        for (int i = 0; i < sim.length && remaining > 0; i++) {
            if (sim[i].isEmpty()) {
                int add = Math.min(Math.min(maxStack, limit[i]), remaining);
                ItemStack ns = stack.copy(); ns.setCount(add);
                sim[i] = ns; remaining -= add;
            }
        }
        return remaining <= 0;
    }

    // ----- Transaction (serveur) ------------------------------------------

    public boolean canFulfill(ShopOffer offer) {
        // Simulation exacte (retrait marchandise puis encaissement des DEUX paiements) :
        // évite les faux positifs de l'ancien check slot par slot.
        return offer.isValid() && maxTrades(offer) >= 1;
    }

    private int countInPlayer(Player player, ItemStack like) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, like)) total += s.getCount();
        }
        return total;
    }

    private void removeFromPlayer(Player player, ItemStack like, int amount) {
        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, like)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    public boolean tryTrade(int offerIndex, Player player) {
        if (level == null || level.isClientSide) return false;
        if (offerIndex < 0 || offerIndex >= getActiveOfferCount()) return false;
        ShopOffer offer = offers[offerIndex];
        if (!canFulfill(offer)) return false;

        if (countInPlayer(player, offer.getPriceA()) < offer.getPriceA().getCount()) return false;
        if (!offer.getPriceB().isEmpty()
                && countInPlayer(player, offer.getPriceB()) < offer.getPriceB().getCount()) return false;

        // Marchandise retirée AVANT d'encaisser : même ordre que la simulation maxTrades.
        extractFromStorage(offer.getResult(), offer.getResult().getCount());
        removeFromPlayer(player, offer.getPriceA(), offer.getPriceA().getCount());
        insertIntoStorage(offer.getPriceA().copy());
        if (!offer.getPriceB().isEmpty()) {
            removeFromPlayer(player, offer.getPriceB(), offer.getPriceB().getCount());
            insertIntoStorage(offer.getPriceB().copy());
        }

        ItemStack reward = offer.getResult().copy();
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        setChanged();
        return true;
    }

    // ----- NBT -------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Storage", storage.serializeNBT(registries));
        tag.put("ChestUpgrade", chestUpgrade.serializeNBT(registries));
        tag.put("TradeUpgrade", tradeUpgrade.serializeNBT(registries));
        tag.put("Upgrades", upgrades.serializeNBT(registries));
        tag.put("SaveSlot", saveSlot.serializeNBT(registries));
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
            CompoundTag e = offers[i].save(registries);
            e.putInt("Index", i);
            offersList.add(e);
        }
        tag.put("Offers", offersList);
        tag.putString("ShopName", shopName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Storage")) storage.deserializeNBT(registries, tag.getCompound("Storage"));
        if (tag.contains("ChestUpgrade")) chestUpgrade.deserializeNBT(registries, tag.getCompound("ChestUpgrade"));
        if (tag.contains("TradeUpgrade")) tradeUpgrade.deserializeNBT(registries, tag.getCompound("TradeUpgrade"));
        if (tag.contains("Upgrades")) upgrades.deserializeNBT(registries, tag.getCompound("Upgrades"));
        if (tag.contains("SaveSlot")) saveSlot.deserializeNBT(registries, tag.getCompound("SaveSlot"));
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
            if (idx >= 0 && idx < MAX_OFFERS) offers[idx] = ShopOffer.load(registries, e);
        }
    }


    /** Synchronise le BE au client (chunk load) : le client connaît le proprio
     *  -> pas d'animation de casse fantôme sur un comptoir protégé. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /** Lâche tout (stock + coffres installés) quand le bloc est cassé. */
    public void dropContents() {
        if (level == null) return;
        SimpleContainer c = new SimpleContainer(MAX_STORAGE + 2 + upgrades.getSlots() + saveSlot.getSlots());
        for (int i = 0; i < MAX_STORAGE; i++) c.setItem(i, storage.getStackInSlot(i));
        c.setItem(MAX_STORAGE, chestUpgrade.getStackInSlot(0));
        c.setItem(MAX_STORAGE + 1, tradeUpgrade.getStackInSlot(0));
        for (int i = 0; i < upgrades.getSlots(); i++) c.setItem(MAX_STORAGE + 2 + i, upgrades.getStackInSlot(i));
        for (int i = 0; i < saveSlot.getSlots(); i++) c.setItem(MAX_STORAGE + 2 + upgrades.getSlots() + i, saveSlot.getStackInSlot(i));
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
        be.checkNotifications(level);
    }

    /** Détecte les transitions OK -> bloqué et notifie les proprios (si paratonnerre). */
    private void checkNotifications(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
        if (!hasNotifier()) { supplyFlagged = false; stockFlagged = false; return; }
        boolean sup = hasSupplyProblem();
        boolean st = hasStockProblem();
        if (sup && !supplyFlagged) {
            com.villagershop.notify.ShopNotifications.dispatch(sl, this,
                    com.villagershop.notify.ShopNotifications.Reason.SUPPLY);
        }
        supplyFlagged = sup;
        if (st && !stockFlagged) {
            com.villagershop.notify.ShopNotifications.dispatch(sl, this,
                    com.villagershop.notify.ShopNotifications.Reason.STOCK);
        }
        stockFlagged = st;
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

    @Nullable
    public Villager findShopkeeper() {
        if (level == null) return null;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(getBlockPos()).inflate(16.0);
        for (Villager v : level.getEntitiesOfClass(Villager.class, box)) {
            if (isThisShopkeeper(v, level, getBlockPos())) return v;
        }
        return null;
    }

    private static boolean isThisShopkeeper(Villager v, Level level, BlockPos pos) {
        if (v.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER.get()) return false;
        var js = v.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return js.isPresent() && js.get().pos().equals(pos) && js.get().dimension().equals(level.dimension());
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
