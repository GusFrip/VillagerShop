package com.villagershop.event;

import com.villagershop.network.ModNetwork;
import com.villagershop.network.SyncConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Pousse la config du serveur à chaque joueur qui se connecte. */
public class ConfigSyncHandler {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModNetwork.sendTo(SyncConfigPacket.capture(), sp);
        }
    }
}
