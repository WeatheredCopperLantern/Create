package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.content.redstone.link.Frequency;
import com.simibubi.create.content.redstone.link.RedstoneEntityLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetwork;
import com.simibubi.create.content.redstone.link.RedstoneLinkableSnapshot;
import com.simibubi.create.content.redstone.link.RedstoneLinkableType;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import org.jspecify.annotations.NonNull;

public class RedstoneControllerLinkable extends RedstoneEntityLinkable {

	private int lifetime;

	@Override
	public void tick() {
		this.lifetime--;
		if (this.lifetime <= 0) {
			this.destroy();
		} else {
			super.tick();
		}
	}

	@Override
	public boolean doSave() {
		return false;
	}

	@Override
	public @NonNull RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_CONTROLLER.value();
	}

	@Override
	protected boolean shouldSetFrequency(final boolean first, final @NonNull Frequency frequency) {
		return true;
	}

	@Override
	protected void onFrequencyChanged(final boolean first) {

	}

	@Override
	protected void onModeChanged(final @NonNull RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected void onSignalChanged() {

	}

	public void refresh(final int lifetime, final Couple<Frequency> channel) {
		this.lifetime = lifetime;
		this.setChannel(channel);
		if(this.network == null){
			this.setEntity(this.entity);
		}
	}

	@Override
	protected boolean shouldSetMode(final boolean receiver) {
		return false;
	}

	public RedstoneControllerLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(nbt, channel, receiver, registries, dimensions, network);
	}

	public RedstoneControllerLinkable(final Couple<Frequency> channel, final Entity entity) {
		super(channel, entity);
		this.setTransmittedStrength(15);
	}
}
