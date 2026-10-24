package com.villagershop.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Fichier de configuration {@code config/villagershop-common.toml}.
 *
 * Lu par le serveur (ou le jeu solo) puis transmis aux clients : voir {@link VSConfig}.
 */
public final class ShopConfigSpec {
    private ShopConfigSpec() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue[] ENABLED = new ModConfigSpec.BooleanValue[VSConfig.Upgrade.values().length];
    private static final ModConfigSpec.ConfigValue<String>[] ITEMS = newArray(VSConfig.Upgrade.values().length);
    private static ModConfigSpec.ConfigValue<String> MOVEMENT_STATIC_ITEM;
    private static final ModConfigSpec.IntValue MAX_CHESTS;
    private static final ModConfigSpec.IntValue SLOTS_PER_CHEST;
    private static final ModConfigSpec.IntValue MAX_OFFERS;

    @SuppressWarnings("unchecked")
    private static ModConfigSpec.ConfigValue<String>[] newArray(int n) {
        return new ModConfigSpec.ConfigValue[n];
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("VillagerShop configuration.",
                  "Values are read by the server (or the single-player game) and sent to every client on login,",
                  "so only the server-side file matters in multiplayer.").push("upgrades");
        for (VSConfig.Upgrade u : VSConfig.Upgrade.values()) {
            b.push(u.key);
            ENABLED[u.ordinal()] = b.comment("Enable this upgrade. When disabled its slot is hidden and the feature is off.")
                    .define("enabled", true);
            ITEMS[u.ordinal()] = b.comment("Item accepted by this upgrade slot (registry id, e.g. \"minecraft:nether_star\").")
                    .define("item", BuiltInRegistries.ITEM.getKey(u.defaultItem).toString());
            if (u == VSConfig.Upgrade.MOVEMENT) {
                MOVEMENT_STATIC_ITEM = b.comment("Second item for the Movement upgrade: the seller stands still in front of the counter.",
                                "\"item\" above is the tether mode (stays within 3 blocks).")
                        .define("staticItem", "minecraft:amethyst_shard");
            }
            b.pop();
        }
        b.pop();

        b.comment("Shop storage (Storage upgrade).").push("storage");
        MAX_CHESTS = b.comment("Maximum number of chests in the Storage upgrade slot (1-16).")
                .defineInRange("maxChests", 8, 1, VSConfig.ABS_MAX_CHESTS);
        SLOTS_PER_CHEST = b.comment("Storage slots granted per chest. Total capacity is capped at 432 slots (16 x 27).")
                .defineInRange("slotsPerChest", 27, 1, VSConfig.ABS_MAX_STORAGE);
        b.pop();

        b.comment("Trade offers (Trades upgrade).").push("trades");
        MAX_OFFERS = b.comment("Maximum number of offers per shop, including the free one (1-8). Each emerald block unlocks one more.")
                .defineInRange("maxOffers", 8, 1, VSConfig.ABS_MAX_OFFERS);
        b.pop();

        SPEC = b.build();
    }

    /** Recopie le fichier dans {@link VSConfig} (à appeler au chargement / rechargement de la config). */
    public static void applyToRuntime() {
        int n = VSConfig.Upgrade.values().length;
        boolean[] flags = new boolean[n];
        List<String> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            flags[i] = ENABLED[i].get();
            ids.add(ITEMS[i].get());
        }
        VSConfig.apply(flags, ids, MOVEMENT_STATIC_ITEM.get(), MAX_CHESTS.get(), SLOTS_PER_CHEST.get(), MAX_OFFERS.get());
    }
}
