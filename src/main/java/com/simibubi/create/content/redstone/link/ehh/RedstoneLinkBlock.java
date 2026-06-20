package com.simibubi.create.content.redstone.link.ehh;

import com.simibubi.create.AllBlockEntityTypes;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
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

	public RedstoneLinkBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(RECEIVER, false));
		registerDefaultState(defaultBlockState().setValue(ROTATED_ANTENNA, false).setValue(ROTATED_ANTENNA, false));
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
	                            boolean isMoving) {
		if (level.isClientSide) return;
		queueUpdate(level, pos);
	}

	public void queueUpdate(Level level, BlockPos pos) {
		withBlockEntityDo(level, pos, be -> be.behaviour.link.queueUpdate());
	}

	@Override
	public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
		IBE.onRemove(pState, pLevel, pPos, pNewState);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return state.getValue(POWERED) && state.getValue(RECEIVER);
	}

	@Override
	public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
		if (side != blockState.getValue(FACING)) return 0;
		return getSignal(blockState, blockAccess, pos, side);
	}

	@Override
	public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction side) {
		if (!state.getValue(RECEIVER)) return 0;
		return getBlockEntityOptional(blockAccess, pos).map(RedstoneLinkBlockEntity::getReceivedSignal).orElse(0);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(POWERED, RECEIVER, ROTATED_ANTENNA);
		super.createBlockStateDefinition(builder);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player.isShiftKeyDown() && toggleMode(state, level, pos) == InteractionResult.SUCCESS) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public InteractionResult toggleMode(BlockState state, Level level, BlockPos pos) {
		if (level.isClientSide) return InteractionResult.SUCCESS;

		return onBlockEntityUse(level, pos, be -> {
			be.setMode(!state.getValue(RECEIVER));
			boolean blockPowered = level.hasNeighborSignal(pos);
			level.setBlock(pos, state.cycle(RECEIVER).setValue(POWERED, blockPowered), Block.UPDATE_ALL);
			return InteractionResult.SUCCESS;
		});
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (toggleMode(state, context.getLevel(), context.getClickedPos()) == InteractionResult.SUCCESS) {
			return InteractionResult.SUCCESS;
		}
		return super.onWrenched(state, context);
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction _targetedFace) {
		return originalState;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return side != null;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
		return canSurviveStatic(state, worldIn, pos);
	}

	public static boolean canSurviveStatic(BlockState state, LevelReader worldIn, BlockPos pos) {
		BlockPos neighbourPos = pos.relative(state.getValue(FACING).getOpposite());
		BlockState neighbour = worldIn.getBlockState(neighbourPos);
		return !neighbour.canBeReplaced();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		Direction facing = context.getClickedFace();
		state = state.setValue(FACING, facing);
		if(facing.getAxis().isVertical()){
			state = state.setValue(ROTATED_ANTENNA, true);
		}

		CustomData nbt = context.getItemInHand().get(DataComponents.BLOCK_ENTITY_DATA);

		if (nbt != null && !nbt.copyTag().getBoolean("Transmitter")) {
			state = state.setValue(RECEIVER, true);
		}

		return state;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.REDSTONE_LINK.get(state.getValue(FACING));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
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
}
