package com.villagershop.client;

import com.villagershop.ShopMod;
import com.villagershop.block.ShopBlockEntity;
import com.villagershop.config.VSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapte le guide Patchouli à la config en vigueur (celle du serveur une fois connecté) :
 * - fonctions de texte {@code $(vsitem:<upgrade>)} et {@code $(vsnum:<valeur>)}, évaluées à l'affichage ;
 * - flags {@code villagershop:*} qui choisissent les pages à afficher (upgrade désactivée = page absente).
 * Le guide ne parle jamais de config : il décrit simplement ce qui est en vigueur.
 */
public final class GuideSync {
    private GuideSync() {}

    private static String lastSignature = null;

    /** À appeler une fois au démarrage du client. */
    public static void registerFunctions() {
        PatchouliAPI.get().registerFunction("vsitem", (arg, style) -> itemName(arg));
        PatchouliAPI.get().registerFunction("vsnum", (arg, style) -> String.valueOf(number(arg)));
    }

    private static String itemName(String key) {
        Item item = null;
        if ("movement_static".equals(key)) item = VSConfig.movementStaticItem();
        else for (VSConfig.Upgrade u : VSConfig.Upgrade.values()) if (u.key.equals(key)) item = VSConfig.item(u);
        return item == null ? key : new ItemStack(item).getHoverName().getString();
    }

    private static int number(String key) {
        return switch (key) {
            case "base_capacity" -> ShopBlockEntity.BASE_CAPACITY;
            case "slots_per_chest" -> VSConfig.slotsPerChest();
            case "max_chests" -> VSConfig.maxChests();
            case "max_capacity" -> VSConfig.capacityFor(VSConfig.maxChests(), ShopBlockEntity.BASE_CAPACITY);
            case "max_offers" -> VSConfig.maxOffers();
            case "max_trade_blocks" -> VSConfig.maxTradeBlocks();
            default -> 0;
        };
    }

    private static Map<String, Boolean> flags() {
        Map<String, Boolean> f = new LinkedHashMap<>();
        for (VSConfig.Upgrade u : VSConfig.Upgrade.values()) {
            f.put("villagershop:" + u.key, VSConfig.isEnabled(u));
            f.put("villagershop:no_" + u.key, !VSConfig.isEnabled(u));
        }
        int chests = VSConfig.maxChests(), blocks = VSConfig.maxTradeBlocks();
        f.put("villagershop:storage_many", chests > 1);
        f.put("villagershop:storage_one", chests == 1);
        f.put("villagershop:trades_many", blocks > 1);
        f.put("villagershop:trades_one", blocks == 1);
        f.put("villagershop:trades_none", blocks == 0);
        return f;
    }

    /** Positionne les flags du guide ; recharge le livre si les pages à afficher ont changé. */
    public static void refresh() {
        StringBuilder sig = new StringBuilder();
        try {
            for (Map.Entry<String, Boolean> e : flags().entrySet()) {
                PatchouliAPI.get().setConfigFlag(e.getKey(), e.getValue());
                sig.append(e.getValue() ? '1' : '0');
            }
            String s = sig.toString();
            boolean changed = lastSignature != null && !s.equals(lastSignature);
            lastSignature = s;
            var level = Minecraft.getInstance().level;
            if (changed && level != null) vazkii.patchouli.common.book.BookRegistry.INSTANCE.reloadContents(level);
        } catch (Throwable t) {
            ShopMod.LOGGER.warn("VillagerShop: could not refresh the guide ({})", t.toString());
        }
    }
}
