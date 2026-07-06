package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.ArrayList;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

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

public class LecternControllerBlock extends LecternBlock implements IBE<LecternControllerBlockEntity>, SpecialBlockItemRequirement {

	public LecternControllerBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LecternBlock.HAS_BOOK, true));
	}

	@Override
	public Class<LecternControllerBlockEntity> getBlockEntityClass() {
		return LecternControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends LecternControllerBlockEntity> getBlockEntityType() {
		return null; //AllBlockEntityTypes.LECTERN_CONTROLLER.get();
	}

	@Override
	public BlockEntity newBlockEntity(final BlockPos p_153573_, final BlockState p_153574_) {
		return IBE.super.newBlockEntity(p_153573_, p_153574_);
	}

	@Override
	protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
		if (!player.isShiftKeyDown() && LecternControllerBlockEntity.playerInRange(player, level, pos)) {
			if (!level.isClientSide) this.withBlockEntityDo(level, pos, be -> be.tryStartUsing(player));
			return ItemInteractionResult.SUCCESS;
		}

		if (player.isShiftKeyDown()) {
			if (!level.isClientSide) this.replaceWithLectern(state, level, pos);
			return ItemInteractionResult.SUCCESS;
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public void onRemove(final BlockState state, final Level world, final BlockPos pos, final BlockState newState, final boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			if (!world.isClientSide) this.withBlockEntityDo(world, pos, be -> be.dropController(state));

			super.onRemove(state, world, pos, newState, isMoving);
		}
	}

	@Override
	public int getAnalogOutputSignal(final BlockState state, final Level world, final BlockPos pos) {
		return 15;
	}

	public void replaceLectern(final BlockState lecternState, final Level world, final BlockPos pos, final ItemStack controller) {
		world.setBlockAndUpdate(pos, this.defaultBlockState().setValue(LecternBlock.FACING, lecternState.getValue(LecternBlock.FACING)).setValue(LecternBlock.POWERED, lecternState.getValue(LecternBlock.POWERED)));
		this.withBlockEntityDo(world, pos, be -> be.setController(controller));
	}

	public void replaceWithLectern(final BlockState state, final Level world, final BlockPos pos) {
		AllSoundEvents.CONTROLLER_TAKE.playOnServer(world, pos);
		world.setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, state.getValue(LecternBlock.FACING)).setValue(LecternBlock.POWERED, state.getValue(LecternBlock.POWERED)));
	}

	@Override
	public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level, final BlockPos pos, final Player player) {
		return Blocks.LECTERN.getCloneItemStack(state, target, level, pos, player);
	}

	@Override
	public ItemRequirement getRequiredItems(final BlockState state, final BlockEntity be) {
		final ArrayList<ItemStack> requiredItems = new ArrayList<>();
		requiredItems.add(new ItemStack(Blocks.LECTERN));
		//requiredItems.add(new ItemStack(AllItems.LINKED_CONTROLLER.get()));
		return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItems);
	}
}
