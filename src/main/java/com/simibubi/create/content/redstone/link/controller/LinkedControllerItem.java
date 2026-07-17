package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.link.interfaces.ILinkableBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LinkedControllerItem extends Item implements MenuProvider {

	public final DyeColor color;

	public LinkedControllerItem(final Properties properties, DyeColor color) {
		super(properties);
		this.color = color;
	}

	private void putOnCooldown(final Player player) {
		AllItems.LINKED_CONTROLLERS.forEach(linkedControllerItemItemEntry -> player.getCooldowns().addCooldown(linkedControllerItemItemEntry.asItem(), 5));
	}

	@Override
	public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
		final Player player = context.getPlayer();
		if (player == null || !player.mayBuild()) return InteractionResult.PASS;
		final Level world = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final BlockState hitState = world.getBlockState(pos);
		final Block block = hitState.getBlock();

		if (player.isShiftKeyDown()) {
			if (block == AllBlocks.LECTERN_CONTROLLER.get()) {
				if (!world.isClientSide) {
					AllBlocks.LECTERN_CONTROLLER.get().withBlockEntityDo(world, pos, be -> be.swapControllers(stack, player, context.getHand(), hitState));
				}
				return InteractionResult.SUCCESS;
			}
		} else {
			if (world.getBlockEntity(pos) instanceof ILinkableBlockEntity) {
				if (world.isClientSide) CreateClient.LINKED_CONTROLLER_HANDLER.toggleBindMode(pos);
				this.putOnCooldown(player);
				return InteractionResult.SUCCESS;
			}

			if (block == Blocks.LECTERN && !hitState.getValue(LecternBlock.HAS_BOOK)) {
				if (!world.isClientSide) {
					final ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
					AllBlocks.LECTERN_CONTROLLER.get().replaceLectern(hitState, world, pos, lecternStack);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
		final ItemStack heldItem = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			if (!world.isClientSide && player.mayBuild()) {
				player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, heldItem));
			}
			return InteractionResultHolder.success(heldItem);
		}

		if (!player.isShiftKeyDown()) {
			if (world.isClientSide) CreateClient.LINKED_CONTROLLER_HANDLER.toggle();
			this.putOnCooldown(player);
		}

		return InteractionResultHolder.pass(heldItem);
	}

	public static ItemStackHandler getFrequencyItems(final ItemStack stack) {
		if (!AllItems.LINKED_CONTROLLERS.contains(stack.getItem())) {
			throw new IllegalArgumentException("Cannot get frequency items from non-controller: " + stack);
		}
		final ItemStackHandler newInv = new ItemStackHandler(12);
		if (!stack.has(AllDataComponents.LINKED_CONTROLLER_ITEMS)) return newInv;
		ItemHelper.fillItemStackHandler(stack.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY), newInv);
		return newInv;
	}

	@Override
	public Component getDisplayName() {
		return this.getDescription();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
		final ItemStack heldItem = player.getMainHandItem();
		return LinkedControllerMenu.create(id, inventory, heldItem);
	}
}
