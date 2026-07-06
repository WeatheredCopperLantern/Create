package com.simibubi.create.content.redstone.link;

import net.createmod.catnip.data.Couple;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

public record RedstoneLinkableSnapshot(Vector3fc receivingPosition, Vector3fc transmissionPosition, int receivingRange, int transmissionRange,
                                       int signal, boolean receiver, Couple<Frequency> channel) {

	static RedstoneLinkableSnapshot of(final @NonNull RedstoneLinkable linkable) {
		final Vector3fc receivingPosition = linkable.getReceivingPosition();
		final Vector3fc transmissionPosition = linkable.getTransmissionPosition();
		final int receivingRange = linkable.getReceivingRange();
		final int transmissionRange = linkable.getTransmissionRange();
		final int signal = linkable.getRawSignal();
		final boolean receiver = linkable.isReceiver();
		final Couple<Frequency> channel = linkable.channel.copy();

		return new RedstoneLinkableSnapshot(receivingPosition, transmissionPosition, receivingRange, transmissionRange, signal, receiver, channel);
	}
}
