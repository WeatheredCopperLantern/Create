package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public class RedstoneEntityLinkableImp extends RedstoneEntityLinkable{

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_ENTITY.value();
	}

	@Override
	protected boolean shouldSetFrequency(boolean first, Frequency frequency) {
		return false;
	}

	@Override
	protected void onFrequencyChanged(boolean first) {

	}

	@Override
	protected void onModeChanged(RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected void onSignalChanged() {

	}

	@Override
	protected boolean shouldSetMode(boolean receiver) {
		return false;
	}

	public RedstoneEntityLinkableImp(CompoundTag nbt, Couple<Frequency> channel, boolean receiver, HolderLookup.Provider registries, DimensionPalette dimensions, RedstoneLinkNetwork network) {
		super(nbt, channel, receiver, registries, dimensions, network);
		setTransmittedStrength(15);
	}

	public RedstoneEntityLinkableImp(Couple<Frequency> channel, Entity entity) {
		super(channel, entity);
		setTransmittedStrength(15);
	}
}
