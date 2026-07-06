package com.simibubi.create.content.redstone.link;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.link.interfaces.ICustomReceive;
import com.simibubi.create.content.redstone.link.interfaces.ITickingLinkable;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.nbt.NBTHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public class RedstoneLinkNetwork {

	// region Sets & Lists ===========================================================
	private Map<Couple<Frequency>, Couple<Set<RedstoneLinkable>>> channels;
	private final Collection<ITickingLinkable> tickingLinkables = new HashSet<>(23 /* Allows 16 entries before resize */, 0.7f);
	private final Queue<RedstoneLinkable> updates = new ArrayDeque<>(16);
	private final Queue<RedstoneLinkable> recalcQueue = new ArrayDeque<>(16);
	private final Queue<Pair<RedstoneLinkable, Integer>> queuedReceiverSignals = new ArrayDeque<>(16);
	private final Map<RedstoneLinkable, Integer> queuedRemovals = new HashMap<>(23 /* Allows 16 entries before resize */, 0.7f);
	//endregion

	public ServerLevel level;

	// region Saving/Loading =========================================================
	public CompoundTag write(final HolderLookup.Provider registries, final DimensionPalette dimensions) {
		final CompoundTag nbt = new CompoundTag(1);

		nbt.put("Channels", NBTHelper.writeCompoundList(this.channels.entrySet(), entry -> {
			final CompoundTag channelNBT = new CompoundTag(4);

			final Couple<Frequency> channel = entry.getKey();
			final Couple<Set<RedstoneLinkable>> linkables = entry.getValue();

			if (linkables.both(Set::isEmpty)) return null;

			channel.forEachWithParams((frequency, name) -> channelNBT.put(name, frequency.write()), Couple.create("Frequency_1", "Frequency_2"));

			linkables.forEachWithParams((redstoneLinkables, name) -> channelNBT.put(name, NBTHelper.writeCompoundList(redstoneLinkables, linkable -> {
				if (!linkable.doSave()) return null;

				final ResourceLocation resourceLocation = CreateBuiltInRegistries.REDSTONE_LINKABLE.getKey(linkable.getType());
				if (resourceLocation == null) {
					Create.LOGGER.error("{} is not a registered RedstoneLinkable, it will NOT be saved. Overwrite RedstoneLinkable#doSave() if this is wanted.", linkable.getClass().getName());
					return null;
				}

				final CompoundTag linkableNBT = linkable.write(registries, dimensions);
				NBTHelper.writeResourceLocation(linkableNBT, "ResourceLocation", resourceLocation);

				return linkableNBT;
			})), Couple.create("Receivers", "Transmitters"));

			return channelNBT;
		}));

		final ListTag tag = new ListTag();
		for (final RedstoneLinkable linkable : this.updates) {
			tag.add(NbtUtils.createUUID(linkable.uuid));
		}
		nbt.put("Updates", tag);

		tag.clear();
		for (final RedstoneLinkable linkable : this.recalcQueue) {
			tag.add(NbtUtils.createUUID(linkable.uuid));
		}
		nbt.put("Recalc", tag);

		nbt.put("QueuedSignals", NBTHelper.writeCompoundList(this.queuedReceiverSignals, redstoneLinkableIntegerPair -> {
			final CompoundTag compoundTag = new CompoundTag(2);
			compoundTag.putUUID("uuid", redstoneLinkableIntegerPair.getFirst().uuid);
			compoundTag.putInt("strength", redstoneLinkableIntegerPair.getSecond());
			return compoundTag;
		}));

		nbt.put("QueuedRemovals", NBTHelper.writeCompoundList(this.queuedRemovals.entrySet(), redstoneLinkableIntegerEntry -> {
			final CompoundTag compoundTag = new CompoundTag(2);
			compoundTag.putUUID("uuid", redstoneLinkableIntegerEntry.getKey().uuid);
			compoundTag.putInt("time", redstoneLinkableIntegerEntry.getValue());
			return compoundTag;
		}));

		return nbt;
	}

	public static RedstoneLinkNetwork read(final CompoundTag nbt, final HolderLookup.Provider registries, final DimensionPalette dimensions, final Map<UUID, RedstoneLinkable> linkables, final ServerLevel level) {
		final RedstoneLinkNetwork network = new RedstoneLinkNetwork();
		final ListTag channelsTag = nbt.getList("Channels", Tag.TAG_COMPOUND);
		final Map<Couple<Frequency>, Couple<Set<RedstoneLinkable>>> channels = new HashMap<>((int) Math.ceil(channelsTag.size() / 0.7f), 0.7f);

		NBTHelper.iterateCompoundList(channelsTag, channelNBT -> {
			final Couple<Frequency> channel = Couple.createWithContext(first -> Frequency.read(channelNBT.getCompound(first ? "Frequency_1" : "Frequency_2"), registries));

			final Couple<Set<RedstoneLinkable>> sets = Couple.createWithContext(aBoolean -> {
				final ListTag linkablesTag = channelNBT.getList(aBoolean ? "Receivers" : "Transmitters", Tag.TAG_COMPOUND);
				final Set<RedstoneLinkable> set = new HashSet<>((int) Math.ceil(linkablesTag.size() / 0.7f), 0.7f);

				NBTHelper.iterateCompoundList(linkablesTag, linkableNBT -> {
					final RedstoneLinkableType linkType = CreateBuiltInRegistries.REDSTONE_LINKABLE.get(NBTHelper.readResourceLocation(linkableNBT, "ResourceLocation"));
					assert linkType != null;
					final RedstoneLinkable link = linkType.factory().apply(linkableNBT, channel.copy(), aBoolean, registries, dimensions, network);
					set.add(link);
					linkables.put(link.uuid, link);
				});
				return set;
			});

			channels.put(channel, sets);
		});

		ListTag tag = nbt.getList("Updates", Tag.TAG_INT_ARRAY);
		tag.forEach(tag1 -> network.updates.add(linkables.get(NbtUtils.loadUUID(tag1))));

		tag = nbt.getList("Recalc", Tag.TAG_INT_ARRAY);
		tag.forEach(tag1 -> network.recalcQueue.add(linkables.get(NbtUtils.loadUUID(tag1))));

		NBTHelper.iterateCompoundList(nbt.getList("QueuedSignals", Tag.TAG_COMPOUND), compoundTag -> {
			final int strength = compoundTag.getInt("strength");
			network.queuedReceiverSignals.add(Pair.of(linkables.get(compoundTag.getUUID("uuid")), strength));
		});

		NBTHelper.iterateCompoundList(nbt.getList("QueuedRemovals", Tag.TAG_COMPOUND), compoundTag -> {
			final int time = compoundTag.getInt("time");
			network.queuedRemovals.put(linkables.get(compoundTag.getUUID("uuid")), time);
		});

		network.channels = channels;
		network.level = level;
		return network;
	}

	public RedstoneLinkNetwork(final ServerLevel level) {
		this.channels = new HashMap<>(32, 0.7f);
		this.level = level;
	}

	private RedstoneLinkNetwork() {

	}

	//endregion

	public void tick() {
		//Tick + Handle queuedRemovals
		final Iterator<Map.Entry<RedstoneLinkable, Integer>> iter = this.queuedRemovals.entrySet().iterator();
		while (iter.hasNext()) {
			final Map.Entry<RedstoneLinkable, Integer> entry = iter.next();
			if (entry.getValue() == 0) {
				this.removeLinkable(entry.getKey());
				iter.remove();
			} else {
				entry.setValue(entry.getValue() - 1);
			}
		}

		//Handle queuedReceiverSignals
		while (!this.queuedReceiverSignals.isEmpty()) {
			final Pair<RedstoneLinkable, Integer> entry = this.queuedReceiverSignals.remove();
			final RedstoneLinkable linkable = entry.getFirst();
			if (linkable.isReceiver()) {
				linkable.setReceivedStrength(entry.getSecond());
			}
		}

		//Handle updates
		while (!this.updates.isEmpty()) {
			this.updates.remove().delayedUpdate();
		}

		//Handle recalcQueue
		while (!this.recalcQueue.isEmpty()) {
			final RedstoneLinkable linkable = this.recalcQueue.remove();
			final Set<RedstoneLinkable> inRange = new HashSet<>(23 /* Allows 16 entries before resize */, 0.7f);
			this.forLinkableInRange(linkable, redstoneLinkable -> true, inRange::add);

			if (linkable instanceof final ICustomReceive customReceiveLinkable) {
				customReceiveLinkable.calculateSignal(inRange);
				return;
			}

			int maxStrength = 0;
			for (final RedstoneLinkable candidate : inRange) {
				final int strength = candidate.getTransmittedStrength();
				if (strength > maxStrength) {
					maxStrength = strength;
					if (strength == 15) {
						break;
					}
				}
			}
			linkable.considerRecalcDequeued();
			this.queuedReceiverSignals.add(Pair.of(linkable, maxStrength));
		}

		//Tick ITickingLinkables
		this.tickingLinkables.forEach(ITickingLinkable::tick);
	}

	public void addLinkable(final @NonNull RedstoneLinkable linkable) {
		this.getChannel(linkable.channel).get(linkable.isReceiver()).add(linkable);
		if (linkable instanceof final ITickingLinkable tickingLinkable) {
			this.tickingLinkables.add(tickingLinkable);
		}
		Create.REDSTONE_LINK_NETWORK.linkables.put(linkable.uuid, linkable);
		Create.REDSTONE_LINK_NETWORK.setDirty();
		if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		} else if (linkable.getTransmittedStrength() > 0) {
			this.forLinkableInRange(linkable, RedstoneLinkable::allowRecalcQueue, linkable1 -> {
				linkable1.considerRecalcQueued();
				this.recalcQueue.add(linkable1);
			});
		}
	}

	public void removeLinkable(final @NonNull RedstoneLinkable linkable) {
		this.getChannel(linkable.channel).get(linkable.isReceiver()).remove(linkable);
		Create.REDSTONE_LINK_NETWORK.linkables.remove(linkable.uuid);
		Create.REDSTONE_LINK_NETWORK.setDirty();
		if (linkable.isTransmitter() && linkable.getTransmittedStrength() > 0) {
			this.forLinkableInRange(linkable, RedstoneLinkable::allowRecalcQueue, linkable1 -> {
				linkable1.considerRecalcQueued();
				this.recalcQueue.add(linkable1);
			});
		}
	}

	public void removeIn(final @NonNull RedstoneLinkable linkable, final int ticks) {
		this.queuedRemovals.compute(linkable, (redstoneLinkable, integer) -> (integer == null) ? ticks : Math.max(integer, ticks));
	}

	public void queueUpdate(final @NonNull RedstoneLinkable linkable) {
		this.updates.add(linkable);
	}

	public void forLinkableInRange(final @NonNull RedstoneLinkableSnapshot previous, final Predicate<RedstoneLinkable> check, final Consumer<RedstoneLinkable> action) {
		for (final RedstoneLinkable linkable2 : this.getChannel(previous.channel()).get(!previous.receiver())) {
			if (RedstoneLinkNetwork.canCommunicate(previous, linkable2) && check.test(linkable2)) {
				action.accept(linkable2);
			}
		}
	}

	public void forLinkableInRange(final @NonNull RedstoneLinkable linkable, final Predicate<RedstoneLinkable> check, final Consumer<RedstoneLinkable> action) {
		for (final RedstoneLinkable linkable2 : this.getChannel(linkable.channel).get(linkable.isTransmitter())) {
			if (RedstoneLinkNetwork.canCommunicate(linkable, linkable2) && check.test(linkable2)) {
				action.accept(linkable2);
			}
		}
	}

	public void channelChanged(final @NonNull RedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		this.getChannel(snapshot.channel()).get(linkable.isReceiver()).remove(linkable);
		this.getChannel(linkable.channel).get(linkable.isReceiver()).add(linkable);
		Create.REDSTONE_LINK_NETWORK.setDirty();

		if (linkable.isTransmitter() && snapshot.signal() > 0) {
			this.forLinkableInRange(snapshot, RedstoneLinkable::allowRecalcQueue, this.recalcQueue::add);
			this.forLinkableInRange(linkable, RedstoneLinkable::allowRecalcQueue, this.recalcQueue::add);
		} else if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		}
	}

	public void modeChanged(final @NonNull RedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		this.getChannel(linkable.channel).get(snapshot.receiver()).remove(linkable);
		this.getChannel(linkable.channel).get(linkable.isReceiver()).add(linkable);
		Create.REDSTONE_LINK_NETWORK.setDirty();

		if (!snapshot.receiver() && snapshot.signal() > 0) {
			this.forLinkableInRange(snapshot, RedstoneLinkable::allowRecalcQueue, linkable1 -> {
				linkable1.considerRecalcQueued();
				this.recalcQueue.add(linkable1);
			});
		}

		if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		} else if (!linkable.isReceiver()) {
			linkable.queueUpdate();
		}
	}

	public void signalChanged(final @NonNull RedstoneLinkable linkable, final RedstoneLinkableSnapshot snapshot) {
		Create.REDSTONE_LINK_NETWORK.setDirty();
		this.forLinkableInRange(linkable, linkable1 -> {
			if (!linkable1.allowRecalcQueue()) return false;
			if (linkable1 instanceof ICustomReceive) return true;
			final int currentStrength = linkable1.getReceivedStrength();
			return (currentStrength == snapshot.signal() || currentStrength < linkable.getTransmittedStrength());
		}, linkable1 -> {
			linkable1.considerRecalcQueued();
			this.recalcQueue.add(linkable1);
		});
	}

	public void linkMoved(final @NonNull RedstoneLinkable linkable, final @NonNull RedstoneLinkableSnapshot snapshot) {
		Create.REDSTONE_LINK_NETWORK.setDirty();
		if (linkable.isReceiver() && linkable.allowRecalcQueue()) {
			linkable.considerRecalcQueued();
			this.recalcQueue.add(linkable);
		} else if (linkable.isTransmitter()) {
			final Collection<RedstoneLinkable> oldReceivers = new HashSet<>(23 /* Allows 16 entries before resize */, 0.7f);
			final Collection<RedstoneLinkable> newReceivers = new HashSet<>(23 /* Allows 16 entries before resize */, 0.7f);

			this.forLinkableInRange(snapshot, RedstoneLinkable::allowRecalcQueue, oldReceivers::add);
			this.forLinkableInRange(linkable, RedstoneLinkable::allowRecalcQueue, newReceivers::add);

			for (final RedstoneLinkable link : oldReceivers) {
				if (!newReceivers.contains(link)) {
					link.considerRecalcQueued();
					this.recalcQueue.add(link);
				}
			}

			for (final RedstoneLinkable link : newReceivers) {
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
	public static boolean canCommunicate(final @NonNull RedstoneLinkable linkable1, final @NonNull RedstoneLinkable linkable2) {
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
	public static boolean canCommunicate(final @NonNull RedstoneLinkableSnapshot linkable1, final @NonNull RedstoneLinkable linkable2) {
		if (linkable1.receiver()) {
			return linkable1.receivingPosition().distanceSquared(linkable2.getTransmissionPosition()) <= Math.pow(Math.min(linkable1.receivingRange(), linkable1.transmissionRange()), 2);
		} else {
			return linkable1.transmissionPosition().distanceSquared(linkable2.getReceivingPosition()) <= Math.pow(Math.min(linkable1.transmissionRange(), linkable1.receivingRange()), 2);
		}
	}

	@Contract(pure = true)
	public @NonNull Couple<Set<RedstoneLinkable>> getChannel(final @NonNull Couple<Frequency> key) {
		return this.channels.computeIfAbsent(key, frequencies -> Couple.create(new HashSet<>(12, 0.7f), new HashSet<>(12, 0.7f)));
	}
}
