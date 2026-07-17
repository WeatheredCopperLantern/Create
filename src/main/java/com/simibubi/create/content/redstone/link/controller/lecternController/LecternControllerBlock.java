package com.simibubi.create.content.redstone.link.controller.lecternController;

import java.util.ArrayList;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LecternControllerBlock extends LecternBlock implements IBE<LecternControllerBlockEntity>, SpecialBlockItemRequirement {

	@Override
	public int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(lecternControllerBlockEntity -> lecternControllerBlockEntity.hasUser() ? 15 : 0).orElse(0);
	}

	@Override
	public Class<LecternControllerBlockEntity> getBlockEntityClass() {
		return LecternControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends LecternControllerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.LECTERN_CONTROLLER.get();
	}

	@Override
	public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level, final BlockPos pos, final Player player) {
		return Blocks.LECTERN.getCloneItemStack(state, target, level, pos, player);
	}

	@Override
	public ItemRequirement getRequiredItems(final BlockState state, final @Nullable BlockEntity blockEntity) {
		final ArrayList<ItemStack> requiredItems = new ArrayList<>(2);
		requiredItems.add(new ItemStack(Blocks.LECTERN));
		if (blockEntity instanceof LecternControllerBlockEntity lecternControllerBlockEntity) {
			requiredItems.add(AllItems.LINKED_CONTROLLERS.get(lecternControllerBlockEntity.getColor()).asStack());
		} else {
			requiredItems.add(AllItems.BROWN_LINKED_CONTROLLER.asStack());
		}
		return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItems);
	}

	@Override
	public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		final BlockEntity tmp = IBE.super.newBlockEntity(pos, state);
		assert tmp != null;
		return tmp;
	}

	@Override
	public void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean isMoving) {
		if (newState.getBlock() != this) {
			if (!level.isClientSide) this.withBlockEntityDo(level, pos, be -> be.dropController(state));

			super.onRemove(state, level, pos, newState, isMoving);
		}
	}

	public void replaceLectern(final BlockState lecternState, final Level level, final BlockPos pos, final ItemStack controller) {
		level.setBlockAndUpdate(pos, this.defaultBlockState().setValue(LecternBlock.FACING, lecternState.getValue(LecternBlock.FACING)));
		this.withBlockEntityDo(level, pos, be -> be.setController(controller));
	}

	public void replaceWithLectern(final BlockState state, final Level level, final BlockPos pos) {
		AllSoundEvents.CONTROLLER_TAKE.playOnServer(level, pos);
		level.setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, state.getValue(LecternBlock.FACING)));
	}

	@Override
	protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
		if (!player.isShiftKeyDown() && LecternControllerBlockEntity.playerInRange(player, pos)) {
			if (!level.isClientSide) {
				this.withBlockEntityDo(level, pos, be -> {
					if (be.isUsedBy(player)) {
						be.tryStopUsing(player);
					} else {
						be.tryStartUsing(player);
					}
				});
			}
			return ItemInteractionResult.SUCCESS;
		}

		if (player.isShiftKeyDown()) {
			if (!level.isClientSide) this.replaceWithLectern(state, level, pos);
			return ItemInteractionResult.SUCCESS;
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	public LecternControllerBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LecternBlock.HAS_BOOK, true).setValue(LecternBlock.POWERED, false));
	}
}
