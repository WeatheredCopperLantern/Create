package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import io.netty.buffer.ByteBuf;

public class LinkedControllerInputPacket extends LinkedControllerPacketBase {

	public static final StreamCodec<ByteBuf, LinkedControllerInputPacket> STREAM_CODEC = StreamCodec.composite(CatnipStreamCodecBuilders.list(ByteBufCodecs.INT), p -> p.activatedButtons, ByteBufCodecs.BOOL, p -> p.press, CatnipStreamCodecs.NULLABLE_BLOCK_POS, LinkedControllerPacketBase::getLecternPos, LinkedControllerInputPacket::new);

	private final List<Integer> activatedButtons;
	private final boolean press;

	public LinkedControllerInputPacket(final Collection<Integer> activatedButtons, final boolean press) {
		this(activatedButtons, press, null);
	}

	public LinkedControllerInputPacket(final Collection<Integer> activatedButtons, final boolean press, final BlockPos lecternPos) {
		super(lecternPos);
		this.activatedButtons = List.copyOf(activatedButtons);
		this.press = press;
	}

	@Override
	protected void handleLectern(final ServerPlayer player, final LecternControllerBlockEntity lectern) {
		if (lectern.isUsedBy(player)) this.handleItem(player, lectern.getController());
	}

	@Override
	protected void handleItem(final ServerPlayer player, final ItemStack heldItem) {
		final Level world = player.getCommandSenderWorld();
		final UUID uniqueID = player.getUUID();
		final BlockPos pos = player.blockPosition();

		if (player.isSpectator() && this.press) return;

		LinkedControllerServerHandler.receivePressed(player, world, uniqueID, this.activatedButtons.stream().map(i -> LinkedControllerItem.toFrequency(heldItem, i)).collect(Collectors.toList()), this.press);
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return null; //AllPackets.LINKED_CONTROLLER_INPUT;
	}
}
