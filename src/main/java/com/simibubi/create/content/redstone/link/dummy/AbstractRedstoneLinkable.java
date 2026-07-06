package com.simibubi.create.content.redstone.link.dummy;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.simibubi.create.content.redstone.link.dummy.interfaces.IRedstoneLinkable;
import com.simibubi.create.foundation.mixin.accessor.CValueAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.common.ModConfigSpec;

public abstract class AbstractRedstoneLinkable implements IRedstoneLinkable {

	private static final ModConfigSpec.ConfigValue<Integer> logistics_linkRange = (ModConfigSpec.ConfigValue<Integer>) ((CValueAccessor) AllConfigs.server().logistics.linkRange).create$getRawValue();

	private RedstoneLinkNetworkHandler.Frequency frequencyFirst;
	private RedstoneLinkNetworkHandler.Frequency frequencyLast;
	private Mode mode;
	protected IntSupplier transmission;
	protected IntConsumer signalCallback;
	private RedstoneLinkNetwork network;

	private boolean queuedUpdate;
	private boolean queuedRecalc;

	protected AbstractRedstoneLinkable(final Couple<RedstoneLinkNetworkHandler.Frequency> channel, final Mode mode, final IntConsumer signalCallback, final IntSupplier transmission) {
		this(channel.getFirst(), channel.getSecond(), mode, signalCallback, transmission);
	}

	protected AbstractRedstoneLinkable(final RedstoneLinkNetworkHandler.Frequency first, final RedstoneLinkNetworkHandler.Frequency last, final Mode mode, final IntConsumer signalCallback, final IntSupplier transmission) {
		this.frequencyFirst = first;
		this.frequencyLast = last;
		this.signalCallback = signalCallback;
		this.transmission = transmission;
		this.mode = mode;
	}

	@Override
	public void queueUpdate() {
		if (!this.queuedUpdate && this.network != null) {
			this.queuedUpdate = true;
			this.network.queueDelayedUpdate(this);
		}
	}

	@Override
	public void considerQueued() {
		this.queuedRecalc = true;
	}

	@Override
	public void considerDequeued() {
		this.queuedRecalc = false;
	}

	@Override
	public boolean allowRecalcQueue() {
		return !this.queuedRecalc;
	}

	@Override
	public boolean allowUpdateQueue() {
		return !this.queuedUpdate;
	}

	@Override
	public void delayedUpdate() {
		this.queuedUpdate = false;
	}

	@Override
	public int receivingRange() {
		return AbstractRedstoneLinkable.logistics_linkRange.get();
	}

	@Override
	public int transmissionRange() {
		return AbstractRedstoneLinkable.logistics_linkRange.get();
	}

	public final void notifySignalChange() {
		if (this.network == null) return;
		this.network.signalChanged(this);
	}

	@Override
	public final int getTransmittedStrength() {
		return this.transmission.getAsInt();
	}

	@Override
	public final void setReceivedStrength(final int power) {
		this.signalCallback.accept(power);
	}

	@Override
	public final boolean isListening() {
		return this.mode == Mode.RECEIVE;
	}

	@Override
	public final Couple<RedstoneLinkNetworkHandler.Frequency> getChannelKey() {
		return Couple.create(this.frequencyFirst, this.frequencyLast);
	}

	@Override
	public final void setNetwork(final RedstoneLinkNetwork newNetwork) {
		if (newNetwork == this.network) return;
		if (this.network == null && this.isListening()) {
			this.network = newNetwork;
			this.network.addSilent(this);
		} else {
			if (this.network != null) this.network.remove(this);
			this.network = newNetwork;
			this.network.add(this);
		}
	}

	@Override
	public final void clearNetwork(final int inTicks) {
		if (this.network != null) {
			this.network.removeIn(this, inTicks);
		}
		this.network = null;
	}

	@Override
	public final void clearNetwork() {
		if (this.network != null) this.network.remove(this);
		this.network = null;
	}

	@Override
	public final RedstoneLinkNetwork getNetwork() {
		return this.network;
	}

	/**
	 * Override {@link AbstractRedstoneLinkable#shouldSetMode(Mode)} and/or
	 * {@link AbstractRedstoneLinkable#onModeChanged(Mode)} to change behavior.
	 */
	@Override
	public final void setMode(final Mode newMode) {
		if (this.mode == newMode || !this.shouldSetMode(newMode)) return;
		if (this.network == null) {
			this.mode = newMode;
		} else {
			this.network.remove(this);
			this.mode = newMode;
			this.network.add(this);
		}
		this.onModeChanged(newMode);
	}

	/**
	 * Override {@link AbstractRedstoneLinkable#shouldSetFrequency(boolean, ItemStack)} and/or
	 * {@link AbstractRedstoneLinkable#onFrequencyChanged(boolean, ItemStack)} to change behavior.
	 */
	@Override
	public final void setFrequency(final boolean first, ItemStack stack) {
		stack = stack.copy();
		stack.setCount(1);
		final ItemStack toCompare = first ? this.frequencyFirst.getStack() : this.frequencyLast.getStack();
		if (ItemStack.isSameItemSameComponents(stack, toCompare) || !this.shouldSetFrequency(first, stack)) return;

		if (this.network != null) this.network.remove(this);

		if (first) {
			this.frequencyFirst = RedstoneLinkNetworkHandler.Frequency.of(stack);
		} else {
			this.frequencyLast = RedstoneLinkNetworkHandler.Frequency.of(stack);
		}

		if (this.network != null) this.network.add(this);
		this.onFrequencyChanged(first, stack);
	}

	//These are meant to allow logic for setting Frequency/Mode without needing to reimplement the boilerplate performance/null checks
	protected abstract boolean shouldSetMode(Mode newMode);

	protected abstract boolean shouldSetFrequency(boolean first, ItemStack stack);

	protected abstract void onModeChanged(Mode newMode);

	protected abstract void onFrequencyChanged(boolean first, ItemStack stack);
}
