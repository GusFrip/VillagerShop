package com.villagershop.event;

import com.villagershop.block.ShopBlockEntity;
import com.villagershop.registry.ModVillagers;
import com.villagershop.trade.ShopMerchant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerLevel;
import com.villagershop.notify.ShopNotifications;
import net.neoforged.bus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Intercepte le clic droit sur un villageois "shopkeeper" (celui qui occupe un
 * comptoir comme poste de travail) pour ouvrir l'interface de marchand native de
 * Minecraft, alimentée par les offres et le stock du comptoir. Le titre affiché est
 * le nom du shop saisi par le propriétaire ; pas de progression de niveau.
 */
public class VillagerInteractHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (villager.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER.get()) return;

        // On gère nous-mêmes l'interaction : pas de troc vanilla par défaut.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        Player player = event.getEntity();
        Level level = event.getLevel();
        if (level.isClientSide) return;

        ShopBlockEntity be = findShopFor(villager, level);
        if (be == null) {
            // villageois orphelin (comptoir disparu) : on le libère
            villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NONE));
            villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            player.displayClientMessage(Component.translatable("message.villagershop.no_shop"), true);
            return;
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
    }

    @Nullable
    private ShopBlockEntity findShopFor(Villager villager, Level level) {
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isEmpty()) return null;
        GlobalPos gp = jobSite.get();
        if (!gp.dimension().equals(level.dimension())) return null;
        BlockPos pos = gp.pos();
        if (level.getBlockEntity(pos) instanceof ShopBlockEntity be) return be;
        return null;
    }

    /** Si un villageois Marchand meurt, prévient les propriétaires connectés. */
    @SubscribeEvent
    public void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        Level level = villager.level();
        if (level.isClientSide) return;
        if (villager.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER.get()) return;

        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isEmpty() || !jobSite.get().dimension().equals(level.dimension())) return;
        BlockPos pos = jobSite.get().pos();
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity be)) return;

        if (!be.hasNotifier()) return; // paratonnerre requis pour les notifications
        if (level instanceof ServerLevel sl) {
            ShopNotifications.dispatch(sl, be, ShopNotifications.Reason.DEATH);
        }
    }

    /** À la connexion d'un joueur : délivre ses notifications de boutique en attente. */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            ShopNotifications.get(sp.getServer()).deliver(sp);
        }
    }
}
