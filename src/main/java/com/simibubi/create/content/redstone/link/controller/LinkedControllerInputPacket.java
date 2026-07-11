package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.redstone.link.dummy.controller.LinkedControllerItem;
import com.simibubi.create.content.redstone.link.dummy.controller.LinkedControllerServerHandler;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.BitSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class LinkedControllerInputPacket implements ServerboundPacketPayload {

	public static final StreamCodec<ByteBuf, LinkedControllerInputPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BYTE_ARRAY, linkedControllerInputPacket -> linkedControllerInputPacket.keys.toByteArray(),
			CatnipStreamCodecs.NULLABLE_BLOCK_POS, linkedControllerInputPacket -> linkedControllerInputPacket.pos, LinkedControllerInputPacket::new
	);

	private final BitSet keys;
	private final BlockPos pos;

	public LinkedControllerInputPacket(BitSet keys, BlockPos pos) {
		this.keys = keys;
		this.pos = pos;
	}

	public LinkedControllerInputPacket(byte[] keys, BlockPos pos) {
		this.keys = BitSet.valueOf(keys);
		this.pos = pos;
	}

	@Override
	public void handle(ServerPlayer player) {
		final Level world = player.getCommandSenderWorld();
		final UUID uniqueID = player.getUUID();

		if (player.isSpectator() && !keys.isEmpty()) return;

		//LinkedControllerServerHandler.receivePressed(player, world, uniqueID, this.activatedButtons.stream().map(i -> LinkedControllerItem.toFrequency(heldItem, i)).collect(Collectors.toList()), this.press);
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.LINKED_CONTROLLER_INPUT;
	}
}
