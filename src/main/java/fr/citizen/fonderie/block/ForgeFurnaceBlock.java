package fr.citizen.fonderie.block;

import com.mojang.serialization.MapCodec;
import fr.citizen.fonderie.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Four de forgeron multi-blocs. Chaque bloc de la structure porte l'index de sa partie (PART).
 * La partie 0 (en bas, à gauche, en façade) est le « maître » : elle seule a le block entity.
 */
public class ForgeFurnaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Empêche la cascade quand on retire les autres parties de la structure. */
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

    private final ForgeTier tier;
    private final MapCodec<ForgeFurnaceBlock> codec;

    public ForgeFurnaceBlock(ForgeTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.codec = simpleCodec(p -> new ForgeFurnaceBlock(tier, p));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, 0)
                .setValue(LIT, false));
    }

    public ForgeTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // BaseEntityBlock renvoie INVISIBLE par défaut
    }

    // ---------------------------------------------------------------- géométrie

    /** Position d'une partie à partir du maître. FACING = direction vers laquelle regarde la façade. */
    public BlockPos partPos(BlockPos master, Direction facing, int part) {
        Direction right = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        return master.relative(right, tier.dx(part)).above(tier.dy(part)).relative(back, tier.dz(part));
    }

    public BlockPos masterPos(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        Direction right = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        return pos.relative(right, -tier.dx(part)).below(tier.dy(part)).relative(back, -tier.dz(part));
    }

    // ---------------------------------------------------------------- pose

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos master = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);

        for (int part = 1; part < tier.blocks(); part++) {
            BlockPos p = partPos(master, facing, part);
            boolean ok = p.getY() < level.getMaxBuildHeight()
                    && level.getWorldBorder().isWithinBounds(p)
                    && level.getBlockState(p).canBeReplaced(ctx)
                    && level.isUnobstructed(state.setValue(PART, part), p, collision);
            if (!ok) {
                if (level.isClientSide && player != null) {
                    player.displayClientMessage(Component.translatable("message.citizenfonderie.pas_de_place",
                            tier.dimensions()), true);
                }
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (int part = 1; part < tier.blocks(); part++) {
            BlockPos p = partPos(pos, facing, part);
            level.setBlock(p, state.setValue(PART, part).setValue(LIT, false), Block.UPDATE_ALL);
            level.invalidateCapabilities(p);
        }
    }

    // ---------------------------------------------------------------- destruction

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && !REMOVING.get()) {
            REMOVING.set(true);
            try {
                Direction facing = state.getValue(FACING);
                BlockPos master = masterPos(pos, state);
                if (level.getBlockEntity(master) instanceof ForgeFurnaceBlockEntity forge) {
                    forge.dropContents(level, master);
                }
                for (int part = 0; part < tier.blocks(); part++) {
                    BlockPos p = partPos(master, facing, part);
                    if (p.equals(pos)) {
                        continue;
                    }
                    BlockState other = level.getBlockState(p);
                    if (other.is(this) && other.getValue(PART) == part && other.getValue(FACING) == facing) {
                        // flag 35 = UPDATE_ALL | UPDATE_SUPPRESS_DROPS : un seul item lâché (celui du bloc cassé)
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                    }
                    level.invalidateCapabilities(p);
                }
            } finally {
                REMOVING.set(false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---------------------------------------------------------------- interaction

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos master = masterPos(pos, state);
        if (level.getBlockEntity(master) instanceof ForgeFurnaceBlockEntity forge && player instanceof ServerPlayer sp) {
            sp.openMenu(forge, buf -> {
                buf.writeBlockPos(master);
                buf.writeVarInt(tier.ordinal());
            });
        }
        return InteractionResult.CONSUME;
    }

    // ---------------------------------------------------------------- block entity

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new ForgeFurnaceBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != 0) {
            return null;
        }
        return createTickerHelper(type, ModRegistry.FOUR_BE.get(), ForgeFurnaceBlockEntity::serverTick);
    }

    // ---------------------------------------------------------------- rotation

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ---------------------------------------------------------------- effets

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        int part = state.getValue(PART);
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        if (tier.isMouth(part)) {
            if (random.nextDouble() < 0.1) {
                level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
            Direction facing = state.getValue(FACING);
            double along = random.nextDouble() * 0.6 - 0.3;
            double ox = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : along;
            double oz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : along;
            double oy = random.nextDouble() * 6.0 / 16.0;
            level.addParticle(ParticleTypes.SMOKE, x + ox, y + oy, z + oz, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, x + ox, y + oy, z + oz, 0.0, 0.0, 0.0);
        }
        if (part == tier.chimneyPart && random.nextDouble() < 0.5) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    x + random.nextDouble() * 0.3 - 0.15, y + 1.05, z + random.nextDouble() * 0.3 - 0.15,
                    0.0, 0.06, 0.0);
        }
    }
}
