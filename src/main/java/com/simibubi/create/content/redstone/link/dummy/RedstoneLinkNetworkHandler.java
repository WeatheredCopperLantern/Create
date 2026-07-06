package com.simibubi.create.content.redstone.link.dummy;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.simibubi.create.content.redstone.link.dummy.interfaces.IRedstoneLinkable;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class RedstoneLinkNetworkHandler {

	public final Map<Object, RedstoneLinkNetwork> networks = new IdentityHashMap<>();

	public final AtomicInteger globalPowerVersion = new AtomicInteger();

	public void addNetworkFor(final Object object, final RedstoneLinkNetwork network) {
		this.networks.put(object, network);
	}

	public RedstoneLinkNetwork createNetworkFor(final Object object) {
		final RedstoneLinkNetwork network = new RedstoneLinkNetwork();
		this.networks.put(object, network);
		return network;
	}

	public void deleteNetworkOf(final Object object) {
		this.networks.remove(object);
	}

	public RedstoneLinkNetwork findNetwork(final Vec3 pos, final Level level) {
		return this.networks.get(level);
	}

	public RedstoneLinkNetwork findNetwork(final BlockEntity blockEntity) {
		return this.findNetwork(Vec3.atCenterOf(blockEntity.getBlockPos()), blockEntity.getLevel());
	}

	public RedstoneLinkNetwork findNetwork(final Entity entity) {
		return this.findNetwork(Vec3.atCenterOf(entity.blockPosition()), entity.level());
	}

	public void tick() {
		this.networks.values().forEach(RedstoneLinkNetwork::tick);
	}

	public boolean hasAnyLoadedPower(final Couple<Frequency> frequency) {
		for (final RedstoneLinkNetwork network : this.networks.values()) {
			final Couple<Set<IRedstoneLinkable>> links = network.getChannelMembers(frequency);
			if (links.get(false).isEmpty()) {
				return false;
			}
			for (final IRedstoneLinkable link : links.get(false)) {
				if (link.getTransmittedStrength() > 0) return true;
			}
		}
		return false;
	}

	public static class Frequency {

		public static final Frequency EMPTY = new Frequency(ItemStack.EMPTY);
		private static final Map<Item, Frequency> simpleFrequencies = new IdentityHashMap<>();
		private final ItemStack stack;
		private final Item item;
		private final int color;

		public static Frequency of(final ItemStack stack) {
			if (stack.isEmpty()) return Frequency.EMPTY;
			if (stack.getComponents().isEmpty()) return Frequency.simpleFrequencies.computeIfAbsent(stack.getItem(), $ -> new Frequency(stack));
			return new Frequency(stack);
		}

		private Frequency(final ItemStack stack) {
			this.stack = stack;
			this.item = stack.getItem();
			this.color = stack.has(DataComponents.DYED_COLOR) ? stack.get(DataComponents.DYED_COLOR).rgb() : -1;
		}

		public ItemStack getStack() {
			return this.stack;
		}

		@Override
		public int hashCode() {
			return (this.item.hashCode() * 31) ^ this.color;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) return true;
			return obj instanceof final Frequency frequency && frequency.item == this.item && frequency.color == this.color;
		}
	}
}
