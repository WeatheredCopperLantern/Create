package com.simibubi.create.content.redstone.link;

import net.createmod.catnip.data.Couple;

import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

public record RedstoneLinkableSnapshot(Vector3f receivingPosition, Vector3f transmissionPosition, int receivingRange, int transmissionRange,
                                       int signal, boolean receiver, Couple<Frequency> channel) {

	static RedstoneLinkableSnapshot of(final @NonNull RedstoneLinkable linkable) {
		final Vector3f receivingPosition = linkable.getReceivingPosition();
		final Vector3f transmissionPosition = linkable.getTransmissionPosition();
		final int receivingRange = linkable.getReceivingRange();
		final int transmissionRange = linkable.getTransmissionRange();
		final int signal = linkable.getRawSignal();
		final boolean receiver = linkable.isReceiver();
		final Couple<Frequency> channel = linkable.channel;

		return new RedstoneLinkableSnapshot(receivingPosition, transmissionPosition, receivingRange, transmissionRange, signal, receiver, channel);
	}
}
