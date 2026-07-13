package com.simibubi.create.content.redstone.link.controller.packet;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LecternControllerBlockEntity;
import net.createmod.catnip.net.base.ServerboundPacketPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class LinkedControllerPacketBase implements ServerboundPacketPayload {

	public @Nullable ItemStack getController(final @NonNull ServerPlayer player, final @Nullable BlockPos pos) {
		ItemStack controller = null;
		if (pos == null) {
			controller = player.getMainHandItem();
			if (!AllItems.LINKED_CONTROLLER.isIn(controller)) {
				return player.getOffhandItem();
			}
		} else {
			final BlockEntity be = player.level().getBlockEntity(pos);
			if (be instanceof final LecternControllerBlockEntity lcbe) {
				controller = lcbe.getController();
			}
		}
		return controller;
	}
}
