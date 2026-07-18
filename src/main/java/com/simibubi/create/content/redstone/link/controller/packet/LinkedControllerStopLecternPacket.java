package com.simibubi.create.content.redstone.link.controller.packet;

import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.controller.lecternController.LecternControllerBlockEntity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.netty.buffer.ByteBuf;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LinkedControllerStopLecternPacket extends LinkedControllerPacketBase {

	//region Fields
	public static final StreamCodec<ByteBuf, LinkedControllerStopLecternPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(LinkedControllerStopLecternPacket::new, stopLecternPacket -> stopLecternPacket.pos);

	private final BlockPos pos;
	//endregion

	@Override
	public PacketTypeProvider getTypeProvider() {
		return AllPackets.LINKED_CONTROLLER_USE_LECTERN;
	}

	@Override
	public void handle(final ServerPlayer player) {
		final BlockEntity be = player.level().getBlockEntity(this.pos);
		if (be instanceof final LecternControllerBlockEntity lecternControllerBlockEntity) {
			lecternControllerBlockEntity.tryStopUsing(player);
			Create.LINKED_CONTROLLER_HANDLER.remove(player);
		}
	}

	public LinkedControllerStopLecternPacket(final BlockPos lecternPos) {
		this.pos = lecternPos;
	}
}
