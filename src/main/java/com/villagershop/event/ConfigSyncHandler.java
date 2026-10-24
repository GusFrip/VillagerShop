package com.villagershop.event;

import com.villagershop.network.ModNetwork;
import com.villagershop.network.SyncConfigPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Pousse la config du serveur à chaque joueur qui se connecte. */
public final class ConfigSyncHandler {
    private ConfigSyncHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ModNetwork.sendTo(SyncConfigPacket.capture(), handler.player));
    }
}
