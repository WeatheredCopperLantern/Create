package com.simibubi.create.content.redstone.link.linkable;

import java.util.UUID;

import com.simibubi.create.CreateBuildInfo;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.foundation.mixin.accessor.CValueAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Base class for {@code Receiver/Transmitter behaviour} implementations. <br>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *  <li>An instance can be created through {@link #RedstoneLinkable(Couple)} during gameplay or {@link #RedstoneLinkable(CompoundTag, Couple, boolean, HolderLookup.Provider, DimensionPalette, RedstoneLinkNetwork)} during loading.</li>
 *  <li>An instance is properly destroyed through {@link RedstoneLinkable#destroy()}.</li>
 *  <li style="color:red">An instance is <b>NOT</b> destroyed when the in world representation is unloaded.</li>
 * </ol>
 *
 * <h2>Extension Guidelines</h2>
 * An instance needs to behave the same, if the in world representation is loaded or not. <br>
 * Unless intentionally implemented otherwise. <br>
 * <p style="color:red">Instances should only ever exist on the Server.</p>
 * <h3>This means it needs to</h3>
 * <ul>
 * 	<li>Do all the processing of received signals.</li>
 *  <li>Store (and save/load) a copy of all in world representation data needed for operation.</li>
 *  <li>Sync data to/from the in world representation when/while it is loaded.</li>
 *  <li>Use the in world representation to sync with clients.</li>
 * </ul>
 *
 * <h3>And can't</h3>
 * <ul>
 *   <li>Rely on directly acessing the in world representation or chunk data for operation.</li>
 *   <li>Sync with clients directly.</li>
 * </ul>
 *
 * <h2>Saving/Loading</h2>
 * <ul>
 * 	<li>All {@link RedstoneLinkable}s have a {@link UUID}, that can be stored by the in world representation to relink after loading.</li>
 * 	<li>Override {@link RedstoneLinkable#writeAdditional(CompoundTag, HolderLookup.Provider, DimensionPalette) writeAdditional} to save custom data and use {@link #readAdditional(CompoundTag, HolderLookup.Provider, DimensionPalette) readAdditional} to read it.</li>
 * 	<li>The {@link UUID}, {@code received/transmitted signal}, {@code channel} and {@code mode} are already saved automatically.</li>
 * </ul>
 */

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class RedstoneLinkable {

	//region Fields
	//TODO: Build caching for linkables in communication range, so we can completely skip any checks for signal change/removal updates
	//Transmission/Receiving ranges get queried a lot. Skipping the ConfigBase#get() method actually has a measurable impact on performance.
	private static ModConfigSpec.ConfigValue<Integer> logistics_linkRange;

	public final UUID uuid;
	public final Couple<Frequency> channel;
	public @Nullable RedstoneLinkNetwork network;
	public boolean receiver;
	public int signal;
	public boolean recalcQueued;
	public boolean updateQueued;
	//endregion

	@Contract(pure = true)
	public boolean allowRecalcQueue() {
		return !this.recalcQueued;
	}

	@Contract(pure = true)
	public boolean allowUpdateQueue() {
		return !this.updateQueued;
	}

	public void considerRecalcDequeued() {
		this.recalcQueued = false;
	}

	public void considerRecalcQueued() {
		this.recalcQueued = true;
	}

	public void considerUpdateQueued() {
		this.updateQueued = true;
	}

	public void delayedUpdate() {
		this.updateQueued = false;
	}

	public void destroy() {
		if (this.network != null) {
			this.network.removeIn(this, 1);
		}
		this.network = null;
	}

	@Contract(pure = true)
	public boolean doSave() {
		return true;
	}

	protected int getRawSignal() {
		return this.signal;
	}

	@Contract(pure = true)
	public int getReceivedStrength() {
		return this.receiver ? this.signal : 0;
	}

	public void setReceivedStrength(final int signal) {
		CreateBuildInfo.runIfDev(() -> {
			if (!this.receiver) {
				throw new UnsupportedOperationException("");
			}
		});
		if (signal != this.signal) {
			this.signal = signal;
			this.onSignalChanged();
		}
	}

	@Contract(pure = true)
	public Vector3fc getReceivingPosition() {
		return this.getTransmissionPosition();
	}

	@SuppressWarnings("MethodMayBeStatic")
	@Contract(pure = true)
	public int getReceivingRange() {
		if(RedstoneLinkable.logistics_linkRange == null){
			//noinspection unchecked
			RedstoneLinkable.logistics_linkRange = (ModConfigSpec.ConfigValue<Integer>) ((CValueAccessor) AllConfigs.server().logistics.linkRange).create$getRawValue();
		}
		return RedstoneLinkable.logistics_linkRange.get();
	}

	@SuppressWarnings("MethodMayBeStatic")
	@Contract(pure = true)
	public int getTransmissionRange() {
		if(RedstoneLinkable.logistics_linkRange == null){
			//noinspection unchecked
			RedstoneLinkable.logistics_linkRange = (ModConfigSpec.ConfigValue<Integer>) ((CValueAccessor) AllConfigs.server().logistics.linkRange).create$getRawValue();
		}
		return RedstoneLinkable.logistics_linkRange.get();
	}

	@Contract(pure = true)
	public int getTransmittedStrength() {
		return this.receiver ? 0 : this.signal;
	}

	public void setTransmittedStrength(final int signal) {
		CreateBuildInfo.runIfDev(() -> {
			if (this.receiver) {
				throw new UnsupportedOperationException("");
			}
		});
		if (this.signal != signal && this.network != null) {
			final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
			this.signal = signal;
			this.network.signalChanged(this, snapshot);
		}
	}

	@Contract(pure = true)
	public boolean isReceiver() {
		return this.receiver;
	}

	@Contract(pure = true)
	public boolean isTransmitter() {
		return !this.receiver;
	}

	public void queueUpdate() {
		this.queueUpdate(false);
	}

	public void queueUpdate(final boolean force) {
		if (this.network != null && (force || this.allowUpdateQueue())) {
			this.considerUpdateQueued();
			this.network.queueUpdate(this);
		}
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

	public void setChannel(final Couple<Frequency> channel) {
		if (this.channel.getFirst() != channel.getFirst()) {
			this.setFrequency(true, channel.getFirst());
		}
		if (this.channel.getSecond() != channel.getSecond()) {
			this.setFrequency(false, channel.getSecond());
		}
	}

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

	public void setNetwork(final RedstoneLinkNetwork network) {
		if (this.network != null) {
			this.network.removeLinkable(this);
		}

		this.network = network;
		this.network.addLinkable(this);
	}

	@ApiStatus.NonExtendable
	public CompoundTag write(final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		final CompoundTag nbt = new CompoundTag();
		nbt.putInt("Signal", this.signal);
		nbt.putUUID("UUID", this.uuid);

		final CompoundTag additionalNBT = new CompoundTag();
		this.writeAdditional(additionalNBT, registries, dimensions);
		nbt.put("Additional", additionalNBT);

		return nbt;
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

	@Contract(pure = true)
	public abstract Vector3fc getTransmissionPosition();

	@Contract(pure = true)
	public abstract RedstoneLinkableType getType();

	protected abstract void onFrequencyChanged(boolean first);

	protected abstract void onModeChanged(final RedstoneLinkableSnapshot snapshot);

	protected abstract void onSignalChanged();

	public abstract void readAdditional(final CompoundTag nbt, final HolderLookup.Provider registries, final DimensionPalette dimensions);

	protected abstract boolean shouldSetFrequency(boolean first, Frequency frequency);

	protected abstract boolean shouldSetMode(final boolean receiver);

	public abstract void writeAdditional(final CompoundTag nbt, final HolderLookup.Provider registries, final DimensionPalette dimensions);
}
