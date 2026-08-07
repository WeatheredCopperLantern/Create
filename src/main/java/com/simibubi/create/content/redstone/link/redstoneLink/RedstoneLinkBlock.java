package com.simibubi.create.content.redstone.link.redstoneLink;

import java.util.Arrays;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPersistentObjectTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.redstone.link.linkable.BlockBoundRedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.ILinkableBlockEntity;
import com.simibubi.create.content.redstone.link.linkable.LinkableBehaviour;
import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.persistent.PersistentObjectType;
import com.simibubi.create.foundation.utility.RaycastHelper;

import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber
public class RedstoneLinkBlock extends WrenchableDirectionalBlock implements IBE<RedstoneLinkBlockEntity>, IBBO<RedstoneLinkBlockBoundLinkable> {

	//TODO: move this somewhere more generic
	@SubscribeEvent
	public static void onBlockActivated(final PlayerInteractEvent.RightClickBlock event) {
		final Player player = event.getEntity();

		if (player.isShiftKeyDown() || player.isSpectator()) return;

		final Level world = event.getLevel();
		final BlockPos pos = event.getPos();

		final BlockEntity blockEntity = world.getBlockEntity(pos);

		if (!(blockEntity instanceof final ILinkableBlockEntity linkableBlockEntity)) return;
		final LinkableBehaviour<?> linkableBehaviour = linkableBlockEntity.getLinkableBehaviour();
		final BlockState blockState = world.getBlockState(pos);

		final InteractionHand hand = event.getHand();

		final ItemStack heldItem = player.getItemInHand(hand);
		if (AllItems.WRENCH.isIn(heldItem)) return;

		final BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, player.blockInteractionRange());
		final boolean fakePlayer = player instanceof FakePlayer;
		boolean fakePlayerChoice = false;

		final Couple<ValueBoxTransform> slots = linkableBehaviour.getSlots();

		if (fakePlayer) {
			final Vec3 localHit = ray.getLocation().subtract(Vec3.atLowerCornerOf(pos)).add(Vec3.atLowerCornerOf(ray.getDirection().getNormal()).scale(0.25f));
			fakePlayerChoice = localHit.distanceToSqr(slots.getFirst().getLocalOffset(world, pos, blockState)) > localHit.distanceToSqr(slots.getSecond().getLocalOffset(world, pos, blockState));
		}

		for (final boolean first : Arrays.asList(false, true)) {
			if (fakePlayer && fakePlayerChoice == first || linkableBehaviour.testHit(world, blockState, pos, first, ray.getLocation())) {
				final Frequency newFrequency = Frequency.of(heldItem);
				if ((blockState.getBlock() instanceof final IBBO<?> ibbo && ibbo.getBlockBoundObject(world, pos) instanceof BlockBoundRedstoneLinkable blockBoundRedstoneLinkable) && blockBoundRedstoneLinkable.setFrequency(first, newFrequency)) {
					world.playSound(null, pos, newFrequency == Frequency.EMPTY ? SoundEvents.ITEM_FRAME_REMOVE_ITEM : SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1f);
				}
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
			}
		}
	}

	//region Properties
	public static final Couple<ValueBoxTransform> SLOTS = Couple.createWithContext(RedstoneLinkFrequencySlot::new);

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");
	public static final BooleanProperty ROTATED_ANTENNA = BooleanProperty.create("rotated_antenna");
	//endregion

	//region Logic

	@Override
	protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
		if (AllItems.WRENCH.isIn(stack) || player instanceof FakePlayer || player.isShiftKeyDown()) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}

		if (!level.isClientSide) {
			this.withBlockEntityDo(level, pos, blockEntity -> player.openMenu(blockEntity.getLinkableBehaviour(), blockEntity::sendToMenu));
		}

		return ItemInteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighborBlock, final BlockPos neighborPos, final boolean movedByPiston) {
		this.withBlockBoundObjectDo(level, pos, BlockBoundRedstoneLinkable::updateFromWorld);
	}

	@Override
	protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
		if (level.isClientSide || oldState.getBlock() == this) return;
		this.withBlockBoundObjectDo(level, pos, BlockBoundRedstoneLinkable::updateFromWorld);
	}

	public void updateFromWorld(final Level level, final BlockPos pos, BlockState currentState) {
		if (!this.canSurvive(currentState, level, pos)) {
			level.destroyBlock(pos, true);
			return;
		}
		final boolean rotateAntenna = currentState.getValue(FACING).getAxis() == Direction.Axis.Y || !level.getBlockState(pos.above()).isAir();

		if (!currentState.getValue(RedstoneLinkBlock.RECEIVER)) {
			final int signal = level.getBestNeighborSignal(pos);
			if (signal > 0 != currentState.getValue(RedstoneLinkBlock.POWERED)) {
				this.withBlockBoundObjectDo(level, pos, bbo -> bbo.setSignal(signal));
			}
		}
		if (currentState.getValue(RedstoneLinkBlock.ROTATED_ANTENNA) != rotateAntenna) {
			level.setBlock(pos, currentState.cycle(RedstoneLinkBlock.ROTATED_ANTENNA), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE); //This will prevent Observers from getting triggered
		}
	}

	@Override
	public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
		this.withBlockBoundObjectDo(context.getLevel(), context.getClickedPos(), bbo -> bbo.setMode(bbo.isTransmitter()));
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean canSurvive(final BlockState state, final LevelReader worldIn, final BlockPos pos) {
		final BlockPos neighbourPos = pos.relative(state.getValue(FACING).getOpposite());
		final BlockState neighbour = worldIn.getBlockState(neighbourPos);
		return !neighbour.canBeReplaced();
	}

	@Override
	public int getDirectSignal(final BlockState blockState, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
		if (side == blockState.getValue(DirectionalBlock.FACING)) return this.getSignal(blockState, blockAccess, pos, side);
		return 0;
	}

	@Override
	public int getSignal(final BlockState state, final BlockGetter blockAccess, final BlockPos pos, final Direction side) {
		//noinspection PointlessBooleanExpression, improves readability
		if (state.getValue(RedstoneLinkBlock.RECEIVER) == false || blockAccess instanceof ClientLevel) return 0;
		return this.getBlockBoundObjectOptional(blockAccess, pos).map(BlockBoundRedstoneLinkable::getSignal).orElse(0);
	}

	@Override
	public boolean isSignalSource(final BlockState state) {
		return state.getValue(RECEIVER) && state.getValue(POWERED);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		final Direction facing = context.getClickedFace();
		state = state.setValue(FACING, facing);
		if (facing.getAxis().isVertical()) {
			state = state.setValue(ROTATED_ANTENNA, true);
		}
		return state;
	}
	//endregion

	//region IBE
	@Override
	public Class<RedstoneLinkBlockEntity> getBlockEntityClass() {
		return RedstoneLinkBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RedstoneLinkBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.REDSTONE_LINK.get();
	}
	//endregion

	//region IBBO
	@Override
	public PersistentObjectType<RedstoneLinkBlockBoundLinkable> getPersistentBlockBoundObjectType() {
		return AllPersistentObjectTypes.REDSTONE_LINK.get();
	}
	//endregion

	//region Non Logic Block Stuff
	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED, RECEIVER, ROTATED_ANTENNA);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext context) {
		return AllShapes.REDSTONE_LINK.get(state.getValue(FACING));
	}

	@Override
	public void onRemove(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pNewState, final boolean pMovedByPiston) {
		IBE.onRemove(pState, pLevel, pPos, pNewState);
	}

	@Override
	protected boolean isPathfindable(final BlockState state, final PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public boolean canConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos, @Nullable final Direction direction) {
		return true;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
		return null;
	}

	public RedstoneLinkBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false).setValue(RECEIVER, false).setValue(ROTATED_ANTENNA, false));
	}
	//endregion
}
