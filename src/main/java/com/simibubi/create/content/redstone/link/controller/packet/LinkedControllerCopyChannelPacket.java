package com.simibubi.create.content.redstone.link.controller.packet;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import com.simibubi.create.content.redstone.link.interfaces.ILinkableBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.items.ItemStackHandler;

public class LinkedControllerCopyChannelPacket extends LinkedControllerPacketBase {

	//region Fields
	public static final StreamCodec<ByteBuf, LinkedControllerCopyChannelPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, channelPacket -> channelPacket.key, BlockPos.STREAM_CODEC, channelPacket -> channelPacket.pos, LinkedControllerCopyChannelPacket::new);

	private final int key;
	private final BlockPos pos;
	//endregion

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.LINKED_CONTROLLER_COPY_CHANNEL;
	}

	@Override
	public void handle(final ServerPlayer player) {
		if (player.isSpectator()) return;

		final ItemStack controller = this.getController(player);

		if (controller == null) return;

		final BlockEntity be = player.level().getBlockEntity(this.pos);
		if (be instanceof final ILinkableBlockEntity linkableBe) {
			ItemStackHandler controllerItems = LinkedControllerItem.getFrequencyItems(controller);
			linkableBe.getLinkable().channel.forEachWithContext((frequency, isFirst) -> controllerItems.setStackInSlot(this.key * 2 + (isFirst ? 0 : 1), frequency.stack.copy()));
			controller.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemHelper.containerContentsFromHandler(controllerItems));
		}
	}

	public LinkedControllerCopyChannelPacket(final int key, final BlockPos pos) {
		this.key = key;
		this.pos = pos;
	}
}
