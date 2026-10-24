package com.villagershop.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Valeurs de configuration EFFECTIVES, utilisées partout dans le mod (serveur et client).
 *
 * Le serveur les charge depuis son fichier de config (ShopConfigSpec) puis les pousse
 * à chaque client à la connexion (SyncConfigPacket) : c'est toujours la config du
 * serveur qui fait autorité, même si le client a un fichier différent.
 *
 * Les plafonds absolus (ABS_*) sont dictés par le design des interfaces et ne sont
 * pas configurables.
 */
public final class VSConfig {
    private VSConfig() {}

    // ----- Plafonds absolus (design des GUI) -------------------------------
    public static final int ABS_MAX_CHESTS = 16;
    public static final int ABS_MAX_STORAGE = 432;   // 16 coffres × 27
    public static final int ABS_MAX_OFFERS = 8;

    /** Améliorations, dans l'ordre des slots du comptoir (0..6), puis coffres et blocs d'émeraude. */
    public enum Upgrade {
        IMMORTALITY(0, "immortality", Items.NETHER_STAR),
        MOVEMENT(1, "movement", Items.PRISMARINE_SHARD),
        COMMUNICATION(2, "communication", Items.LIGHTNING_ROD),
        MEMORY(3, "memory", Items.BOOKSHELF),
        ACCESS(4, "access", Items.GOLD_BLOCK),
        BLAST_PROTECTION(5, "blast_protection", Items.OBSIDIAN),
        ADMIN(6, "admin", Items.COMMAND_BLOCK),
        STORAGE(-1, "storage", Items.CHEST),
        TRADES(-1, "trades", Items.EMERALD_BLOCK);

        /** Index dans le handler "upgrades" du comptoir, -1 pour coffres / émeraudes (slots dédiés). */
        public final int slot;
        public final String key;
        public final Item defaultItem;

        Upgrade(int slot, String key, Item defaultItem) {
            this.slot = slot;
            this.key = key;
            this.defaultItem = defaultItem;
        }

        public static Upgrade bySlot(int slot) {
            for (Upgrade u : values()) if (u.slot == slot) return u;
            return null;
        }
    }

    // ----- Valeurs effectives ---------------------------------------------
    private static final boolean[] enabled = new boolean[Upgrade.values().length];
    private static final Item[] items = new Item[Upgrade.values().length];
    /** Second item de l'amélioration Déplacement (mode "immobile"). */
    private static Item movementStaticItem = Items.AMETHYST_SHARD;
    private static int maxChests = 8;
    private static int slotsPerChest = 27;
    private static int maxOffers = ABS_MAX_OFFERS;

    static {
        resetDefaults();
    }

    public static void resetDefaults() {
        for (Upgrade u : Upgrade.values()) {
            enabled[u.ordinal()] = true;
            items[u.ordinal()] = u.defaultItem;
        }
        movementStaticItem = Items.AMETHYST_SHARD;
        maxChests = 8;
        slotsPerChest = 27;
        maxOffers = ABS_MAX_OFFERS;
    }

    // ----- Accès ------------------------------------------------------------

    public static boolean isEnabled(Upgrade u) { return enabled[u.ordinal()]; }

    /** Amélioration du slot {@code slot} du comptoir activée ? (slot inconnu = désactivé) */
    public static boolean isSlotEnabled(int slot) {
        Upgrade u = Upgrade.bySlot(slot);
        return u != null && isEnabled(u);
    }

    public static Item item(Upgrade u) { return items[u.ordinal()]; }

    public static Item movementStaticItem() { return movementStaticItem; }

    /** L'item est-il accepté par le slot {@code slot} du comptoir ? */
    public static boolean accepts(int slot, ItemStack stack) {
        Upgrade u = Upgrade.bySlot(slot);
        if (u == null || !isEnabled(u)) return false;
        if (stack.is(item(u))) return true;
        return u == Upgrade.MOVEMENT && stack.is(movementStaticItem);
    }

    public static int maxChests() { return isEnabled(Upgrade.STORAGE) ? maxChests : 0; }

    public static int slotsPerChest() { return slotsPerChest; }

    /** Capacité de stock pour {@code chests} coffres (plafonnée au maximum absolu). */
    public static int capacityFor(int chests, int baseCapacity) {
        int c = Math.min(chests, maxChests());
        if (c <= 0) return baseCapacity;
        return Math.min(ABS_MAX_STORAGE, c * slotsPerChest);
    }

    /** Nombre maximal d'offres (1 de base + blocs d'émeraude). */
    public static int maxOffers() { return isEnabled(Upgrade.TRADES) ? maxOffers : 1; }

    /** Blocs d'émeraude maximum dans le slot de déblocage. */
    public static int maxTradeBlocks() { return Math.max(0, maxOffers() - 1); }

    // ----- Chargement / synchro -------------------------------------------

    /**
     * Applique un jeu de valeurs (depuis le fichier de config serveur ou le paquet de synchro).
     * {@code itemIds} : un id par Upgrade (ordre de l'enum), {@code staticId} : second item Déplacement.
     */
    public static void apply(boolean[] enabledFlags, List<String> itemIds, String staticId,
                             int chests, int perChest, int offers) {
        for (Upgrade u : Upgrade.values()) {
            enabled[u.ordinal()] = enabledFlags[u.ordinal()];
            items[u.ordinal()] = parseItem(itemIds.get(u.ordinal()), u.defaultItem);
        }
        movementStaticItem = parseItem(staticId, Items.AMETHYST_SHARD);
        maxChests = clamp(chests, 0, ABS_MAX_CHESTS);
        slotsPerChest = clamp(perChest, 1, ABS_MAX_STORAGE);
        maxOffers = clamp(offers, 1, ABS_MAX_OFFERS);
    }

    public static void encode(FriendlyByteBuf buf) {
        for (Upgrade u : Upgrade.values()) {
            buf.writeBoolean(enabled[u.ordinal()]);
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(items[u.ordinal()]));
        }
        buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(movementStaticItem));
        buf.writeVarInt(maxChests);
        buf.writeVarInt(slotsPerChest);
        buf.writeVarInt(maxOffers);
    }

    public static void decodeAndApply(FriendlyByteBuf buf) {
        int n = Upgrade.values().length;
        boolean[] flags = new boolean[n];
        String[] ids = new String[n];
        for (int i = 0; i < n; i++) {
            flags[i] = buf.readBoolean();
            ids[i] = buf.readResourceLocation().toString();
        }
        String staticId = buf.readResourceLocation().toString();
        int chests = buf.readVarInt(), perChest = buf.readVarInt(), offers = buf.readVarInt();
        apply(flags, List.of(ids), staticId, chests, perChest, offers);
    }

    public static Item parseItem(String id, Item fallback) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id.trim());
        if (rl == null) return fallback;
        Item it = ForgeRegistries.ITEMS.getValue(rl);
        return (it == null || it == Items.AIR) ? fallback : it;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
