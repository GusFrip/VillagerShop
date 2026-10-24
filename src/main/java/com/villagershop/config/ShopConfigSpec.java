package com.villagershop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.villagershop.ShopMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fichier de configuration {@code config/villagershop-common.json} (Fabric : pas de TOML natif).
 *
 * Lu au lancement et au démarrage de chaque serveur (solo compris), puis transmis aux clients :
 * voir {@link VSConfig}. Les clés manquantes sont complétées avec les valeurs par défaut.
 */
public final class ShopConfigSpec {
    private ShopConfigSpec() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("villagershop-common.json");
    }

    /** Lit (ou crée) le fichier et recopie les valeurs dans {@link VSConfig}. */
    public static void applyToRuntime() {
        JsonObject root = read();
        boolean changed = false;

        JsonObject upgrades = obj(root, "upgrades");
        int n = VSConfig.Upgrade.values().length;
        boolean[] flags = new boolean[n];
        List<String> ids = new ArrayList<>(n);
        String staticId = "minecraft:amethyst_shard";
        for (VSConfig.Upgrade u : VSConfig.Upgrade.values()) {
            JsonObject o = obj(upgrades, u.key);
            if (!o.has("enabled")) { o.addProperty("enabled", true); changed = true; }
            if (!o.has("item")) { o.addProperty("item", BuiltInRegistries.ITEM.getKey(u.defaultItem).toString()); changed = true; }
            flags[u.ordinal()] = o.get("enabled").getAsBoolean();
            ids.add(o.get("item").getAsString());
            if (u == VSConfig.Upgrade.MOVEMENT) {
                if (!o.has("staticItem")) { o.addProperty("staticItem", staticId); changed = true; }
                staticId = o.get("staticItem").getAsString();
            }
        }

        JsonObject storage = obj(root, "storage");
        if (!storage.has("maxChests")) { storage.addProperty("maxChests", 8); changed = true; }
        if (!storage.has("slotsPerChest")) { storage.addProperty("slotsPerChest", 27); changed = true; }
        JsonObject trades = obj(root, "trades");
        if (!trades.has("maxOffers")) { trades.addProperty("maxOffers", 8); changed = true; }
        if (!root.has("_comment")) {
            root.addProperty("_comment", "VillagerShop: server-side values sent to every client on login. "
                    + "maxChests 1-16, slotsPerChest (total capped at 432), maxOffers 1-8 (free offer included). "
                    + "A disabled upgrade hides its slot and turns the feature off; items are registry ids.");
            changed = true;
        }

        VSConfig.apply(flags, ids, staticId,
                clamp(storage.get("maxChests").getAsInt(), 1, VSConfig.ABS_MAX_CHESTS),
                clamp(storage.get("slotsPerChest").getAsInt(), 1, VSConfig.ABS_MAX_STORAGE),
                clamp(trades.get("maxOffers").getAsInt(), 1, VSConfig.ABS_MAX_OFFERS));

        if (changed) write(root);
    }

    private static JsonObject read() {
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                JsonElement e = GSON.fromJson(r, JsonElement.class);
                if (e != null && e.isJsonObject()) return e.getAsJsonObject();
            } catch (Exception ex) {
                ShopMod.LOGGER.warn("VillagerShop: invalid config {}, using defaults ({})", f, ex.getMessage());
                return new JsonObject(); // fichier illisible : défauts, sans l'écraser
            }
        }
        return new JsonObject();
    }

    private static void write(JsonObject root) {
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { GSON.toJson(root, w); }
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ShopMod.LOGGER.warn("VillagerShop: could not write config {} ({})", f, ex.getMessage());
        }
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) parent.add(key, new JsonObject());
        return parent.getAsJsonObject(key);
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
