package com.simibubi.create.content.redstone.link.linkable;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.ImmutableCouple;

import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

public record RedstoneLinkableSnapshot(Vector3fc receivingPosition, Vector3fc transmissionPosition, int receivingRange, int transmissionRange,
                                       int signal, boolean receiver, ImmutableCouple<Frequency> channel) {

	public static RedstoneLinkableSnapshot of(final @NonNull IRedstoneLinkable linkable) {
		final Vector3fc receivingPosition = linkable.getReceivingPosition();
		final Vector3fc transmissionPosition = linkable.getTransmissionPosition();
		final int receivingRange = linkable.getReceivingRange();
		final int transmissionRange = linkable.getTransmissionRange();
		final int signal = linkable.getSignal();
		final boolean receiver = linkable.isReceiver();
		final ImmutableCouple<Frequency> channel = linkable.getChannel();

		return new RedstoneLinkableSnapshot(receivingPosition, transmissionPosition, receivingRange, transmissionRange, signal, receiver, channel);
	}

	public <T extends IRedstoneLinkable> boolean matches(final T linkable) {
		return this.receiver == linkable.isReceiver()
			&& this.signal == linkable.getSignal()
			&& this.channel.equals(linkable.getChannel())
			&& this.transmissionRange == linkable.getTransmissionRange()
			&& this.receivingRange == linkable.getReceivingRange()
			&& this.transmissionPosition.equals(linkable.getTransmissionPosition())
			&& this.receivingPosition.equals(linkable.getReceivingPosition());
	}

	@Override
	public boolean equals(final Object obj) {
		if(obj instanceof final RedstoneLinkableSnapshot snapshot){
			return this.receiver == snapshot.receiver
				&& this.signal == snapshot.signal
				&& this.channel.equals(snapshot.channel)
				&& this.transmissionRange == snapshot.transmissionRange
				&& this.receivingRange == snapshot.receivingRange
				&& this.transmissionPosition.equals(snapshot.transmissionPosition)
				&& this.receivingPosition.equals(snapshot.receivingPosition);
		}
		return false;
	}
}
