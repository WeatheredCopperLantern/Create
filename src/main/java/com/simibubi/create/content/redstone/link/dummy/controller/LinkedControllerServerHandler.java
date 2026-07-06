package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.redstone.link.dummy.EntityRedstoneLinkable;
import com.simibubi.create.content.redstone.link.dummy.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.WorldAttached;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class LinkedControllerServerHandler {

	private static final WorldAttached<Map<UUID, Collection<LinkedControllerSignal>>> receivedInputs = new WorldAttached<>($ -> new HashMap<>());
	static final int TIMEOUT = 30;

	public static void tick(final LevelAccessor world) {
		final Map<UUID, Collection<LinkedControllerSignal>> map = LinkedControllerServerHandler.receivedInputs.get(world);
		for (final Iterator<Map.Entry<UUID, Collection<LinkedControllerSignal>>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
			final Map.Entry<UUID, Collection<LinkedControllerSignal>> entry = iterator.next();
			final Collection<LinkedControllerSignal> list = entry.getValue();

			list.removeIf(signal -> {
				if (signal.tickLifetime() <= 0) {
					signal.clearNetwork();
					return true;
				}
				return false;
			});

			if (list.isEmpty()) iterator.remove();
		}
	}

	public static void receivePressed(final Entity origin, final LevelAccessor world, final UUID uniqueID, final List<Couple<RedstoneLinkNetworkHandler.Frequency>> collect, final boolean pressed) {
		final Map<UUID, Collection<LinkedControllerSignal>> map = LinkedControllerServerHandler.receivedInputs.get(world);
		final Collection<LinkedControllerSignal> list = map.computeIfAbsent(uniqueID, $ -> new ArrayList<>());

		WithNext:
		for (final Couple<RedstoneLinkNetworkHandler.Frequency> channel : collect) {
			for (final LinkedControllerSignal signal : list) {
				if (!signal.getChannelKey().equals(channel)) continue;
				if (!pressed) {
					signal.clearNetwork();
					list.remove(signal);
				} else {
					signal.resetLifetime();
				}
				continue WithNext;
			}
			final LinkedControllerSignal entry = new LinkedControllerSignal(channel, origin);
			list.add(entry);
			if (!entry.getNetwork().getChannelMembers(entry.getChannelKey()).get(true).isEmpty()) {
				AllAdvancements.LINKED_CONTROLLER.awardTo(world.getPlayerByUUID(uniqueID));
			}
		}
	}

	static class LinkedControllerSignal extends EntityRedstoneLinkable {

		private int lifetime;

		private LinkedControllerSignal(final Couple<RedstoneLinkNetworkHandler.Frequency> channel, final Entity entity) {
			super(channel, Mode.TRANSMIT, value -> {
			}, () -> 15, entity);
			this.lifetime = LinkedControllerServerHandler.TIMEOUT;
		}

		private int tickLifetime() {
			this.tick();
			--this.lifetime;
			return this.lifetime;
		}

		private void resetLifetime() {
			this.lifetime = LinkedControllerServerHandler.TIMEOUT;
		}

		@Override
		protected boolean shouldSetMode(final Mode newMode) {
			return false;
		}

		@Override
		protected boolean shouldSetFrequency(final boolean first, final ItemStack stack) {
			return false;
		}

		@Override
		protected void onModeChanged(final Mode newMode) {

		}

		@Override
		protected void onFrequencyChanged(final boolean first, final ItemStack stack) {

		}
	}
}
