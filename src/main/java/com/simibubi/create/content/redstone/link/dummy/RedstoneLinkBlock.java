package com.simibubi.create.content.redstone.link.dummy;

import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneLinkBlock extends WrenchableDirectionalBlock implements IBE<RedstoneLinkBlockEntity> {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");
	public static final BooleanProperty ROTATED_ANTENNA = BooleanProperty.create("rotated_antenna");

	public RedstoneLinkBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(RedstoneLinkBlock.POWERED, false).setValue(RedstoneLinkBlock.RECEIVER, false));
		this.registerDefaultState(this.defaultBlockState().setValue(RedstoneLinkBlock.ROTATED_ANTENNA, false).setValue(RedstoneLinkBlock.ROTATED_ANTENNA, false));
	}

	@Override
	public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos fromPos, final boolean isMoving) {
		if (level.isClientSide) return;
		this.queueUpdate(level, pos);
	}

	public void queueUpdate(final Level level, final BlockPos pos) {
		this.withBlockEntityDo(level, pos, be -> be.behaviour.link.queueUpdate());
	}

	@Override
	public void onRemove(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pNewState, final boolean pMovedByPiston) {
		IBE.onRemove(pState, pLevel, pPos, pNewState);
	}

	@Override
	public boolean isSignalSource(final BlockState state) {
		return state.getValue(RedstoneLinkBlock.POWERED) && state.getValue(RedstoneLinkBlock.RECEIVER);
	}

	@Override
	public int getDirectSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
		if (side != blockState.getValue(DirectionalBlock.FACING)) return 0;
		return this.getSignal(blockState, blockAccess, pos, side);
	}

	@Override
	public int getSignal(final BlockState state, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
		if (!state.getValue(RedstoneLinkBlock.RECEIVER)) return 0;
		return this.getBlockEntityOptional(blockAccess, pos).map(RedstoneLinkBlockEntity::getReceivedSignal).orElse(0);
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RedstoneLinkBlock.POWERED, RedstoneLinkBlock.RECEIVER, RedstoneLinkBlock.ROTATED_ANTENNA);
		super.createBlockStateDefinition(builder);
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (player.isShiftKeyDown() && this.toggleMode(state, level, pos) == InteractionResult.SUCCESS) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public InteractionResult toggleMode(final BlockState state, final Level level, final BlockPos pos) {
		if (level.isClientSide) return InteractionResult.SUCCESS;

		return this.onBlockEntityUse(level, pos, be -> {
			be.setMode(!state.getValue(RedstoneLinkBlock.RECEIVER));
			final boolean blockPowered = level.hasNeighborSignal(pos);
			level.setBlock(pos, state.cycle(RedstoneLinkBlock.RECEIVER).setValue(RedstoneLinkBlock.POWERED, blockPowered), Block.UPDATE_ALL);
			return InteractionResult.SUCCESS;
		});
	}

	@Override
	public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
		if (this.toggleMode(state, context.getLevel(), context.getClickedPos()) == InteractionResult.SUCCESS) {
			return InteractionResult.SUCCESS;
		}
		return super.onWrenched(state, context);
	}

	@Override
	public BlockState getRotatedBlockState(final BlockState originalState, final Direction _targetedFace) {
		return originalState;
	}

	@Override
	public boolean canConnectRedstone(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
		return side != null;
	}

	@Override
	public boolean canSurvive(final BlockState state, final LevelReader worldIn, final BlockPos pos) {
		return RedstoneLinkBlock.canSurviveStatic(state, worldIn, pos);
	}

	public static boolean canSurviveStatic(final BlockState state, final LevelReader worldIn, final BlockPos pos) {
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

		final CustomData nbt = context.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA);

		if (nbt != null && !nbt.copyTag().getBoolean("Transmitter")) {
			state = state.setValue(RedstoneLinkBlock.RECEIVER, true);
		}

		return state;
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
		return AllShapes.REDSTONE_LINK.get(state.getValue(DirectionalBlock.FACING));
	}

	@Override
	protected boolean isPathfindable(final BlockState state, final PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<RedstoneLinkBlockEntity> getBlockEntityClass() {
		return RedstoneLinkBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RedstoneLinkBlockEntity> getBlockEntityType() {
		return null; //AllBlockEntityTypes.REDSTONE_LINK.get();
	}
}
