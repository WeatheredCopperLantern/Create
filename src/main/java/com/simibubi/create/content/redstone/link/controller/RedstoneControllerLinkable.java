package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.RedstoneEntityLinkable;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableType;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneControllerLinkable extends RedstoneEntityLinkable {

	private int lifetime;

	@Override
	public void tick() {
		this.lifetime--;
		if (this.lifetime <= 0) {
			this.removeFromNetworkInstantly();
		} else {
			super.tick();
		}
	}

	@Override
	public boolean doSave() {
		return false;
	}

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_CONTROLLER.value();
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

	public RedstoneControllerLinkable(final CompoundTag compound, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(compound, channel, receiver, registries, dimensions, network);
	}

	public RedstoneControllerLinkable(final Couple<Frequency> channel, final Entity entity) {
		super(channel, entity);
		this.setTransmittedStrength(15);
	}
}
