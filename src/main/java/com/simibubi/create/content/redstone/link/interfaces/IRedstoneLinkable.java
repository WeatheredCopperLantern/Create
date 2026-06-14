package com.simibubi.create.content.redstone.link.interfaces;

import com.simibubi.create.content.redstone.link.RedstoneLinkNetwork;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import net.createmod.catnip.data.Couple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Extend {@link com.simibubi.create.content.redstone.link.AbstractRedstoneLinkable} or its subclasses whenever possible.
 * Direct implementation of this interface is not considered stable and is likely to break in future versions.
 */
@Internal
public interface IRedstoneLinkable {

	public enum Mode {
		TRANSMIT, RECEIVE
	}

	public void clearNetwork(int inTicks);

	public void clearNetwork();

	public int getTransmittedStrength();

	public void setReceivedStrength(int power);

	public boolean isListening();

	public boolean allowQueue();

	public void queueUpdate();

	public void delayedUpdate();

	public Couple<Frequency> getChannelKey();

	public Vec3 getLocation();

	public Level getLevel();

	public void setNetwork(RedstoneLinkNetwork newNetwork);

	public RedstoneLinkNetwork getNetwork();

	public void setMode(Mode newMode);

	public void setFrequency(boolean first, ItemStack stack);

	public void notifySignalChange();

	public int transmissionRange();

	public int receivingRange();
}
