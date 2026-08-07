package com.simibubi.create.content.redstone.link.linkable;

import java.util.UUID;
import java.util.function.Consumer;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.foundation.persistent.PersistentBlockBoundObject;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.ImmutableCouple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockBoundRedstoneLinkable extends PersistentBlockBoundObject implements IRedstoneLinkable {

	protected @Nullable LinkableBehaviour<? extends BlockBoundRedstoneLinkable> behaviour;
	protected Vector3fc cachedPosition;
	protected @Nullable RedstoneLinkableSnapshot lastSynced;

	protected ImmutableCouple<Frequency> channel;
	protected @Nullable RedstoneLinkNetwork network;
	protected boolean receiver;
	protected int signal;
	protected boolean recalcQueued;
	protected boolean updateQueued;

	public void updateFromWorld() {
		if (this.updateQueued) return;
		this.updateQueued = true;
		this.setDirty();
		Create.SCHEDULER.schedule(0, () -> {
			//Don't move this outside the runnable, because the behaviour is assigned by another runnable, so it might exist once this runs
			this.withBehaviourDo(LinkableBehaviour::updateFromWorld);
			this.updateQueued = false;
			this.setDirty();
		});
	}

	@Override
	public boolean isReceiver() {
		return receiver;
	}

	@Override
	public boolean setFrequency(final boolean first, final Frequency frequency) {
		if (this.channel.get(first) == frequency) return false;
		final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
		this.channel = this.channel.with(first, frequency);
		withNetworkDo(redstoneLinkNetwork -> redstoneLinkNetwork.channelChanged(this, snapshot));
		withBehaviourDo(behaviour -> behaviour.channelChanged(snapshot));
		this.channelChanged(snapshot);
		return true;
	}

	@Override
	public void setMode(final boolean receiver) {
		if (this.receiver == receiver) return;
		final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
		this.receiver = receiver;
		withNetworkDo(redstoneLinkNetwork -> redstoneLinkNetwork.modeChanged(this, snapshot));
		withBehaviourDo(behaviour -> behaviour.modeChanged(snapshot));
		this.modeChanged(snapshot);
	}

	@Override
	public void setSignal(final int signal) {
		if (this.signal == signal) return;
		final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
		this.signal = signal;
		if (this.isTransmitter()) {
			withNetworkDo(redstoneLinkNetwork -> redstoneLinkNetwork.signalChanged(this, snapshot));
		}
		withBehaviourDo(behaviour -> behaviour.signalChanged(snapshot));
		this.signalChanged(snapshot);
	}

	public void setBehaviour(final LinkableBehaviour<? extends BlockBoundRedstoneLinkable> behaviour) {
		this.behaviour = behaviour;
		this.cachedPosition = behaviour.getPos().getCenter().toVector3f();
		syncToBehaviour();
	}

	protected void syncToBehaviour() {
		if (this.lastSynced != null && this.lastSynced.matches(this)) return;
		withBehaviourDo(behaviour -> {
			behaviour.setLinkable(this);
			behaviour.readFromLinkable(this.lastSynced);
		});
		this.lastSynced = RedstoneLinkableSnapshot.of(this);
	}

	@Override
	public UUID getUUID() {
		return this.uuid;
	}

	@Override
	public ImmutableCouple<Frequency> getChannel() {
		return this.channel;
	}

	@Override
	public int getSignal() {
		return this.signal;
	}

	@Override
	public int getTransmissionRange() {
		return AllConfigs.server().logistics.linkRange.get();
	}

	@Override
	public int getReceivingRange() {
		return AllConfigs.server().logistics.linkRange.get();
	}

	@Override
	public Vector3fc getTransmissionPosition() {
		return this.cachedPosition;
	}

	@Override
	public Vector3fc getReceivingPosition() {
		return this.cachedPosition;
	}

	@Override
	public void considerRecalcQueued() {
		this.recalcQueued = true;
	}

	@Override
	public boolean allowRecalcQueue() {
		return !this.recalcQueued;
	}

	@Override
	public void considerRecalcDequeued() {
		this.recalcQueued = false;
	}

	protected BlockBoundRedstoneLinkable(final Block block, final BlockPos blockPos, final BlockState state, final boolean receiver, final ImmutableCouple<Frequency> channel, final ServerLevel serverLevel) {
		super(block, blockPos, state, serverLevel);
		this.cachedPosition = blockPos.getCenter().toVector3f();
		this.receiver = receiver;
		this.signal = 0;
		this.channel = channel;
	}

	protected BlockBoundRedstoneLinkable(final Block block, final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		super(block, compoundTag, registries, serverLevel);
		final ListTag listTag = compoundTag.getList("Position", Tag.TAG_COMPOUND);
		this.cachedPosition = new Vector3f(listTag.getFloat(0), listTag.getFloat(1), listTag.getFloat(2));
		this.receiver = compoundTag.getBoolean("Receiver");
		this.signal = compoundTag.getInt("Signal");
		this.channel = ImmutableCouple.createWithContext(aBoolean -> Frequency.read(compoundTag.get(aBoolean ? LinkableBehaviour.FIRST_FREQUENCY : LinkableBehaviour.SECOND_FREQUENCY), registries));
	}

	protected abstract void channelChanged(final RedstoneLinkableSnapshot snapshot);

	protected abstract void modeChanged(final RedstoneLinkableSnapshot snapshot);

	protected abstract void signalChanged(final RedstoneLinkableSnapshot snapshot);

	protected void withBehaviourDo(final Consumer<LinkableBehaviour<? extends BlockBoundRedstoneLinkable>> action) {
		if (this.behaviour != null) action.accept(this.behaviour);
	}

	protected void withNetworkDo(final Consumer<RedstoneLinkNetwork> action) {
		if (this.network != null) action.accept(this.network);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void blockUnloaded() {
		this.behaviour = null;
		this.lastSynced = null;
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void blockDestroyed() {
		super.blockDestroyed();
		this.removeFromNetwork();
	}

	public void removeFromNetwork() {
		withNetworkDo(network -> network.removeLinkableIn(this, 1));
		this.network = null;
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	protected void initialize() {
		super.initialize();
		this.network = Create.REDSTONE_LINK_NETWORK.getNetwork(this.level);
		this.network.addLinkable(this);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public Tag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		super.save(tag, registries);
		final ListTag listTag = new ListTag();
		listTag.add(FloatTag.valueOf(this.cachedPosition.x()));
		listTag.add(FloatTag.valueOf(this.cachedPosition.y()));
		listTag.add(FloatTag.valueOf(this.cachedPosition.z()));
		tag.put("Position", listTag);
		tag.putBoolean("Receiver", this.receiver);
		tag.putInt("Signal", this.signal);
		channel.forEachWithParams((frequency, key) -> tag.put(key, frequency.write(registries)), Couple.create(LinkableBehaviour.FIRST_FREQUENCY, LinkableBehaviour.SECOND_FREQUENCY));

		if (this.updateQueued) {
			tag.putBoolean("UpdateQueued", true);
		}

		return tag;
	}
}
