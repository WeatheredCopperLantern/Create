package com.simibubi.create.content.redstone.link.linkable;

import java.util.function.Consumer;

import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.redstone.link.redstoneLink.RedstoneLinkBlock;
import com.simibubi.create.content.trains.graph.DimensionPalette;

import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockEntityRedstoneLinkable<T extends LinkableBlockEntity<?>> extends RedstoneLinkable {

	protected T blockEntity;
	protected Vector3fc cachedPosition;

	@Override
	public void readAdditional(final CompoundTag tag, final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		final ListTag listTag = tag.getList("position", Tag.TAG_COMPOUND);
		this.cachedPosition = new Vector3f(listTag.getFloat(0), listTag.getFloat(1), listTag.getFloat(2));
	}

	@Override
	public void writeAdditional(final CompoundTag tag, final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		final ListTag listTag = new ListTag();
		listTag.add(FloatTag.valueOf(this.cachedPosition.x()));
		listTag.add(FloatTag.valueOf(this.cachedPosition.y()));
		listTag.add(FloatTag.valueOf(this.cachedPosition.z()));
		tag.put("position", listTag);
	}

	@Override
	public void representationUnloaded() {
		this.blockEntity = null;
	}

	@Override
	public Vector3fc getTransmissionPosition() {
		return this.cachedPosition;
	}

	@Override
	protected void onFrequencyChanged(final boolean first) {
		this.withBeDo(blockEntity -> {
			blockEntity.channel = this.channel;
			blockEntity.sendData();
		});
	}

	public void setBlockEntity(final T blockEntity) {
		this.blockEntity = blockEntity;
		this.cachedPosition = blockEntity.getBlockPos().getCenter().toVector3f();
	}

	@Override
	public void delayedUpdate() {
		super.delayedUpdate();
		this.withBeDo(be -> {
			final BlockState state = be.getBlockState();
			assert be.getLevel() != null;
			((RedstoneLinkBlock) state.getBlock()).updateFromWorld(be.getLevel(), be.getBlockPos(), state);
		});
	}

	@Override
	protected boolean shouldSetFrequency(final boolean first, final Frequency frequency) {
		return true;
	}

	@Override
	protected void onModeChanged(final RedstoneLinkableSnapshot snapshot) {
		this.withBeDo(blockEntity -> blockEntity.updateFromLinkable());
	}

	@Override
	protected void onSignalChanged() {
		this.withBeDo(blockEntity -> blockEntity.updateFromLinkable());
	}

	@Override
	protected boolean shouldSetMode(final boolean receiver) {
		return true;
	}

	protected void withBeDo(final Consumer<T> action) {
		if (this.blockEntity != null) action.accept(this.blockEntity);
	}

	protected BlockEntityRedstoneLinkable(final CompoundTag tag, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(tag, channel, receiver, registries, dimensions, network);
	}

	protected BlockEntityRedstoneLinkable(final Couple<Frequency> channel) {
		super(channel);
	}
}
