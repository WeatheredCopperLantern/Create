package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import org.joml.Vector3f;

public class RedstoneLinkLinkable extends RedstoneLinkable {

	private RedstoneLinkBlockEntity blockEntity;

	public RedstoneLinkLinkable(final Couple<Frequency> channel, final RedstoneLinkBlockEntity be) {
		super(channel);
		this.blockEntity = be;
		assert be.getLevel() != null;
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(be.getLevel()));
	}

	public RedstoneLinkLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		super(nbt, channel, receiver, registries, dimensions);
	}

	public void setBlockEntity(final RedstoneLinkBlockEntity be) {
		this.blockEntity = be;
	}

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_LINK.value();
	}

	@Override
	public Vector3f getTransmissionPosition() {
		return this.blockEntity.getBlockPos().getCenter().toVector3f();
	}

	@Override
	public void delayedUpdate() {
		super.delayedUpdate();
		this.blockEntity.delayedUpdate();
	}

	@Override
	protected void onModeChanged(final RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected boolean shouldSetMode(final boolean receiver) {
		return true;
	}
}
