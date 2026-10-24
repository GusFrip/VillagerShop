package com.villagershop.event;

import com.villagershop.network.ModNetwork;
import com.villagershop.network.SyncConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Pousse la config du serveur à chaque joueur qui se connecte. */
public class ConfigSyncHandler {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModNetwork.sendTo(new SyncConfigPacket(), sp);
        }
    }
}
