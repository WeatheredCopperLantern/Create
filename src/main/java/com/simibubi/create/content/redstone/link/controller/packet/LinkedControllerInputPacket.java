package com.simibubi.create.content.redstone.link.controller.packet;

import java.util.BitSet;

import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import io.netty.buffer.ByteBuf;
import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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

		final ItemStackHandler frequencyItems = this.getFrequencyItems(player, this.pos);

		if (frequencyItems != null) {
			Create.LINKED_CONTROLLER_HANDLER.handle(this.keys, player, frequencyItems);
		}
	}

	public LinkedControllerInputPacket(final BitSet keys, final @Nullable BlockPos pos) {
		this.keys = keys;
		this.pos = pos;
	}

	public LinkedControllerInputPacket(final byte[] keys, final BlockPos pos) {
		this.keys = BitSet.valueOf(keys);
		this.pos = pos;
	}
}
