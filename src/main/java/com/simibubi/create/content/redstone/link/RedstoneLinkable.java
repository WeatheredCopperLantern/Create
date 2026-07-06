package com.simibubi.create.content.redstone.link;

import java.util.UUID;

import com.simibubi.create.CreateBuildInfo;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.foundation.mixin.accessor.CValueAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3fc;

/**
 * Base class for {@code Receiver/Transmitter behaviour} implementations. <br>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *  <li>An instance is created through {@code TODO creation through be, etc.} or {@code TODO creation through loading savedata}.</li>
 *  <li>An instance is destroyed when {@code TODO destruction through be, etc.} this will call {@link RedstoneLinkable#onDestroy()}
 *  or {@code TODO creation through a server shudown} this will call {@link RedstoneLinkable#write(HolderLookup.Provider, DimensionPalette) write}.</li>
 *  <li style="color:red">An instance is <b>NOT</b> destroyed/created when the {@code TODO be, etc.} is unloaded/loaded.</li>
 * </ol>
 *
 * <h2>Extension Guidelines</h2>
 * An instance needs to behave the same, if the corresponding {@code TODO be, etc.} is loaded or not. <br>
 * Instances only exist on the Server.
 * <h3>This means it needs to</h3>
 * <ul>
 * 	<li>Do all the processing of received signals.</li>
 *  <li>Store (and save/load) a copy of all {@code TODO be, etc.} data needed for operation.</li>
 *  <li>Sync data to/from the {@code TODO be, etc.} when/while it is loaded.</li>
 *  <li>Use the {@code TODO be, etc.} to sync with clients.</li>
 * </ul>
 *
 * <h3>And can't</h3>
 * <ul>
 *   <li>Rely on directly acessing {@code TODO be, etc.} or chunk data for operation.</li>
 *   <li>Sync with clients directly.</li>
 * </ul>
 *
 * <h2>Saving/Loading</h2>
 * <ul>
 * 	<li>All {@link RedstoneLinkable}s have a {@link UUID}, that can be stored by a {@code TODO be, etc.} to relink after loading.</li>
 * 	<li>Override {@link RedstoneLinkable#writeAdditional(CompoundTag, HolderLookup.Provider, DimensionPalette) writeAdditional} to save custom data.</li>
 * 	<li>The {@link UUID}, {@code received/transmitted signal}, {@code channel} and {@code mode} are already saved automatically.</li>
 * </ul>
 */
public abstract class RedstoneLinkable {

	public final UUID uuid;
	public final Couple<Frequency> channel;
	public RedstoneLinkNetwork network;
	public boolean receiver;
	public int signal;
	public boolean recalcQueued;
	public boolean updateQueued;

	//TODO: Build caching for linkables in communication range, so we can completely skip any checks for signal change/removal updates
	//Transmission/Receiving ranges get queried a lot. Skipping the ConfigBase#get() method actualy has a meassurable impact on performance.
	@SuppressWarnings({"CastToIncompatibleInterface", "unchecked", "LawOfDemeter"})
	private static final ModConfigSpec.ConfigValue<Integer> logistics_linkRange = (ModConfigSpec.ConfigValue<Integer>) ((CValueAccessor) AllConfigs.server().logistics.linkRange).create$getRawValue();

	@Contract(pure = true)
	public boolean doSave() {
		return true;
	}

	@ApiStatus.NonExtendable
	public CompoundTag write(final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		final CompoundTag nbt = new CompoundTag();
		// Frequencies and Mode are already in the parent nbt
		nbt.putInt("Signal", this.signal);
		nbt.putUUID("UUID", this.uuid);

		final CompoundTag additionalNBT = new CompoundTag();
		this.writeAdditional(additionalNBT, registries, dimensions);
		nbt.put("Additional", additionalNBT);

		return nbt;
	}

	public void writeAdditional(final CompoundTag nbt, final HolderLookup.Provider registries, final DimensionPalette dimensions) {
	}

	protected RedstoneLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		this.channel = channel;
		this.receiver = receiver;
		this.network = network;
		this.signal = nbt.getInt("Signal");
		this.uuid = nbt.getUUID("UUID");

		this.readAdditional(nbt.getCompound("Additional"), registries, dimensions);
	}

	protected RedstoneLinkable(final Couple<Frequency> channel) {
		this.uuid = UUID.randomUUID();
		this.channel = channel;
	}

	public void readAdditional(final CompoundTag nbt, final HolderLookup.Provider registries, final DimensionPalette dimensions) {

	}

	public void onDestroy() {
		this.network.removeIn(this, 1);
	}

	@Contract(pure = true)
	public boolean isReceiver() {
		return this.receiver;
	}

	@Contract(pure = true)
	public boolean isTransmitter() {
		return !this.receiver;
	}

	@Contract(pure = true)
	public abstract RedstoneLinkableType getType();

	@Contract(pure = true)
	public boolean allowRecalcQueue() {
		return !this.recalcQueued;
	}

	public void considerRecalcQueued() {
		this.recalcQueued = true;
	}

	public void considerRecalcDequeued() {
		this.recalcQueued = false;
	}

	@Contract(pure = true)
	public boolean allowUpdateQueue() {
		return !this.updateQueued;
	}

	public void considerUpdateQueued() {
		this.updateQueued = true;
	}

	public void queueUpdate() {
		this.queueUpdate(false);
	}

	public void queueUpdate(final boolean force) {
		if (force || this.allowUpdateQueue()) {
			this.considerUpdateQueued();
			this.network.queueUpdate(this);
		}
	}

	@Contract(pure = true)
	public int getTransmittedStrength() {
		return this.receiver ? 0 : this.signal;
	}

	public void setTransmittedStrength(final int signal) {
		CreateBuildInfo.runIfDev(() -> {
			if (this.receiver) {
				throw new UnsupportedOperationException();
			}
		});
		if (this.signal != signal) {
			final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
			this.signal = signal;
			this.network.signalChanged(this, snapshot);
		}
	}

	public void setReceivedStrength(final int signal) {
		CreateBuildInfo.runIfDev(() -> {
			if (!this.receiver) {
				throw new UnsupportedOperationException();
			}
		});
		if (signal != this.signal) {
			this.signal = signal;
			this.onSignalChanged();
		}
	}

	@Contract(pure = true)
	public int getReceivedStrength() {
		return this.receiver ? this.signal : 0;
	}

	@Contract(pure = true)
	public int getReceivingRange() {
		return RedstoneLinkable.logistics_linkRange.get();
	}

	@Contract(pure = true)
	public int getTransmissionRange() {
		return RedstoneLinkable.logistics_linkRange.get();
	}

	@Contract(pure = true)
	public Vector3fc getReceivingPosition() {
		return this.getTransmissionPosition();
	}

	@Contract(pure = true)
	public abstract Vector3fc getTransmissionPosition();

	public void setNetwork(final RedstoneLinkNetwork network) {
		if (this.network != null) {
			this.network.removeLinkable(this);
		}

		this.network = network;
		this.network.addLinkable(this);
	}

	public void delayedUpdate() {
		this.updateQueued = false;
	}

	public void setFrequency(final boolean first, final Frequency frequency) {
		if (this.channel.get(first).equals(frequency) || !this.shouldSetFrequency(first, frequency)) return;
		final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
		this.channel.set(first, frequency);
		if (this.network != null) {
			this.network.channelChanged(this, snapshot);
		}
		this.onFrequencyChanged(first);
	}

	protected abstract boolean shouldSetFrequency(boolean first, Frequency frequency);

	protected abstract void onFrequencyChanged(boolean first);

	public void setMode(final boolean receiver) {
		if (this.receiver == receiver || !this.shouldSetMode(receiver)) return;
		final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
		this.signal = 0;
		this.receiver = receiver;
		if (this.network != null) {
			this.network.modeChanged(this, snapshot);
		}
		this.onModeChanged(snapshot);
	}

	protected abstract void onModeChanged(final RedstoneLinkableSnapshot snapshot);

	protected abstract void onSignalChanged();

	protected int getRawSignal() {
		return this.signal;
	}

	protected abstract boolean shouldSetMode(final boolean receiver);
}
