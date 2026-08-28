package com.villagershop.block;

import com.villagershop.network.ModNetwork;
import com.villagershop.network.SyncStatsPacket;
import com.villagershop.stats.ShopSalesStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
 * Le "Registre des ventes" : pupitre de comptable consultable par tous.
 * Au clic droit, le serveur construit un instantané des statistiques des
 * boutiques du joueur (celles où il est proprio/co-proprio ET équipées de
 * l'upgrade Communication) et l'envoie au client, qui ouvre l'écran Registre.
 * Pas de BlockEntity : les données vivent dans ShopSalesStats (SavedData).
 */
public class LedgerBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Forme du lutrin vanilla : base + pilier (comme le comptoir).
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(4, 2, 4, 12, 15, 12));

    public LedgerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            ShopSalesStats stats = ShopSalesStats.get(sl.getServer());
            long mcToday = sl.getDayTime() / 24000L;
            long realToday = System.currentTimeMillis() / 86_400_000L;
            stats.pruneAll(mcToday, realToday);
            ModNetwork.sendTo(SyncStatsPacket.of(stats.shopsFor(sp.getUUID())), sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
