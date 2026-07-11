package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.redstone.link.dummy.LinkBehaviour;
import com.simibubi.create.content.redstone.link.dummy.controller.LinkedControllerItem;
import com.simibubi.create.content.redstone.link.interfaces.ILinkableBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

public class LinkedControllerBindPacket implements ServerboundPacketPayload {

	public static final StreamCodec<ByteBuf, LinkedControllerBindPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, p -> p.key, BlockPos.STREAM_CODEC, p -> p.pos, LinkedControllerBindPacket::new);

	private final int key;
	private final BlockPos pos;

	public LinkedControllerBindPacket(int key, BlockPos pos) {
		this.key = key;
		this.pos = pos;
	}

	@Override
	public void handle(ServerPlayer player) {
		if (player.isSpectator()) return;

		ItemStack heldItem = player.getMainHandItem();
		if (!AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
			heldItem = player.getOffhandItem();
			if (!AllItems.LINKED_CONTROLLER.isIn(heldItem)) return;
		}

		final BlockEntity be = player.level().getBlockEntity(this.pos);
		if (be instanceof ILinkableBlockEntity linkableBe) {
			final ItemStackHandler frequencyItems = LinkedControllerItem.getFrequencyItems(heldItem);
			linkableBe.getLinkable().channel.forEachWithContext((f, first) -> frequencyItems.setStackInSlot(this.key * 2 + (first ? 0 : 1), f.stack.copy()));

			heldItem.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemHelper.containerContentsFromHandler(frequencyItems));
		}
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.LINKED_CONTROLLER_BIND;
	}
}
