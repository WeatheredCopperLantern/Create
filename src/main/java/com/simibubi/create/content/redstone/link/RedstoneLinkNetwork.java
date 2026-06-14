package com.simibubi.create.content.redstone.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.interfaces.ICustomReceive;
import com.simibubi.create.content.redstone.link.interfaces.IRedstoneLinkable;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.*;

public class RedstoneLinkNetwork {

	private final Map<Couple<RedstoneLinkNetworkHandler.Frequency>, Couple<Set<IRedstoneLinkable>>> channels = new HashMap<>();

	private final Queue<IRedstoneLinkable> receiverUpdates = new ArrayDeque<>();

	private final HashMap<IRedstoneLinkable, Integer> queuedRemovals = new HashMap<>();

	private final Queue<IRedstoneLinkable> delayedUpdates = new ArrayDeque<>();

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

		while (!delayedUpdates.isEmpty() || !receiverUpdates.isEmpty()) {
			while (!delayedUpdates.isEmpty()) {
				delayedUpdates.remove().delayedUpdate();
			}

			while (!receiverUpdates.isEmpty()) {
				IRedstoneLinkable link = receiverUpdates.remove();
				Set<IRedstoneLinkable> candidates = getChannel(link.getChannelKey()).get(false);

				if (link instanceof ICustomReceive customReceiveLinkable) {
					Set<IRedstoneLinkable> inRange = Collections.newSetFromMap(new IdentityHashMap<>());
					getInRange(link, inRange);
					customReceiveLinkable.calculateSignal(inRange);
					return;
				}

				int maxStrength = 0;
				for (IRedstoneLinkable candidate : candidates) {
					if (!canReceiveFrom(link, candidate)) continue;
					int strength = candidate.getTransmittedStrength();
					if (strength > maxStrength) {
						maxStrength = strength;
						if (strength == 15) {
							break;
						}
					}
				}
				link.setReceivedStrength(maxStrength);
			}
		}
	}

	public void getInRange(IRedstoneLinkable link, Set<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (IRedstoneLinkable link2 : getChannel(link.getChannelKey()).get(false)) {
				if (canReceiveFrom(link, link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (IRedstoneLinkable link2 : getChannel(link.getChannelKey()).get(true)) {
				if (canSendTo(link, link2)) {
					destination.add(link2);
				}
			}
		}
	}

	public void queueInRange(IRedstoneLinkable link, Queue<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (IRedstoneLinkable link2 : getChannel(link.getChannelKey()).get(false)) {
				if (link2.allowQueue() && canReceiveFrom(link, link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (IRedstoneLinkable link2 : getChannel(link.getChannelKey()).get(true)) {
				if (link2.allowQueue() && canSendTo(link, link2)) {
					destination.add(link2);
				}
			}
		}
	}

	public void getInRange(IRedstoneLinkable link, Vec3 posOverride, Set<IRedstoneLinkable> destination) {
		if (link.isListening()) {
			for (IRedstoneLinkable link2 : getChannel(link.getChannelKey()).get(false)) {
				if (canReceiveFrom(posOverride, link.receivingRange(), link2)) {
					destination.add(link2);
				}
			}
		} else {
			for (IRedstoneLinkable link2 : getChannel(link.getChannelKey()).get(true)) {
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

	public void queueDelayedUpdate(IRedstoneLinkable link) {
		delayedUpdates.add(link);
	}

	public void addSilent(IRedstoneLinkable link) {
		if (!FMLEnvironment.production && link.getNetwork() != this) {
			throw new IllegalStateException("This method is meant to be called from the link. If you want to set a links network call link.setNetwork()");
		}
		getChannel(link.getChannelKey()).get(link.isListening()).add(link);
	}

	public void add(IRedstoneLinkable link) {
		if (!FMLEnvironment.production && link.getNetwork() != this) {
			throw new IllegalStateException("This method is meant to be called from the link. If you want to set a links network call link.setNetwork()");
		}
		getChannel(link.getChannelKey()).get(link.isListening()).add(link);
		if (link.isListening() && link.allowQueue()) {
			receiverUpdates.add(link);
		} else if (link.getTransmittedStrength() != 0) {
			queueInRange(link, receiverUpdates);
		}
	}

	public void removeIn(IRedstoneLinkable link, int ticks) {
		queuedRemovals.compute(link, (iRedstoneLinkable, integer) -> (integer == null) ? ticks : Math.max(integer, ticks));
	}

	public void remove(IRedstoneLinkable link) {
		getChannel(link.getChannelKey()).get(link.isListening()).remove(link);
		if (!link.isListening() && link.getTransmittedStrength() != 0) {
			queueInRange(link, receiverUpdates);
		}
	}

	public void signalChanged(IRedstoneLinkable link) {
		queueInRange(link, receiverUpdates);
	}

	public void linkMoved(IRedstoneLinkable link, Vec3 oldPos) {
		RedstoneLinkNetwork newNetwork = Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(link.getLocation(), link.getLevel());
		if (newNetwork != this) {
			link.setNetwork(newNetwork);
		} else if (!link.isListening()) {
			Set<IRedstoneLinkable> oldReceivers = Collections.newSetFromMap(new IdentityHashMap<>());
			Set<IRedstoneLinkable> newReceivers = Collections.newSetFromMap(new IdentityHashMap<>());

			getInRange(link, oldPos, oldReceivers);
			getInRange(link, newReceivers);

			for (IRedstoneLinkable linkable : oldReceivers) {
				if (!newReceivers.contains(linkable)) {
					receiverUpdates.add(linkable);
				}
			}

			for (IRedstoneLinkable linkable : newReceivers) {
				if (!oldReceivers.contains(linkable)) {
					receiverUpdates.add(linkable);
				}
			}
		} else {
			receiverUpdates.add(link);
		}
	}

	public Couple<Set<IRedstoneLinkable>> getChannel(Couple<RedstoneLinkNetworkHandler.Frequency> key) {
		if (!channels.containsKey(key)) channels.put(key, Couple.create(new HashSet<>(), new HashSet<>()));
		return channels.get(key);
	}
}
