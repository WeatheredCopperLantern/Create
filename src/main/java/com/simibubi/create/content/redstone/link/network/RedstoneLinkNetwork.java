package com.simibubi.create.content.redstone.link.network;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.interfaces.ICustomReceive;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableSnapshot;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.ImmutableCouple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.nbt.NBTHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.Contract;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkNetwork {

	// region Sets & Lists ===========================================================
	private final Map<ImmutableCouple<Frequency>, Couple<Set<IRedstoneLinkable>>> channels;

	private final Queue<IRedstoneLinkable> recalcQueue = new ArrayDeque<>(16);
	private final Queue<Pair<IRedstoneLinkable, Integer>> queuedReceiverSignals = new ArrayDeque<>(16);
	private final Map<IRedstoneLinkable, Integer> queuedRemovals = HashMap.newHashMap(16);

	//endregion

	public ServerLevel level;

	// region Saving/Loading =========================================================
	public CompoundTag write(final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		final CompoundTag tag = new CompoundTag(1);

		final ListTag listTag = new ListTag();
		for (final IRedstoneLinkable linkable : this.recalcQueue) {
			listTag.add(NbtUtils.createUUID(linkable.getUUID()));
		}
		tag.put("Recalc", listTag);

		tag.put("QueuedSignals", NBTHelper.writeCompoundList(this.queuedReceiverSignals, IRedstoneLinkableIntegerPair -> {
			final CompoundTag compoundTag = new CompoundTag(2);
			compoundTag.putUUID("UUID", IRedstoneLinkableIntegerPair.getFirst().getUUID());
			compoundTag.putInt("Strength", IRedstoneLinkableIntegerPair.getSecond());
			return compoundTag;
		}));

		tag.put("QueuedRemovals", NBTHelper.writeCompoundList(this.queuedRemovals.entrySet(), IRedstoneLinkableIntegerEntry -> {
			final CompoundTag compoundTag = new CompoundTag(2);
			compoundTag.putUUID("UUID", IRedstoneLinkableIntegerEntry.getKey().getUUID());
			compoundTag.putInt("Time", IRedstoneLinkableIntegerEntry.getValue());
			return compoundTag;
		}));

		return tag;
	}

	public RedstoneLinkNetwork(final ServerLevel level) {
		this.channels = new HashMap<>(32, 0.7f);
		this.level = level;
	}

	//endregion

	public void tick() {
		//Handle queuedRemovals
		final Iterator<Map.Entry<IRedstoneLinkable, Integer>> iter = this.queuedRemovals.entrySet().iterator();
		while (iter.hasNext()) {
			final Map.Entry<IRedstoneLinkable, Integer> entry = iter.next();
			if (entry.getValue() <= 0) {
				this.removeLinkable(entry.getKey());
				iter.remove();
			} else {
				entry.setValue(entry.getValue() - 1);
			}
		}

		//Handle queuedReceiverSignals
		while (!this.queuedReceiverSignals.isEmpty()) {
			final Pair<IRedstoneLinkable, Integer> entry = this.queuedReceiverSignals.remove();
			final IRedstoneLinkable linkable = entry.getFirst();
			if (linkable.isReceiver()) {
				linkable.setSignal(entry.getSecond());
			}
		}

		//Handle recalcQueue
		while (!this.recalcQueue.isEmpty()) {
			final IRedstoneLinkable linkable = this.recalcQueue.remove();
			final Set<IRedstoneLinkable> inRange = HashSet.newHashSet(16);
			this.forLinkableInRange(linkable, IRedstoneLinkable -> true, inRange::add);
			linkable.considerRecalcDequeued();

			if (linkable instanceof final ICustomReceive customReceiveLinkable) {
				customReceiveLinkable.calculateSignal(inRange);
				return;
			}

			int maxStrength = 0;
			for (final IRedstoneLinkable candidate : inRange) {
				final int strength = candidate.getSignal();
				if (strength > maxStrength) {
					maxStrength = strength;
					if (strength == 15) {
						break;
					}
				}
			}
			this.queuedReceiverSignals.add(Pair.of(linkable, maxStrength));
		}
	}

	public void addLinkable(final IRedstoneLinkable linkable) {
		this.getChannel(linkable.getChannel()).get(linkable.isReceiver()).add(linkable);
		Create.REDSTONE_LINK_NETWORK.linkables.put(linkable.getUUID(), linkable);
		Create.REDSTONE_LINK_NETWORK.setDirty();
		if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		} else if (linkable.getSignal() > 0) {
			this.forLinkableInRange(linkable, IRedstoneLinkable::allowRecalcQueue, linkable1 -> {
				linkable1.considerRecalcQueued();
				this.recalcQueue.add(linkable1);
			});
		}
	}

	public void removeLinkable(final IRedstoneLinkable linkable) {
		this.getChannel(linkable.getChannel()).get(linkable.isReceiver()).remove(linkable);
		Create.REDSTONE_LINK_NETWORK.linkables.remove(linkable.getUUID());
		Create.REDSTONE_LINK_NETWORK.setDirty();
		if (linkable.isTransmitter() && linkable.getSignal() > 0) {
			this.forLinkableInRange(linkable, IRedstoneLinkable::allowRecalcQueue, linkable1 -> {
				linkable1.considerRecalcQueued();
				this.recalcQueue.add(linkable1);
			});
		}
	}

	public void removeLinkableIn(final IRedstoneLinkable linkable, final int ticks) {
		this.queuedRemovals.compute(linkable, (IRedstoneLinkable, integer) -> (integer == null) ? ticks : Math.max(integer, ticks));
	}

	public void forLinkableInRange(final RedstoneLinkableSnapshot previous, final Predicate<IRedstoneLinkable> check, final Consumer<IRedstoneLinkable> action) {
		for (final IRedstoneLinkable linkable2 : this.getChannel(previous.channel()).get(!previous.receiver())) {
			if (RedstoneLinkNetwork.canCommunicate(previous, linkable2) && check.test(linkable2)) {
				action.accept(linkable2);
			}
		}
	}

	public void forLinkableInRange(final IRedstoneLinkable linkable, final Predicate<IRedstoneLinkable> check, final Consumer<IRedstoneLinkable> action) {
		for (final IRedstoneLinkable linkable2 : this.getChannel(linkable.getChannel()).get(linkable.isTransmitter())) {
			if (RedstoneLinkNetwork.canCommunicate(linkable, linkable2) && check.test(linkable2)) {
				action.accept(linkable2);
			}
		}
	}

	public void channelChanged(final IRedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		this.getChannel(snapshot.channel()).get(linkable.isReceiver()).remove(linkable);
		this.getChannel(linkable.getChannel()).get(linkable.isReceiver()).add(linkable);
		Create.REDSTONE_LINK_NETWORK.setDirty();

		if (linkable.isTransmitter() && snapshot.signal() > 0) {
			this.forLinkableInRange(snapshot, IRedstoneLinkable::allowRecalcQueue, this.recalcQueue::add);
			this.forLinkableInRange(linkable, IRedstoneLinkable::allowRecalcQueue, this.recalcQueue::add);
		} else if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		}
	}

	public void modeChanged(final IRedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		this.getChannel(linkable.getChannel()).get(snapshot.receiver()).remove(linkable);
		this.getChannel(linkable.getChannel()).get(linkable.isReceiver()).add(linkable);
		Create.REDSTONE_LINK_NETWORK.setDirty();

		if (!snapshot.receiver() && snapshot.signal() > 0) {
			this.forLinkableInRange(snapshot, IRedstoneLinkable::allowRecalcQueue, linkable1 -> {
				linkable1.considerRecalcQueued();
				this.recalcQueue.add(linkable1);
			});
		}

		if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		}
	}

	public void signalChanged(final IRedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		Create.REDSTONE_LINK_NETWORK.setDirty();
		this.forLinkableInRange(linkable, linkable1 -> {
			if (!linkable1.allowRecalcQueue()) return false;
			if (linkable1 instanceof ICustomReceive) return true;
			final int currentStrength = linkable1.getSignal();
			return (currentStrength == snapshot.signal() || currentStrength < linkable.getSignal());
		}, linkable1 -> {
			linkable1.considerRecalcQueued();
			this.recalcQueue.add(linkable1);
		});
	}

	public void linkMoved(final IRedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		Create.REDSTONE_LINK_NETWORK.setDirty();
		if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		} else if (linkable.isTransmitter()) {
			final Collection<IRedstoneLinkable> oldReceivers = new HashSet<>(23 /* Allows 16 entries before resize */, 0.7f);
			final Collection<IRedstoneLinkable> newReceivers = new HashSet<>(23 /* Allows 16 entries before resize */, 0.7f);

			this.forLinkableInRange(snapshot, IRedstoneLinkable::allowRecalcQueue, oldReceivers::add);
			this.forLinkableInRange(linkable, IRedstoneLinkable::allowRecalcQueue, newReceivers::add);

			for (final IRedstoneLinkable link : oldReceivers) {
				if (!newReceivers.contains(link)) {
					link.considerRecalcQueued();
					this.recalcQueue.add(link);
				}
			}

			for (final IRedstoneLinkable link : newReceivers) {
				if (!oldReceivers.contains(link)) {
					link.considerRecalcQueued();
					this.recalcQueue.add(link);
				}
			}
		}
	}

	/**
	 * <p>Does <b style="color:red">NOT</b> check if the linkables are actually receiver + transmitter.</p>
	 * <p>Instead assumes that linkable2 is the opposite of linkable1.</p>
	 */
	@Contract(pure = true)
	public static boolean canCommunicate(final IRedstoneLinkable linkable1, final IRedstoneLinkable linkable2) {
		if (linkable1.isReceiver()) {
			return linkable1.getReceivingPosition().distanceSquared(linkable2.getTransmissionPosition()) <= Math.pow(Math.min(linkable1.getReceivingRange(), linkable1.getTransmissionRange()), 2);
		} else {
			return linkable1.getTransmissionPosition().distanceSquared(linkable2.getReceivingPosition()) <= Math.pow(Math.min(linkable1.getTransmissionRange(), linkable1.getReceivingRange()), 2);
		}
	}

	/**
	 * <p>Does <b style="color:red">NOT</b> check if the linkables are actually receiver + transmitter.</p>
	 * <p>Instead assumes that linkable2 is the opposite of linkable1.</p>
	 */
	@Contract(pure = true)
	public static boolean canCommunicate(final RedstoneLinkableSnapshot linkable1, final IRedstoneLinkable linkable2) {
		if (linkable1.receiver()) {
			return linkable1.receivingPosition().distanceSquared(linkable2.getTransmissionPosition()) <= Math.pow(Math.min(linkable1.receivingRange(), linkable1.transmissionRange()), 2);
		} else {
			return linkable1.transmissionPosition().distanceSquared(linkable2.getReceivingPosition()) <= Math.pow(Math.min(linkable1.transmissionRange(), linkable1.receivingRange()), 2);
		}
	}

	@Contract(pure = true)
	public Couple<Set<IRedstoneLinkable>> getChannel(final ImmutableCouple<Frequency> key) {
		return this.channels.computeIfAbsent(key, frequencies -> Couple.create(new HashSet<>(12, 0.7f), new HashSet<>(12, 0.7f)));
	}
}
