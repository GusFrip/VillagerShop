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
        if (!(event.getTarget() instanceof net.minecraft.world.entity.Mob mob)) return;
        // Vendeur = villageois shopkeeper (mécanique historique) ou n'importe
        // quel Mob portant un tag de liaison (vendeurs tiers, ex. pillager
        // marchand de PillagerControl).
        boolean isVillagerShopkeeper = mob instanceof Villager v
                && v.getVillagerData().getProfession() == ModVillagers.SHOPKEEPER.get();
        boolean isTaggedVendor = vendorTagPos(mob) != null;
        if (!isVillagerShopkeeper && !isTaggedVendor) return;

        // On gère nous-mêmes l'interaction : pas de troc vanilla par défaut.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        Player player = event.getEntity();
        Level level = event.getLevel();
        if (level.isClientSide) return;

        ShopBlockEntity be = findShopFor(mob, level);
        if (be == null) {
            if (mob instanceof Villager villager) {
                // villageois orphelin (comptoir disparu) : on le libère
                villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NONE));
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            }
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
    private ShopBlockEntity findShopFor(net.minecraft.world.entity.Mob mob, Level level) {
        BlockPos pos = null;
        if (mob instanceof Villager villager) {
            Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (jobSite.isEmpty()) return null;
            GlobalPos gp = jobSite.get();
            if (!gp.dimension().equals(level.dimension())) return null;
            pos = gp.pos();
        } else {
            pos = vendorTagPos(mob);
        }
        if (pos != null && level.getBlockEntity(pos) instanceof ShopBlockEntity be) return be;
        return null;
    }

    /** Position du comptoir encodée dans le tag de liaison d'un vendeur tiers, ou null. */
    @Nullable
    private static BlockPos vendorTagPos(net.minecraft.world.entity.Mob mob) {
        for (String tag : mob.getTags()) {
            if (tag.startsWith("villagershop_vendor_")) {
                try {
                    return BlockPos.of(Long.parseLong(tag.substring("villagershop_vendor_".length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /** Si un vendeur (villageois ou tiers) meurt, prévient les propriétaires connectés. */
    @SubscribeEvent
    public void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
        Level level = mob.level();
        if (level.isClientSide) return;
        if (mob instanceof Villager villager
                && villager.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER.get()) return;

        ShopBlockEntity be = findShopFor(mob, level);
        if (be == null) return;

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
