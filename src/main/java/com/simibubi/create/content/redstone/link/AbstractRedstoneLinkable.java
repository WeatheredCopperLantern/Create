package com.simibubi.create.content.redstone.link;

import com.simibubi.create.content.redstone.link.interfaces.IRedstoneLinkable;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public abstract class AbstractRedstoneLinkable implements IRedstoneLinkable {

	private Frequency frequencyFirst;
	private Frequency frequencyLast;
	private Mode mode;
	protected IntSupplier transmission;
	protected IntConsumer signalCallback;
	private RedstoneLinkNetwork network;

	private boolean queued = false;

	public AbstractRedstoneLinkable(Couple<Frequency> channel, Mode mode, IntConsumer signalCallback, IntSupplier transmission) {
		this(channel.getFirst(), channel.getSecond(), mode, signalCallback, transmission);
	}

	public AbstractRedstoneLinkable(Frequency first, Frequency last, Mode mode, IntConsumer signalCallback, IntSupplier transmission) {
		frequencyFirst = first;
		frequencyLast = last;
		this.signalCallback = signalCallback;
		this.transmission = transmission;
		this.mode = mode;
	}

	@Override
	public void queueUpdate() {
		if (!queued && getNetwork() != null) {
			queued = true;
			getNetwork().queueDelayedUpdate(this);
		}
	}

	@Override
	public boolean allowQueue() {
		return !queued;
	}

	@Override
	public void delayedUpdate() {
		queued = false;
	}

	@Override
	public int receivingRange() {
		return AllConfigs.server().logistics.linkRange.get();
	}

	@Override
	public int transmissionRange() {
		return AllConfigs.server().logistics.linkRange.get();
	}

	public final void notifySignalChange() {
		if (network == null) return;
		network.signalChanged(this);
	}

	@Override
	public final int getTransmittedStrength() {
		return transmission.getAsInt();
	}

	@Override
	public final void setReceivedStrength(int power) {
		queued = false;
		signalCallback.accept(power);
	}

	@Override
	public final boolean isListening() {
		return mode == Mode.RECEIVE;
	}

	@Override
	public final Couple<Frequency> getChannelKey() {
		return Couple.create(frequencyFirst, frequencyLast);
	}

	@Override
	public final void setNetwork(RedstoneLinkNetwork newNetwork) {
		if (newNetwork == network) return;
		if (network == null && isListening()) {
			network = newNetwork;
			network.addSilent(this);
		} else {
			if (network != null) network.remove(this);
			network = newNetwork;
			network.add(this);
		}
	}

	@Override
	public final void clearNetwork(int inTicks) {
		if (network != null) network.removeIn(this, inTicks);
		network = null;
	}

	@Override
	public final void clearNetwork() {
		if (network != null) network.remove(this);
		network = null;
	}

	@Override
	public final RedstoneLinkNetwork getNetwork() {
		return network;
	}

	/**
	 * Override {@link com.simibubi.create.content.redstone.link.AbstractRedstoneLinkable#shouldSetMode(Mode)} and/or
	 * {@link com.simibubi.create.content.redstone.link.AbstractRedstoneLinkable#onModeChanged(Mode)} to change behavior.
	 */
	@Override
	public final void setMode(Mode newMode) {
		if (mode == newMode || !shouldSetMode(newMode)) return;
		if (network == null) {
			mode = newMode;
		} else {
			network.remove(this);
			mode = newMode;
			network.add(this);
		}
		onModeChanged(newMode);
	}

	/**
	 * Override {@link com.simibubi.create.content.redstone.link.AbstractRedstoneLinkable#shouldSetFrequency(boolean, ItemStack)} and/or
	 * {@link com.simibubi.create.content.redstone.link.AbstractRedstoneLinkable#onFrequencyChanged(boolean, ItemStack)} to change behavior.
	 */
	@Override
	public final void setFrequency(boolean first, ItemStack stack) {
		stack = stack.copy();
		stack.setCount(1);
		ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
		if (ItemStack.isSameItemSameComponents(stack, toCompare) || !shouldSetFrequency(first, stack)) return;

		if (network != null) network.remove(this);

		if (first) frequencyFirst = Frequency.of(stack);
		else frequencyLast = Frequency.of(stack);

		if (network != null) network.add(this);
		onFrequencyChanged(first, stack);
	}

	//These are meant to allow logic for setting Frequency/Mode without needing to reimplement the boilerplate performance/null checks
	protected abstract boolean shouldSetMode(Mode newMode);

	protected abstract boolean shouldSetFrequency(boolean first, ItemStack stack);

	protected abstract void onModeChanged(Mode newMode);

	protected abstract void onFrequencyChanged(boolean first, ItemStack stack);
}
