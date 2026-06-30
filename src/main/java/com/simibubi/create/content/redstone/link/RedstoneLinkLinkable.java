package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.interfaces.ITickingLinkable;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.BlockGetter;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class RedstoneLinkLinkable extends RedstoneLinkable implements ITickingLinkable {

	private WeakReference<RedstoneLinkBlockEntity> blockEntity;

	private Vector3f positionCache;

	/**
	 * Used on place
	 */
	public RedstoneLinkLinkable(final Couple<Frequency> channel, final RedstoneLinkBlockEntity be) {
		super(channel);
		this.setBlockEntity(be);
		assert be.getLevel() != null;
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(be.getLevel()));
	}

	/**
	 * Used on load
	 */
	public RedstoneLinkLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(nbt, channel, receiver, registries, dimensions, network);
	}

	public void setBlockEntity(final RedstoneLinkBlockEntity be) {
		this.blockEntity = new WeakReference<>(be);
		this.positionCache = be.getBlockPos().getCenter().toVector3f();
	}

	private void doWithBeIfExist(final Consumer<RedstoneLinkBlockEntity> action) {
		final RedstoneLinkBlockEntity be = this.blockEntity.get();
		if(be != null){
			action.accept(be);
		}
	}

	@Override
	public Vector3f getTransmissionPosition() {
		return this.positionCache;
	}

	@Override
	public void setReceivedStrength(int signal) {
		super.setReceivedStrength(signal);
		doWithBeIfExist(redstoneLinkBlockEntity -> redstoneLinkBlockEntity.receive(signal));
	}

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_LINK.value();
	}

	@Override
	public void delayedUpdate() {
		super.delayedUpdate();
		doWithBeIfExist(RedstoneLinkBlockEntity::delayedUpdate);
	}

	@Override
	protected void onModeChanged(final RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected boolean shouldSetMode(final boolean receiver) {
		return true;
	}

	@Override
	public void tick() {
		if(this.blockEntity == null || this.blockEntity.get() == null){
			Create.LOGGER.warn("be not set");
		}
	}
}
