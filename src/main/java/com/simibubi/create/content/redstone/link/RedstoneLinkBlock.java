package com.simibubi.create.content.redstone.link;

import java.util.function.Consumer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class RedstoneLinkBlock extends WrenchableDirectionalBlock implements IBE<RedstoneLinkBlockEntity> {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");
	public static final BooleanProperty ROTATED_ANTENNA = BooleanProperty.create("rotated_antenna");

	public RedstoneLinkBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(RedstoneLinkBlock.POWERED, false).setValue(RedstoneLinkBlock.RECEIVER, false).setValue(RedstoneLinkBlock.ROTATED_ANTENNA, false));
	}

	@Override
	public void neighborChanged(@NonNull final BlockState state, final Level level, @NonNull final BlockPos pos, @NonNull final Block block, @NonNull final BlockPos fromPos, final boolean isMoving) {
		if (level.isClientSide) return;
		this.queueUpdate(level, pos);
	}

	private void queueUpdate(final BlockGetter level, final BlockPos pos) {
		this.withLinkableDo(level, pos, RedstoneLinkable::queueUpdate);
	}

	@Override
	public void onRemove(@NonNull final BlockState pState, @NonNull final Level pLevel, @NonNull final BlockPos pPos, @NonNull final BlockState pNewState, final boolean pMovedByPiston) {
		IBE.onRemove(pState, pLevel, pPos, pNewState);
	}

	@Override
	public boolean isSignalSource(final @NonNull BlockState state) {
		return state.getValue(RedstoneLinkBlock.RECEIVER) && state.getValue(RedstoneLinkBlock.POWERED);
	}

	@Override
	public int getDirectSignal(final BlockState blockState, final @NonNull BlockGetter blockAccess, final @NonNull BlockPos pos, final @NonNull Direction side) {
		if (side == blockState.getValue(DirectionalBlock.FACING)) this.getSignal(blockState, blockAccess, pos, side);
		return 0;
	}

	@Override
	public int getSignal(final BlockState state, final @NonNull BlockGetter blockAccess, final @NonNull BlockPos pos, final @NonNull Direction side) {
		//noinspection PointlessBooleanExpression, improves readability
		if (state.getValue(RedstoneLinkBlock.RECEIVER) == false) return 0;
		//return this.getBlockEntityOptional(blockAccess, pos).map(RedstoneLinkBlockEntity::getReceivedSignal).orElse(0);
		return 1;
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RedstoneLinkBlock.POWERED, RedstoneLinkBlock.RECEIVER, RedstoneLinkBlock.ROTATED_ANTENNA);
		super.createBlockStateDefinition(builder);
	}

	@Override
	protected @NonNull InteractionResult useWithoutItem(@NonNull final BlockState state, @NonNull final Level level, @NonNull final BlockPos pos, final Player player, @NonNull final BlockHitResult hitResult) {
		if (player.isShiftKeyDown()) {
			this.toggleMode(state, level, pos);
		} else {
			//TODO: Open inv showing channel Items
		}
		return InteractionResult.SUCCESS;
	}

	public void toggleMode(final BlockState state, final Level level, final BlockPos pos) {
		if (level.isClientSide) return;
		this.onBlockEntityUse(level, pos, be -> {
			final boolean newMode = !state.getValue(RedstoneLinkBlock.RECEIVER);
			be.linkable.setMode(newMode);
			level.setBlock(pos, state.setValue(RedstoneLinkBlock.RECEIVER, newMode).setValue(RedstoneLinkBlock.POWERED, false), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
			return InteractionResult.SUCCESS;
		});
	}

	@Override
	public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
		this.toggleMode(state, context.getLevel(), context.getClickedPos());
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean canConnectRedstone(final @NonNull BlockState state, final @NonNull BlockGetter level, final @NonNull BlockPos pos, @Nullable final Direction direction) {
		return true;
	}

	@Override
	public boolean canSurvive(@NonNull final BlockState state, @NonNull final LevelReader worldIn, @NonNull final BlockPos pos) {
		return RedstoneLinkBlock.canSurviveStatic(state, worldIn, pos);
	}

	public static boolean canSurviveStatic(final BlockState state, final BlockGetter worldIn, final BlockPos pos) {
		final BlockPos neighbourPos = pos.relative(state.getValue(DirectionalBlock.FACING).getOpposite());
		final BlockState neighbour = worldIn.getBlockState(neighbourPos);
		return !neighbour.canBeReplaced();
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		final Direction facing = context.getClickedFace();
		state = state.setValue(DirectionalBlock.FACING, facing);
		if (facing.getAxis().isVertical()) {
			state = state.setValue(RedstoneLinkBlock.ROTATED_ANTENNA, true);
		}

		//TODO: Handle this from the be
		//final CustomData nbt = context.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA);

		//if (nbt != null && !nbt.copyTag().getBoolean("Transmitter")) {
		//	state = state.setValue(RedstoneLinkBlock.RECEIVER, true);
		//}

		return state;
	}

	@Override
	public @NonNull VoxelShape getShape(final BlockState state, @NonNull final BlockGetter worldIn, @NonNull final BlockPos pos, @NonNull final CollisionContext context) {
		return AllShapes.REDSTONE_LINK.get(state.getValue(DirectionalBlock.FACING));
	}

	@Override
	protected boolean isPathfindable(final @NonNull BlockState state, final @NonNull PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<RedstoneLinkBlockEntity> getBlockEntityClass() {
		return RedstoneLinkBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RedstoneLinkBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.REDSTONE_LINK.get();
	}

	@Override
	public <S extends BlockEntity> BlockEntityTicker<S> getTicker(final Level level, final BlockState blockState, final BlockEntityType<S> blockEntityType) {
		return null;
	}

	@Override
	protected void tick(final @NonNull BlockState state, final @NonNull ServerLevel level, final @NonNull BlockPos pos, final @NonNull RandomSource random) {
		if (level.isClientSide) return;
		this.withBlockEntityDo(level, pos, rlbe -> {
			if (!rlbe.isInitialized() && rlbe.hasLevel()) {
				rlbe.initialize();
			}
		});
	}

	@Override
	protected void onPlace(final @NonNull BlockState state, final @NonNull Level level, final @NonNull BlockPos pos, final @NonNull BlockState oldState, final boolean movedByPiston) {
		if (level.isClientSide) return;
		level.scheduleTick(pos, this, 0, TickPriority.EXTREMELY_HIGH);
	}

	private void withLinkableDo(final BlockGetter world, final BlockPos pos, final Consumer<RedstoneLinkLinkable> action) {
		this.getBlockEntityOptional(world, pos).ifPresent(rlbe -> action.accept(rlbe.linkable));
	}
}
