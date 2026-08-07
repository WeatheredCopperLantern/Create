package com.simibubi.create.content.redstone.link.controller;

import java.util.UUID;

import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.RedstoneEntityLinkable;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.ImmutableCouple;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import org.joml.Vector3fc;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneControllerLinkable extends RedstoneEntityLinkable {

	private int lifetime;

	@Override
	public void tick() {
		this.lifetime--;
		if (this.lifetime <= 0) {
		//	this.removeFromNetworkInstantly();
		} else {
			super.tick();
		}
	}

	@Override
	public UUID getUUID() {
		return null;
	}

	@Override
	public ImmutableCouple<Frequency> getChannel() {
		return null;
	}

	@Override
	public int getSignal() {
		return 0;
	}

	//@Override
	//public void queueUpdate() {
//
	//}

	@Override
	public void setMode(final boolean receiver) {

	}

	@Override
	public boolean setFrequency(final boolean first, final Frequency frequency) {

		return first;
	}

	@Override
	public int getTransmissionRange() {
		return 0;
	}

	@Override
	public int getReceivingRange() {
		return 0;
	}

	@Override
	public Vector3fc getReceivingPosition() {
		return null;
	}

	@Override
	public void considerRecalcQueued() {

	}

	@Override
	public boolean allowRecalcQueue() {
		return false;
	}

	@Override
	public void considerRecalcDequeued() {

	}

	//@Override
	//public void removeFromNetwork() {
//
	//}
//
	//@Override
	//public void removeFromNetworkInstantly() {
//
	//}
//
	//@Override
	//public boolean doSave() {
	//	return false;
	//}

	@Override
	public void setSignal(final int signal) {

	}

	@Override
	public boolean isReceiver() {
		return false;
	}

	//@Override
	//public RedstoneLinkableType getType() {
	//	return AllRedstoneLinkables.REDSTONE_CONTROLLER.value();
	//}

	public void refresh(final int lifetime, final ImmutableCouple<Frequency> channel) {
		//this.lifetime = lifetime;
		//this.setChannel(channel);
		//if(this.network == null){
		//	this.setEntity(this.entity);
		//}
	}

	//@Override
	protected boolean shouldSetMode(final boolean receiver) {
		return false;
	}

	//public RedstoneControllerLinkable(final CompoundTag compound, final ImmutableCouple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
	//	super(compound, channel, receiver, registries, dimensions, network);
	//}
//
	//public RedstoneControllerLinkable(final ImmutableCouple<Frequency> channel, final Entity entity) {
	//	super(channel, entity);
	//	this.setTransmittedStrength(15);
	//}
}
