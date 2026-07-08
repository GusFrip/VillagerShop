package com.villagershop.event;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.notify.ShopNotifications;
import com.villagershop.registry.ModVillagers;
import com.villagershop.trade.ShopMerchant;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Fabric : intercepte le clic droit sur un villageois "shopkeeper" pour ouvrir
 * l'interface marchand alimentée par le comptoir, la mort du marchand (notif),
 * et la connexion des joueurs (livraison des notifs en attente).
 */
public class VillagerInteractHandler {

    public static void register() {
        UseEntityCallback.EVENT.register(VillagerInteractHandler::onUseEntity);
        ServerLivingEntityEvents.AFTER_DEATH.register(VillagerInteractHandler::onDeath);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ShopNotifications.get(server).deliver(handler.player));
    }

    private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
                                                 Entity target, @Nullable EntityHitResult hit) {
        if (!(target instanceof Villager villager)) return InteractionResult.PASS;
        if (villager.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER) return InteractionResult.PASS;

        // On gère nous-mêmes l'interaction : pas de troc vanilla. SUCCESS annule la suite.
        if (hand != InteractionHand.MAIN_HAND || level.isClientSide) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        ShopBlockEntity be = findShopFor(villager, level);
        if (be == null) {
            // villageois orphelin (comptoir disparu) : on le libère
            villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NONE));
            villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            player.displayClientMessage(Component.translatable("message.villagershop.no_shop"), true);
            return InteractionResult.SUCCESS;
        }

        String name = be.getShopName();
        Component title = (name == null || name.isBlank())
                ? Component.translatable("container.villagershop.trade")
                : Component.literal(name);

        ShopMerchant merchant = new ShopMerchant(be);
        merchant.setTradingPlayer(player);
        // Ouverture via notre menu marchand (playTradeSound neutralisé) pour éviter le
        // crash/dupe au shift-clic. Côté client, Minecraft instancie le marchand vanilla.
        ServerPlayer sp = (ServerPlayer) player;
        java.util.OptionalInt cid = sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, pl) -> new com.villagershop.menu.ShopMerchantMenu(id, inv, merchant), title));
        if (cid.isPresent()) {
            net.minecraft.world.item.trading.MerchantOffers offers = merchant.getOffers();
            if (!offers.isEmpty()) {
                sp.sendMerchantOffers(cid.getAsInt(), offers, 0,
                        merchant.getVillagerXp(), merchant.showProgressBar(), merchant.canRestock());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static ShopBlockEntity findShopFor(Villager villager, Level level) {
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isEmpty()) return null;
        GlobalPos gp = jobSite.get();
        if (!gp.dimension().equals(level.dimension())) return null;
        BlockPos pos = gp.pos();
        if (level.getBlockEntity(pos) instanceof ShopBlockEntity be) return be;
        return null;
    }

    /** Si un villageois Marchand meurt, prévient les propriétaires (via la file de notifs). */
    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Villager villager)) return;
        Level level = villager.level();
        if (level.isClientSide) return;
        if (villager.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER) return;

        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isEmpty() || !jobSite.get().dimension().equals(level.dimension())) return;
        BlockPos pos = jobSite.get().pos();
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity be)) return;

        if (!be.hasNotifier()) return; // paratonnerre requis pour les notifications
        if (level instanceof ServerLevel sl) {
            ShopNotifications.dispatch(sl, be, ShopNotifications.Reason.DEATH);
        }
    }
}
