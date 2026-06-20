package com.simibubi.create.content.redstone.link.ehh;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.simibubi.create.content.redstone.link.ehh.interfaces.IRedstoneLinkable;
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

	public void addNetworkFor(Object object, RedstoneLinkNetwork network) {
		networks.put(object, network);
	}

	public RedstoneLinkNetwork createNetworkFor(Object object) {
		RedstoneLinkNetwork network = new RedstoneLinkNetwork();
		networks.put(object, network);
		return network;
	}

	public void deleteNetworkOf(Object object) {
		networks.remove(object);
	}

	public RedstoneLinkNetwork findNetwork(Vec3 pos, Level level) {
		return networks.get(level);
	}

	public RedstoneLinkNetwork findNetwork(BlockEntity blockEntity) {
		return findNetwork(Vec3.atCenterOf(blockEntity.getBlockPos()), blockEntity.getLevel());
	}

	public RedstoneLinkNetwork findNetwork(Entity entity) {
		return findNetwork(Vec3.atCenterOf(entity.blockPosition()), entity.level());
	}

	public void tick() {
		networks.values().forEach(RedstoneLinkNetwork::tick);
	}

	public boolean hasAnyLoadedPower(Couple<Frequency> frequency) {
		for (RedstoneLinkNetwork network : networks.values()) {
			Couple<Set<IRedstoneLinkable>> links = network.getChannelMembers(frequency);
			if (links.get(false).isEmpty()) {
				return false;
			}
			for (IRedstoneLinkable link : links.get(false)) {
				if (link.getTransmittedStrength() > 0) return true;
			}
		}
		return false;
	}

	public static class Frequency {
		public static final Frequency EMPTY = new Frequency(ItemStack.EMPTY);
		private static final Map<Item, Frequency> simpleFrequencies = new IdentityHashMap<>();
		private ItemStack stack;
		private Item item;
		private int color;

		public static Frequency of(ItemStack stack) {
			if (stack.isEmpty())
				return EMPTY;
			if (stack.getComponents().isEmpty())
				return simpleFrequencies.computeIfAbsent(stack.getItem(), $ -> new Frequency(stack));
			return new Frequency(stack);
		}

		private Frequency(ItemStack stack) {
			this.stack = stack;
			item = stack.getItem();
			color = stack.has(DataComponents.DYED_COLOR) ? stack.get(DataComponents.DYED_COLOR).rgb() : -1;
		}

		public ItemStack getStack() {
			return stack;
		}

		@Override
		public int hashCode() {
			return (item.hashCode() * 31) ^ color;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			return obj instanceof Frequency frequency && frequency.item == item && frequency.color == color;
		}
	}
}
