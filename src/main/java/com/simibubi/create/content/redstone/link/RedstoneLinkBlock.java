package com.simibubi.create.content.redstone.link;

import java.util.Optional;
import java.util.function.Consumer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.Create;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

	private void withLinkableDo(BlockGetter world, BlockPos pos, Consumer<RedstoneLinkLinkable> action) {
		getBlockEntityOptional(world, pos).ifPresent(be -> action.accept(be.linkable));
	}

	public void updateFromLinkable(Level world, BlockPos pos){
		withLinkableDo(world, pos, linkable -> {
			BlockState state = world.getBlockState(pos);
			if(state.getValue(POWERED) != linkable.signal > 0 || state.getValue(RECEIVER) != linkable.isReceiver()){
				state = state.setValue(POWERED, linkable.signal > 0);
				state = state.setValue(RECEIVER, linkable.isReceiver());
				world.setBlock(pos, state, Block.UPDATE_CLIENTS);
			}
		});
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if(context.getLevel().isClientSide) return InteractionResult.CONSUME;
		withLinkableDo(context.getLevel(), context.getClickedPos(), linkable -> {
			linkable.setMode(linkable.isTransmitter());
		});
		return InteractionResult.CONSUME;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		Create.LOGGER.warn("heh");
	}

	//@Override
	//protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
	//	if (oldState.getBlock() == this && (oldState.getValue(POWERED) != state.getValue(POWERED) || oldState.getValue(RECEIVER) != state.getValue(RECEIVER))) {
	//		update(level, pos, state);
	//	}
	//}
//
	//private void update(LevelAccessor level, BlockPos pos, BlockState state) {
	//	Direction attachedFace = state.getValue(FACING).getOpposite();
	//	BlockPos attachedPos = pos.relative(attachedFace);
	//	level.blockUpdated(pos, this);
	//	level.blockUpdated(attachedPos, level.getBlockState(attachedPos).getBlock());
	//}

	@Override
	public int getDirectSignal(final BlockState blockState, final @NonNull BlockGetter blockAccess, final @NonNull BlockPos pos, final @NonNull Direction side) {
		if (side == blockState.getValue(DirectionalBlock.FACING)) this.getSignal(blockState, blockAccess, pos, side);
		return 0;
	}

	@Override
	public int getSignal(final BlockState state, final @NonNull BlockGetter blockAccess, final @NonNull BlockPos pos, final @NonNull Direction side) {
		//noinspection PointlessBooleanExpression, improves readability
		if (state.getValue(RedstoneLinkBlock.RECEIVER) == false) return 0;
		return this.getBlockEntityOptional(blockAccess, pos).map(redstoneLinkBlockEntity -> redstoneLinkBlockEntity.linkable.signal).orElse(0);
	}

	@Override
	public boolean isSignalSource(final @NonNull BlockState state) {
		return state.getValue(RedstoneLinkBlock.RECEIVER) && state.getValue(RedstoneLinkBlock.POWERED);
	}

	@Override
	public @NonNull VoxelShape getShape(final BlockState state, @NonNull final BlockGetter worldIn, @NonNull final BlockPos pos, @NonNull final CollisionContext context) {
		return AllShapes.REDSTONE_LINK.get(state.getValue(DirectionalBlock.FACING));
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		final Direction facing = context.getClickedFace();
		state = state.setValue(DirectionalBlock.FACING, facing);
		if (facing.getAxis().isVertical()) {
			state = state.setValue(RedstoneLinkBlock.ROTATED_ANTENNA, true);
		}
		return state;
	}

	@Override
	protected boolean isPathfindable(final @NonNull BlockState state, final @NonNull PathComputationType pathComputationType) {
		return false;
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
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RedstoneLinkBlock.POWERED, RedstoneLinkBlock.RECEIVER, RedstoneLinkBlock.ROTATED_ANTENNA);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public void onRemove(@NonNull final BlockState pState, @NonNull final Level pLevel, @NonNull final BlockPos pPos, @NonNull final BlockState pNewState, final boolean pMovedByPiston) {
		IBE.onRemove(pState, pLevel, pPos, pNewState);
	}

	@Override
	public <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<S> p_153214_) {
		return null;
	}

	@Override
	public Class<RedstoneLinkBlockEntity> getBlockEntityClass() {
		return RedstoneLinkBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RedstoneLinkBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.REDSTONE_LINK.get();
	}

	public RedstoneLinkBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(RedstoneLinkBlock.POWERED, false).setValue(RedstoneLinkBlock.RECEIVER, false).setValue(RedstoneLinkBlock.ROTATED_ANTENNA, false));
	}
}
