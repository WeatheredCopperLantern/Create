package com.simibubi.create.content.redstone.link.redstoneLink;

import java.util.function.Consumer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableSnapshot;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableType;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

public class RedstoneLinkLinkable extends RedstoneLinkable {

	private RedstoneLinkBlockEntity blockEntity;
	private Vector3fc cachedPosition;

	@Override
	public void readAdditional(final @NonNull CompoundTag nbt, final HolderLookup.@NonNull Provider registries, final @NonNull DimensionPalette dimensions) {
		final ListTag tag = nbt.getList("position", Tag.TAG_COMPOUND);
		this.cachedPosition = new Vector3f(tag.getFloat(0), tag.getFloat(1), tag.getFloat(2));
	}

	@Override
	public void writeAdditional(final @NonNull CompoundTag nbt, final HolderLookup.@NonNull Provider registries, final @NonNull DimensionPalette dimensions) {
		final ListTag tag = new ListTag();
		tag.add(FloatTag.valueOf(this.cachedPosition.x()));
		tag.add(FloatTag.valueOf(this.cachedPosition.y()));
		tag.add(FloatTag.valueOf(this.cachedPosition.z()));
		nbt.put("position", tag);
	}

	@Override
	public void representationUnloaded() {
		this.blockEntity = null;
	}

	@Override
	public void delayedUpdate() {
		super.delayedUpdate();
		this.withBeDo(be -> {
			final BlockState state = be.getBlockState();
			((RedstoneLinkBlock) state.getBlock()).updateFromWorld(be.getLevel(), be.getBlockPos(), state);
		});
	}

	@Override
	protected boolean shouldSetFrequency(final boolean first, final @NonNull Frequency frequency) {
		return true;
	}

	@Override
	protected void onFrequencyChanged(final boolean first) {
		this.withBeDo(redstoneLinkBlockEntity -> {
			redstoneLinkBlockEntity.channel = this.channel;
			redstoneLinkBlockEntity.sendData();
		});
	}

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_LINK.value();
	}

	@Override
	public Vector3fc getTransmissionPosition() {
		return this.cachedPosition;
	}

	@Override
	protected void onModeChanged(final @NonNull RedstoneLinkableSnapshot snapshot) {
		this.withBeDo(redstoneLinkBlockEntity -> {
			AllBlocks.REDSTONE_LINK.get().updateFromLinkable(redstoneLinkBlockEntity.getLevel(), redstoneLinkBlockEntity.getBlockPos());
		});
	}

	@Override
	protected void onSignalChanged() {
		this.withBeDo(redstoneLinkBlockEntity -> {
			AllBlocks.REDSTONE_LINK.get().updateFromLinkable(redstoneLinkBlockEntity.getLevel(), redstoneLinkBlockEntity.getBlockPos());
		});
	}

	@Override
	public void setReceivedStrength(final int signal) {
		super.setReceivedStrength(signal);
	}

	private void withBeDo(final Consumer<RedstoneLinkBlockEntity> action) {
		if (this.blockEntity != null) action.accept(this.blockEntity);
	}

	@Override
	protected boolean shouldSetMode(final boolean receiver) {
		return true;
	}

	public RedstoneLinkLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(nbt, channel, receiver, registries, dimensions, network);
	}

	public RedstoneLinkLinkable(final Couple<Frequency> channel, final RedstoneLinkBlockEntity be) {
		super(channel);
		this.setBlockEntity(be);
		assert be.getLevel() != null;
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(be.getLevel()));
	}

	public void setBlockEntity(final RedstoneLinkBlockEntity redstoneLinkBlockEntity) {
		this.blockEntity = redstoneLinkBlockEntity;
		this.cachedPosition = redstoneLinkBlockEntity.getBlockPos().getCenter().toVector3f();
	}
}
