package com.simibubi.create.content.redstone.link.linkable;

import java.util.UUID;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.ImmutableCouple;

import org.joml.Vector3fc;

public interface IRedstoneLinkable {

	/**
	 * Called from block/behaviour
	 */
	void setMode(boolean receiver);

	boolean setFrequency(boolean first, Frequency frequency);

	boolean isReceiver();

	default boolean isTransmitter() {
		return !isReceiver();
	}

	UUID getUUID();

	ImmutableCouple<Frequency> getChannel();

	int getSignal();

	int getTransmissionRange();

	int getReceivingRange();

	Vector3fc getTransmissionPosition();

	Vector3fc getReceivingPosition();

	void considerRecalcQueued();

	boolean allowRecalcQueue();

	void considerRecalcDequeued();

	void setSignal(int signal);
}
