package com.simibubi.create.content.redstone.link.controller.packet;

import java.util.BitSet;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import io.netty.buffer.ByteBuf;

public class LinkedControllerInputPacket extends LinkedControllerPacketBase {

	//region Fields
	public static final StreamCodec<ByteBuf, LinkedControllerInputPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, inputPacket -> inputPacket.keys.toByteArray(), CatnipStreamCodecs.NULLABLE_BLOCK_POS, inputPacket -> inputPacket.pos, LinkedControllerInputPacket::new);

	private final BitSet keys;
	private final BlockPos pos;
	//endregion

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.LINKED_CONTROLLER_INPUT;
	}

	@Override
	public void handle(final ServerPlayer player) {
		if (player.isSpectator() && !this.keys.isEmpty()) return;

		final ItemStack controller = this.getController(player, this.pos);

		if (controller != null) {
			LinkedControllerServerHandler.handle(this.keys, player, controller);
		}
	}

	public LinkedControllerInputPacket(final BitSet keys, final BlockPos pos) {
		this.keys = keys;
		this.pos = pos;
	}

	public LinkedControllerInputPacket(final byte[] keys, final BlockPos pos) {
		this.keys = BitSet.valueOf(keys);
		this.pos = pos;
	}
}
