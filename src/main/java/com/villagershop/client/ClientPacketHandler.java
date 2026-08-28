package com.villagershop.client;

import com.villagershop.data.ShopOffer;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Logique réseau exécutée uniquement côté client (chargée via DistExecutor). */
public class ClientPacketHandler {
    public static void handleSyncAllowed(List<String> names) {
        if (Minecraft.getInstance().screen instanceof ShopConfigScreen screen) {
            screen.setAllowedNames(names);
        }
    }

    public static void handleSyncOffers(List<ShopOffer> offers, int selected) {
        if (Minecraft.getInstance().screen instanceof ShopConfigScreen screen) {
            screen.setOffers(offers, selected);
        }
    }

    /** Ouvre l'écran Registre des ventes avec l'instantané reçu du serveur. */
    public static void handleSyncStats(List<com.villagershop.stats.ShopStatSnapshot> shops) {
        Minecraft.getInstance().setScreen(new LedgerScreen(shops));
    }
}
