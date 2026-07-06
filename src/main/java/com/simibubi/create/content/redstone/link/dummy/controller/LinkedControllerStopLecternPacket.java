package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import io.netty.buffer.ByteBuf;

public class LinkedControllerStopLecternPacket extends LinkedControllerPacketBase {

	public static final StreamCodec<ByteBuf, LinkedControllerStopLecternPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(LinkedControllerStopLecternPacket::new, LinkedControllerPacketBase::getLecternPos);

	public LinkedControllerStopLecternPacket(final BlockPos lecternPos) {
		super(Objects.requireNonNull(lecternPos));
	}

	@Override
	protected void handleLectern(final ServerPlayer player, final LecternControllerBlockEntity lectern) {
		lectern.tryStopUsing(player);
	}

	@Override
	protected void handleItem(final ServerPlayer player, final ItemStack heldItem) {
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return null; //AllPackets.LINKED_CONTROLLER_USE_LECTERN;
	}
}
