package com.villagershop.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import com.villagershop.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Le bloc "Comptoir de marchand" : héberge le ShopBlockEntity, route les clics,
 * s'oriente vers le joueur à la pose (FACING), et a une hitbox calée sur son
 * modèle (base + pilier + plan de lecture) plutôt qu'un cube plein.
 */
public class ShopBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Forme du lutrin vanilla : base + pilier (symétrique, indépendante de l'orientation).
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(4, 2, 4, 12, 14, 12));

    public ShopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.SHOP_COUNTER.get()) return null;
        return (lvl, pos, st, be) -> ShopBlockEntity.serverTick(lvl, pos, st, (ShopBlockEntity) be);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Comptoir protégé : incassable par quiconque n'est pas proprio/co-proprio (créatif passe outre).
        if (level.getBlockEntity(pos) instanceof ShopBlockEntity be && !be.canBreak(player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShopBlockEntity be && !be.canBreak(player)) {
            player.displayClientMessage(Component.translatable("message.villagershop.protected"), true);
        }
        super.attack(state, level, pos, player);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        // Upgrade obsidienne : résistance façon obsidienne.
        if (level.getBlockEntity(pos) instanceof ShopBlockEntity be && be.hasBlastProtection()) return 1200.0F;
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof ShopBlockEntity be) {
            be.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ShopBlockEntity be) {
                if (be.canAccess(player)) {
                    ((ServerPlayer) player).openMenu(be, buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeUtf(be.getShopName());
                        // Vendeur tiers (pillager…) : débloque le slot Kit de garde.
                        var vendor = be.findShopkeeper();
                        buf.writeBoolean(vendor != null
                                && !(vendor instanceof net.minecraft.world.entity.npc.Villager));
                    });
                } else {
                    player.displayClientMessage(Component.translatable("message.villagershop.not_owner"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ShopBlockEntity be) {
                if (!level.isClientSide) be.revertVillager(level, pos);
                be.dropContents();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
