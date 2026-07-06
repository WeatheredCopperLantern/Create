package com.simibubi.create.content.redstone.link;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import com.simibubi.create.*;
import com.simibubi.create.content.redstone.link.dummy.LinkBehaviour;
import com.simibubi.create.content.redstone.link.dummy.RedstoneLinkFrequencySlot;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.RaycastHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@EventBusSubscriber
public class RedstoneLinkBlock extends WrenchableDirectionalBlock implements IBE<RedstoneLinkBlockEntity> {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");
	public static final BooleanProperty ROTATED_ANTENNA = BooleanProperty.create("rotated_antenna");


	public static final Pair<ValueBoxTransform, ValueBoxTransform> SLOTS = ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);

	@SubscribeEvent
	public static void onBlockActivated(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();

		if (player.isShiftKeyDown() || player.isSpectator()) return;

		Level world = event.getLevel();
		BlockPos pos = event.getPos();

		Optional<RedstoneLinkBlockEntity> be = world.getBlockEntity(pos, AllBlockEntityTypes.REDSTONE_LINK.get());
		if (be.isEmpty()) return;

		InteractionHand hand = event.getHand();

		ItemStack heldItem = player.getItemInHand(hand);
		//if (AllItems.LINKED_CONTROLLER.isIn(heldItem))
		//	return;
		if (AllItems.WRENCH.isIn(heldItem)) return;

		BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, player.blockInteractionRange());

		BlockState blockState = world.getBlockState(pos);
		boolean fakePlayer = player instanceof FakePlayer;
		boolean fakePlayerChoice = false;

		if (fakePlayer) {
			Vec3 localHit = ray.getLocation().subtract(Vec3.atLowerCornerOf(pos)).add(Vec3.atLowerCornerOf(ray.getDirection().getNormal()).scale(.25f));
			fakePlayerChoice = localHit.distanceToSqr(SLOTS.getLeft().getLocalOffset(world, pos, blockState)) > localHit.distanceToSqr(SLOTS.getRight().getLocalOffset(world, pos, blockState));
		}

		for (boolean first : Arrays.asList(false, true)) {
			if (fakePlayer && fakePlayerChoice == first || testHit(world, blockState, pos, first, ray.getLocation())) {
				if(!world.isClientSide) be.get().linkable.setFrequency(first, Frequency.of(heldItem));
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
				world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .25f, .1f);
			}
		}
	}

	public static boolean testHit(Level level, BlockState state, BlockPos pos, Boolean first, Vec3 hit) {
		Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(pos));
		return (first ? SLOTS.getLeft() : SLOTS.getRight()).testHit(level, pos, state, localHit);
	}

	private void withLinkableDo(BlockGetter world, BlockPos pos, Consumer<RedstoneLinkLinkable> action) {
		getBlockEntityOptional(world, pos).ifPresent(be -> action.accept(be.linkable));
	}

	public void updateFromLinkable(Level world, BlockPos pos) {
		withLinkableDo(world, pos, linkable -> {
			BlockState state = world.getBlockState(pos);
			if (state.getValue(POWERED) != linkable.signal > 0 || state.getValue(RECEIVER) != linkable.isReceiver()) {
				state = state.setValue(POWERED, linkable.signal > 0);
				state = state.setValue(RECEIVER, linkable.isReceiver());
				world.setBlock(pos, state, Block.UPDATE_ALL);
				updateNeighbours(state, world, pos);
			}
		});
	}

	public void updateFromWorld(Level level, BlockPos pos, BlockState currentState) {
		if (!this.canSurvive(currentState, level, pos)) {
			level.destroyBlock(pos, true);
			return;
		}
		boolean rotateAntenna = currentState.getValue(FACING).getAxis() == Direction.Axis.Y || !level.getBlockState(pos.above()).isAir();

		if (currentState.getValue(RECEIVER)) {
			if (currentState.getValue(ROTATED_ANTENNA) != rotateAntenna) {
				level.setBlock(pos, currentState.cycle(ROTATED_ANTENNA), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE); //This will prevent Observers from getting triggered
			}
		} else {
			int signal = level.getBestNeighborSignal(pos);
			if (signal > 0 != currentState.getValue(POWERED)) {
				currentState = currentState.cycle(POWERED);
				currentState = currentState.setValue(ROTATED_ANTENNA, rotateAntenna);
				level.setBlock(pos, currentState, Block.UPDATE_CLIENTS);
			} else if (currentState.getValue(ROTATED_ANTENNA) != rotateAntenna) {
				level.setBlock(pos, currentState.cycle(ROTATED_ANTENNA), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE); //This will prevent Observers from getting triggered
			}
			withLinkableDo(level, pos, linkable -> linkable.setTransmittedStrength(signal));
		}
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().isClientSide) return InteractionResult.CONSUME;
		withLinkableDo(context.getLevel(), context.getClickedPos(), linkable -> {
			linkable.setMode(linkable.isTransmitter());
		});
		return InteractionResult.CONSUME;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		withLinkableDo(level, pos, RedstoneLinkable::queueUpdate);
	}

	private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
		level.updateNeighborsAt(pos, this);
		level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
	}

	@Override
	public int getDirectSignal(final BlockState blockState, final @NonNull BlockGetter blockAccess, final @NonNull BlockPos pos, final @NonNull Direction side) {
		if (side == blockState.getValue(DirectionalBlock.FACING))
			return this.getSignal(blockState, blockAccess, pos, side);
		return 0;
	}

	@Override
	public int getSignal(final BlockState state, final @NonNull BlockGetter blockAccess, final @NonNull BlockPos pos, final @NonNull Direction side) {
		//noinspection PointlessBooleanExpression, improves readability
		if (state.getValue(RedstoneLinkBlock.RECEIVER) == false || blockAccess instanceof ClientLevel) return 0;
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
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (oldState.getBlock() != this) {
			updateFromWorld(level, pos, state);
		}
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
