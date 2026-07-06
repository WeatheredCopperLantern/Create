package com.simibubi.create.content.redstone.link.dummy;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.simibubi.create.content.redstone.link.dummy.interfaces.ICustomReceive;
import com.simibubi.create.content.redstone.link.dummy.interfaces.IRedstoneLinkable;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;

import net.minecraft.world.phys.Vec3;

import net.neoforged.fml.loading.FMLEnvironment;

public class RedstoneLinkNetwork {

	private final Map<Couple<RedstoneLinkNetworkHandler.Frequency>, Couple<Set<IRedstoneLinkable>>> channels = new HashMap<>();

	/**
	 * @see RedstoneLinkNetwork#queueDelayedUpdate(IRedstoneLinkable)
	 */
	private final Queue<IRedstoneLinkable> delayedUpdates = new ArrayDeque<>();

	// Stores all receivers that need the receiving signal recalculated
	private final Queue<IRedstoneLinkable> receiverInNeedOfRecalc = new ArrayDeque<>();

	// Store the Signal to apply it to receivers in the next tick.
	// This prevents cascading signal changing and allows to keep the tick function simpler.
	// This is also how RedstoneLinks worked already.
	private final Queue<Pair<IRedstoneLinkable, Integer>> queuedReceiverSignals = new ArrayDeque<>();

	private final HashMap<IRedstoneLinkable, Integer> queuedRemovals = new HashMap<>();

	public void tick() {
		final Iterator<Map.Entry<IRedstoneLinkable, Integer>> iter = this.queuedRemovals.entrySet().iterator();
		while (iter.hasNext()) {
			final Map.Entry<IRedstoneLinkable, Integer> entry = iter.next();
			if (entry.getValue() == 0) {
				this.remove(entry.getKey());
				iter.remove();
			} else {
				entry.setValue(entry.getValue() - 1);
			}
		}

		while (!this.queuedReceiverSignals.isEmpty()) {
			final Pair<IRedstoneLinkable, Integer> entry = this.queuedReceiverSignals.remove();
			entry.getFirst().setReceivedStrength(entry.getSecond());
		}

		while (!this.delayedUpdates.isEmpty()) {
			this.delayedUpdates.remove().delayedUpdate();
		}

		while (!this.receiverInNeedOfRecalc.isEmpty()) {
			final IRedstoneLinkable link = this.receiverInNeedOfRecalc.remove();
			final Set<IRedstoneLinkable> inRange = Collections.newSetFromMap(new IdentityHashMap<>());
			this.getInRange(link, inRange);

			if (link instanceof final ICustomReceive customReceiveLinkable) {
				customReceiveLinkable.calculateSignal(inRange);
				return;
			}

			int maxStrength = 0;
			for (final IRedstoneLinkable candidate : inRange) {
				final int strength = candidate.getTransmittedStrength();
				if (strength > maxStrength) {
					maxStrength = strength;
					if (strength == 15) {
						break;
					}
				}
			}
			link.considerDequeued();
			this.queuedReceiverSignals.add(Pair.of(link, maxStrength));
		}
	}

	public void getInRange(final IRedstoneLinkable link, final Set<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (final IRedstoneLinkable link2 : this.getChannelMembers(link.getChannelKey()).get(false)) {
				if (RedstoneLinkNetwork.canReceiveFrom(link, link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (final IRedstoneLinkable link2 : this.getChannelMembers(link.getChannelKey()).get(true)) {
				if (RedstoneLinkNetwork.canSendTo(link, link2)) {
					destination.add(link2);
				}
			}
		}
	}

	public void queueInRange(final IRedstoneLinkable link, final Queue<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (final IRedstoneLinkable link2 : this.getChannelMembers(link.getChannelKey()).get(false)) {
				if (link2.allowRecalcQueue() && RedstoneLinkNetwork.canReceiveFrom(link, link2)) {
					link2.considerQueued();
					destination.add(link2);
				}
			}
		} else {
			for (final IRedstoneLinkable link2 : this.getChannelMembers(link.getChannelKey()).get(true)) {
				if (link2.allowRecalcQueue() && RedstoneLinkNetwork.canSendTo(link, link2)) {
					link2.considerQueued();
					destination.add(link2);
				}
			}
		}
	}

	public void getInRange(final IRedstoneLinkable link, final Vec3 posOverride, final Set<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (final IRedstoneLinkable link2 : this.getChannelMembers(link.getChannelKey()).get(false)) {
				if (RedstoneLinkNetwork.canReceiveFrom(posOverride, link.receivingRange(), link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (final IRedstoneLinkable link2 : this.getChannelMembers(link.getChannelKey()).get(true)) {
				if (RedstoneLinkNetwork.canSendTo(posOverride, link.transmissionRange(), link2)) {
					destination.add(link2);
				}
			}
		}
	}

	public static boolean canReceiveFrom(final IRedstoneLinkable destination, final IRedstoneLinkable source) {
		return RedstoneLinkNetwork.canSendTo(source, destination);
	}

	public static boolean canSendTo(final IRedstoneLinkable source, final IRedstoneLinkable destination) {
		if (source == destination) return true;
		return source.getLocation().closerThan(destination.getLocation(), Math.min(source.transmissionRange(), destination.receivingRange()));
	}

	public static boolean canReceiveFrom(final Vec3 destination, final int receiveRange, final IRedstoneLinkable source) {
		return source.getLocation().closerThan(destination, Math.min(receiveRange, source.transmissionRange()));
	}

	public static boolean canSendTo(final Vec3 source, final int transmissionRange, final IRedstoneLinkable destination) {
		return source.closerThan(destination.getLocation(), Math.min(transmissionRange, destination.receivingRange()));
	}

	/**
	 * Queues a {@link IRedstoneLinkable} to have {@link IRedstoneLinkable#delayedUpdate()} called at the end of the current tick.
	 *
	 * @see IRedstoneLinkable#delayedUpdate()
	 * @see IRedstoneLinkable#queueUpdate()
	 */
	public void queueDelayedUpdate(final IRedstoneLinkable link) {
		this.delayedUpdates.add(link);
	}

	/**
	 * Adds a {@link IRedstoneLinkable} without updating the Network.
	 * Currently used
	 */
	public void addSilent(final IRedstoneLinkable link) {
		if (!FMLEnvironment.production && link.getNetwork() != this) {
			throw new IllegalStateException("This method is meant to be called from the link. If you want to set a links network call link.setNetwork()");
		}
		this.getChannelMembers(link.getChannelKey()).get(link.isListening()).add(link);
	}

	public void add(final IRedstoneLinkable link) {
		if (!FMLEnvironment.production && link.getNetwork() != this) {
			throw new IllegalStateException("This method is meant to be called from the link. If you want to set a links network call link.setNetwork()");
		}
		this.getChannelMembers(link.getChannelKey()).get(link.isListening()).add(link);
		if (link.isListening() && link.allowRecalcQueue()) {
			link.considerQueued();
			this.receiverInNeedOfRecalc.add(link);
		} else if (link.getTransmittedStrength() != 0) {
			this.queueInRange(link, this.receiverInNeedOfRecalc);
		}
	}

	public void removeIn(final IRedstoneLinkable link, final int ticks) {
		this.queuedRemovals.compute(link, (iRedstoneLinkable, integer) -> (integer == null) ? ticks : Math.max(integer, ticks));
	}

	public void remove(final IRedstoneLinkable link) {
		this.getChannelMembers(link.getChannelKey()).get(link.isListening()).remove(link);
		if (!link.isListening() && link.getTransmittedStrength() != 0) {
			this.queueInRange(link, this.receiverInNeedOfRecalc);
		}
	}

	public void signalChanged(final IRedstoneLinkable link) {
		this.queueInRange(link, this.receiverInNeedOfRecalc);
	}

	public void linkMoved(final IRedstoneLinkable link, final Vec3 oldPos) {
		final RedstoneLinkNetwork newNetwork = null;//Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(link.getLocation(), link.getLevel());
		if (newNetwork != this) {
			link.setNetwork(newNetwork);
		} else if (!link.isListening()) {
			final Set<IRedstoneLinkable> oldReceivers = Collections.newSetFromMap(new IdentityHashMap<>());
			final Set<IRedstoneLinkable> newReceivers = Collections.newSetFromMap(new IdentityHashMap<>());

			this.getInRange(link, oldPos, oldReceivers);
			this.getInRange(link, newReceivers);

			for (final IRedstoneLinkable linkable : oldReceivers) {
				if (linkable.allowRecalcQueue() && !newReceivers.contains(linkable)) {
					linkable.considerQueued();
					this.receiverInNeedOfRecalc.add(linkable);
				}
			}

			for (final IRedstoneLinkable linkable : newReceivers) {
				if (linkable.allowRecalcQueue() && !oldReceivers.contains(linkable)) {
					linkable.considerQueued();
					this.receiverInNeedOfRecalc.add(linkable);
				}
			}
		} else if (link.allowRecalcQueue()) {
			link.considerQueued();
			this.receiverInNeedOfRecalc.add(link);
		}
	}

	public Couple<Set<IRedstoneLinkable>> getChannelMembers(final Couple<RedstoneLinkNetworkHandler.Frequency> key) {
		if (!this.channels.containsKey(key)) this.channels.put(key, Couple.create(new HashSet<>(), new HashSet<>()));
		return this.channels.get(key);
	}
}
