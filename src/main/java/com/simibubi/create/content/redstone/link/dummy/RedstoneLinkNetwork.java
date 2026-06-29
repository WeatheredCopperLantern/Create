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
		Iterator<Map.Entry<IRedstoneLinkable, Integer>> iter = queuedRemovals.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<IRedstoneLinkable, Integer> entry = iter.next();
			if (entry.getValue() == 0) {
				remove(entry.getKey());
				iter.remove();
			} else {
				entry.setValue(entry.getValue() - 1);
			}
		}

		while (!queuedReceiverSignals.isEmpty()) {
			Pair<IRedstoneLinkable, Integer> entry = queuedReceiverSignals.remove();
			entry.getFirst().setReceivedStrength(entry.getSecond());
		}

		while (!delayedUpdates.isEmpty()) {
			delayedUpdates.remove().delayedUpdate();
		}

		while (!receiverInNeedOfRecalc.isEmpty()) {
			IRedstoneLinkable link = receiverInNeedOfRecalc.remove();
			Set<IRedstoneLinkable> inRange = Collections.newSetFromMap(new IdentityHashMap<>());
			getInRange(link, inRange);

			if (link instanceof ICustomReceive customReceiveLinkable) {
				customReceiveLinkable.calculateSignal(inRange);
				return;
			}

			int maxStrength = 0;
			for (IRedstoneLinkable candidate : inRange) {
				int strength = candidate.getTransmittedStrength();
				if (strength > maxStrength) {
					maxStrength = strength;
					if (strength == 15) {
						break;
					}
				}
			}
			link.considerDequeued();
			queuedReceiverSignals.add(Couple.of(link, maxStrength));
		}
	}

	public void getInRange(IRedstoneLinkable link, Set<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (IRedstoneLinkable link2 : getChannelMembers(link.getChannelKey()).get(false)) {
				if (canReceiveFrom(link, link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (IRedstoneLinkable link2 : getChannelMembers(link.getChannelKey()).get(true)) {
				if (canSendTo(link, link2)) {
					destination.add(link2);
				}
			}
		}
	}

	public void queueInRange(IRedstoneLinkable link, Queue<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (IRedstoneLinkable link2 : getChannelMembers(link.getChannelKey()).get(false)) {
				if (link2.allowRecalcQueue() && canReceiveFrom(link, link2)) {
					link2.considerQueued();
					destination.add(link2);
				}
			}
		} else {
			for (IRedstoneLinkable link2 : getChannelMembers(link.getChannelKey()).get(true)) {
				if (link2.allowRecalcQueue() && canSendTo(link, link2)) {
					link2.considerQueued();
					destination.add(link2);
				}
			}
		}
	}

	public void getInRange(IRedstoneLinkable link, Vec3 posOverride, Set<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (IRedstoneLinkable link2 : getChannelMembers(link.getChannelKey()).get(false)) {
				if (canReceiveFrom(posOverride, link.receivingRange(), link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (IRedstoneLinkable link2 : getChannelMembers(link.getChannelKey()).get(true)) {
				if (canSendTo(posOverride, link.transmissionRange(), link2)) {
					destination.add(link2);
				}
			}
		}
	}

	public static boolean canReceiveFrom(IRedstoneLinkable destination, IRedstoneLinkable source) {
		return canSendTo(source, destination);
	}

	public static boolean canSendTo(IRedstoneLinkable source, IRedstoneLinkable destination) {
		if (source == destination) return true;
		return source.getLocation().closerThan(destination.getLocation(), Math.min(source.transmissionRange(), destination.receivingRange()));
	}

	public static boolean canReceiveFrom(Vec3 destination, int receiveRange, IRedstoneLinkable source) {
		return source.getLocation().closerThan(destination, Math.min(receiveRange, source.transmissionRange()));
	}

	public static boolean canSendTo(Vec3 source, int transmissionRange, IRedstoneLinkable destination) {
		return source.closerThan(destination.getLocation(), Math.min(transmissionRange, destination.receivingRange()));
	}

	/**
	 * Queues a {@link IRedstoneLinkable} to have {@link IRedstoneLinkable#delayedUpdate()} called at the end of the current tick.
	 *
	 * @see IRedstoneLinkable#delayedUpdate()
	 * @see IRedstoneLinkable#queueUpdate()
	 */
	public void queueDelayedUpdate(IRedstoneLinkable link) {
		delayedUpdates.add(link);
	}

	/**
	 * Adds a {@link IRedstoneLinkable} without updating the Network.
	 * Currently used
	 */
	public void addSilent(IRedstoneLinkable link) {
		if (!FMLEnvironment.production && link.getNetwork() != this) {
			throw new IllegalStateException("This method is meant to be called from the link. If you want to set a links network call link.setNetwork()");
		}
		getChannelMembers(link.getChannelKey()).get(link.isListening()).add(link);
	}

	public void add(IRedstoneLinkable link) {
		if (!FMLEnvironment.production && link.getNetwork() != this) {
			throw new IllegalStateException("This method is meant to be called from the link. If you want to set a links network call link.setNetwork()");
		}
		getChannelMembers(link.getChannelKey()).get(link.isListening()).add(link);
		if (link.isListening() && link.allowRecalcQueue()) {
			link.considerQueued();
			receiverInNeedOfRecalc.add(link);
		} else if (link.getTransmittedStrength() != 0) {
			queueInRange(link, receiverInNeedOfRecalc);
		}
	}

	public void removeIn(IRedstoneLinkable link, int ticks) {
		queuedRemovals.compute(link, (iRedstoneLinkable, integer) -> (integer == null) ? ticks : Math.max(integer, ticks));
	}

	public void remove(IRedstoneLinkable link) {
		getChannelMembers(link.getChannelKey()).get(link.isListening()).remove(link);
		if (!link.isListening() && link.getTransmittedStrength() != 0) {
			queueInRange(link, receiverInNeedOfRecalc);
		}
	}

	public void signalChanged(IRedstoneLinkable link) {
		queueInRange(link, receiverInNeedOfRecalc);
	}

	public void linkMoved(IRedstoneLinkable link, Vec3 oldPos) {
		RedstoneLinkNetwork newNetwork = null;//Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(link.getLocation(), link.getLevel());
		if (newNetwork != this) {
			link.setNetwork(newNetwork);
		} else if (!link.isListening()) {
			Set<IRedstoneLinkable> oldReceivers = Collections.newSetFromMap(new IdentityHashMap<>());
			Set<IRedstoneLinkable> newReceivers = Collections.newSetFromMap(new IdentityHashMap<>());

			getInRange(link, oldPos, oldReceivers);
			getInRange(link, newReceivers);

			for (IRedstoneLinkable linkable : oldReceivers) {
				if (linkable.allowRecalcQueue() && !newReceivers.contains(linkable)) {
					linkable.considerQueued();
					receiverInNeedOfRecalc.add(linkable);
				}
			}

			for (IRedstoneLinkable linkable : newReceivers) {
				if (linkable.allowRecalcQueue() && !oldReceivers.contains(linkable)) {
					linkable.considerQueued();
					receiverInNeedOfRecalc.add(linkable);
				}
			}
		} else if (link.allowRecalcQueue()) {
			link.considerQueued();
			receiverInNeedOfRecalc.add(link);
		}
	}

	public Couple<Set<IRedstoneLinkable>> getChannelMembers(Couple<RedstoneLinkNetworkHandler.Frequency> key) {
		if (!channels.containsKey(key)) channels.put(key, Couple.create(new HashSet<>(), new HashSet<>()));
		return channels.get(key);
	}
}
