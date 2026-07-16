package com.simibubi.create.content.redstone.link.controller.packet;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import com.simibubi.create.content.redstone.link.controller.lecternController.LecternControllerBlockEntity;
import net.createmod.catnip.net.base.ServerboundPacketPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class LinkedControllerPacketBase implements ServerboundPacketPayload {

	public @Nullable ItemStackHandler getFrequencyItems(final @NonNull ServerPlayer player, final @Nullable BlockPos pos) {
		if (pos == null) {
			if (AllItems.LINKED_CONTROLLERS.contains(player.getMainHandItem())) {
				return LinkedControllerItem.getFrequencyItems(player.getMainHandItem());
			} else if (AllItems.LINKED_CONTROLLERS.contains(player.getOffhandItem())) {
				return LinkedControllerItem.getFrequencyItems(player.getOffhandItem());
			}
		} else {
			final BlockEntity be = player.level().getBlockEntity(pos);
			if (be instanceof final LecternControllerBlockEntity lecternControllerBlockEntity) {
				return lecternControllerBlockEntity.getFrequencyItems();
			}
		}
		return null;
	}

	public @Nullable ItemStack getController(final @NonNull ServerPlayer player) {
		if (AllItems.LINKED_CONTROLLERS.contains(player.getMainHandItem())) {
			return player.getMainHandItem();
		} else if (AllItems.LINKED_CONTROLLERS.contains(player.getOffhandItem())) {
			return player.getOffhandItem();
		}
		return null;
	}
}
