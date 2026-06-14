package com.simibubi.create.content.redstone.link.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.simibubi.create.content.redstone.link.EntityRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class LinkedControllerServerHandler {

	private static final WorldAttached<Map<UUID, Collection<LinkedControllerSignal>>> receivedInputs = new WorldAttached<>($ -> new HashMap<>());
	static final int TIMEOUT = 30;

	public static void tick(LevelAccessor world) {
		Map<UUID, Collection<LinkedControllerSignal>> map = receivedInputs.get(world);
		for (Iterator<Entry<UUID, Collection<LinkedControllerSignal>>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
			Entry<UUID, Collection<LinkedControllerSignal>> entry = iterator.next();
			Collection<LinkedControllerSignal> list = entry.getValue();

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

	public static void receivePressed(Entity origin, LevelAccessor world, UUID uniqueID, List<Couple<Frequency>> collect,
									  boolean pressed) {
		Map<UUID, Collection<LinkedControllerSignal>> map = receivedInputs.get(world);
		Collection<LinkedControllerSignal> list = map.computeIfAbsent(uniqueID, $ -> new ArrayList<>());

		WithNext:
		for (Couple<Frequency> channel : collect) {
			for (LinkedControllerSignal signal : list) {
				if (!signal.getChannelKey().equals(channel)) continue;
				if (!pressed) {
					signal.clearNetwork();
					list.remove(signal);
				} else {
					signal.resetLifetime();
				}
				continue WithNext;
			}
			LinkedControllerSignal entry = new LinkedControllerSignal(channel, origin);
			list.add(entry);
			if (!entry.getNetwork().getChannel(entry.getChannelKey()).get(true).isEmpty()) {
				AllAdvancements.LINKED_CONTROLLER.awardTo(world.getPlayerByUUID(uniqueID));
			}
		}
	}

	static class LinkedControllerSignal extends EntityRedstoneLinkable {

		private int lifetime;

		private LinkedControllerSignal(Couple<Frequency> channel, Entity entity) {
			super(channel, Mode.TRANSMIT, value -> {
			}, () -> 15, entity);
			lifetime = TIMEOUT;
		}


		private int tickLifetime() {
			tick();
			return --lifetime;
		}

		private void resetLifetime() {
			lifetime = TIMEOUT;
		}

		@Override
		protected boolean shouldSetMode(Mode newMode) {
			return false;
		}

		@Override
		protected boolean shouldSetFrequency(boolean first, ItemStack stack) {
			return false;
		}

		@Override
		protected void onModeChanged(Mode newMode) {

		}

		@Override
		protected void onFrequencyChanged(boolean first, ItemStack stack) {

		}
	}

}
