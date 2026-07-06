package com.simibubi.create.content.redstone.link.dummy.interfaces;

import com.simibubi.create.content.redstone.link.dummy.AbstractRedstoneLinkable;
import com.simibubi.create.content.redstone.link.dummy.RedstoneLinkNetwork;
import com.simibubi.create.content.redstone.link.dummy.RedstoneLinkNetworkHandler;
import net.createmod.catnip.data.Couple;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.ApiStatus;

/**
 * Extend {@link AbstractRedstoneLinkable} or its subclasses whenever possible.
 * Direct implementation of this interface is not considered stable and is likely to break in future versions.
 */
@ApiStatus.Internal
public interface IRedstoneLinkable {

	enum Mode {
		TRANSMIT, RECEIVE
	}

	void clearNetwork(int inTicks);

	void clearNetwork();

	int getTransmittedStrength();

	void setReceivedStrength(int power);

	boolean isListening();

	void considerQueued();

	void considerDequeued();

	boolean allowUpdateQueue();

	boolean allowRecalcQueue();

	void queueUpdate();

	void delayedUpdate();

	Couple<RedstoneLinkNetworkHandler.Frequency> getChannelKey();

	Vec3 getLocation();

	Level getLevel();

	void setNetwork(RedstoneLinkNetwork newNetwork);

	RedstoneLinkNetwork getNetwork();

	void setMode(Mode newMode);

	void setFrequency(boolean first, ItemStack stack);

	void notifySignalChange();

	int transmissionRange();

	int receivingRange();
}
