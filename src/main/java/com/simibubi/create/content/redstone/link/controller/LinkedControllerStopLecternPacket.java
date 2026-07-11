package com.simibubi.create.content.redstone.link.controller;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class LinkedControllerStopLecternPacket implements ServerboundPacketPayload {

	public LinkedControllerStopLecternPacket(BlockPos lecternPos) {
	}

	@Override
	public void handle(ServerPlayer player) {

	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return null;
	}
}
