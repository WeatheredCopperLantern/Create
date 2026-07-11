package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.Frequency;
import com.simibubi.create.foundation.item.ItemHelper;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class LinkedControllerItem extends Item implements MenuProvider {

	public LinkedControllerItem(final Properties properties) {
		super(properties);
	}

	@Override
	public @NonNull InteractionResult onItemUseFirst(@NonNull ItemStack stack, @NonNull UseOnContext context) {
		final Player player = context.getPlayer();
		if (player == null || !player.mayBuild()) return InteractionResult.PASS;
		final Level world = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final BlockState hitState = world.getBlockState(pos);
		final Block block = hitState.getBlock();

		if (player.isShiftKeyDown()) {
			if (block == AllBlocks.LECTERN_CONTROLLER.get()) {
				if (!world.isClientSide)
					AllBlocks.LECTERN_CONTROLLER.get().withBlockEntityDo(world, pos, be -> be.swapControllers(stack, player, context.getHand(), hitState));
				return InteractionResult.SUCCESS;
			}
		} else {
			if (block == AllBlocks.REDSTONE_LINK.get()) {
				if (world.isClientSide) LinkedControllerClientHandler.toggleBindMode(pos);
				player.getCooldowns().addCooldown(this, 2);
				return InteractionResult.SUCCESS;
			}

			if (block == Blocks.LECTERN && !hitState.getValue(LecternBlock.HAS_BOOK)) {
				if (!world.isClientSide) {
					ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
					AllBlocks.LECTERN_CONTROLLER.get().replaceLectern(hitState, world, pos, lecternStack);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public @NonNull InteractionResultHolder<ItemStack> use(@NonNull Level world, @NonNull Player player, @NonNull InteractionHand hand) {
		final ItemStack heldItem = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			if (!world.isClientSide && player.mayBuild()) {
				player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, heldItem));
			}
			return InteractionResultHolder.success(heldItem);
		}

		if (!player.isShiftKeyDown()) {
			if (world.isClientSide) LinkedControllerClientHandler.toggle();
			player.getCooldowns().addCooldown(this, 2);
		}

		return InteractionResultHolder.pass(heldItem);
	}

	public static ItemStackHandler getFrequencyItems(final ItemStack stack) {
		final ItemStackHandler newInv = new ItemStackHandler(12);
		if (stack.getItem() != AllItems.LINKED_CONTROLLER.get())
			throw new IllegalArgumentException("Cannot get frequency items from non-controller: " + stack);
		if (!stack.has(AllDataComponents.LINKED_CONTROLLER_ITEMS)) return newInv;
		ItemHelper.fillItemStackHandler(stack.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY), newInv);
		return newInv;
	}

	public static Couple<Frequency> toFrequency(final ItemStack controller, final int slot) {
		final ItemStackHandler frequencyItems = com.simibubi.create.content.redstone.link.dummy.controller.LinkedControllerItem.getFrequencyItems(controller);
		return Couple.create(Frequency.of(frequencyItems.getStackInSlot(slot * 2)), Frequency.of(frequencyItems.getStackInSlot(slot * 2 + 1)));
	}

	@Override
	public @NonNull Component getDisplayName() {
		return this.getDescription();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, @NonNull Inventory inventory, @NonNull Player player) {
		final ItemStack heldItem = player.getMainHandItem();
		return LinkedControllerMenu.create(id, inventory, heldItem);
	}
}
