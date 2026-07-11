package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.content.redstone.link.*;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class LinkedControllerServerHandler {

	private static final Map<Entity, Map<Integer, RedstoneControllerLinkable>> activeSignals;

	static {
		activeSignals = HashMap.newHashMap(5);
	}

	public static void handle(final BitSet keys, final Entity sender, final ItemStack controllerItem) {
		final Map<Integer, RedstoneControllerLinkable> senderMap = activeSignals.computeIfAbsent(sender, $ -> new HashMap<>());

		for (int i = 0; i < keys.size(); i++) {
			final boolean pressed = keys.get(i);
			final Couple<Frequency> channel = LinkedControllerItem.toFrequency(controllerItem, i);
			RedstoneControllerLinkable link = senderMap.computeIfAbsent(i, integer -> {
				if (pressed) return new RedstoneControllerLinkable(channel, sender);
				return null;
			});
			if (link == null) continue;
		}
	}

	public static class RedstoneControllerLinkable extends RedstoneEntityLinkable {

		@Override
		public boolean doSave() {
			return false;
		}

		@Override
		public RedstoneLinkableType getType() {
			return null;
		}

		@Override
		protected boolean shouldSetFrequency(boolean first, Frequency frequency) {
			return false;
		}

		@Override
		protected void onFrequencyChanged(boolean first) {

		}

		@Override
		protected void onModeChanged(RedstoneLinkableSnapshot snapshot) {

		}

		@Override
		protected void onSignalChanged() {

		}

		@Override
		protected boolean shouldSetMode(boolean receiver) {
			return false;
		}

		public RedstoneControllerLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
			super(nbt, channel, receiver, registries, dimensions, network);
		}

		public RedstoneControllerLinkable(Couple<Frequency> channel, Entity entity) {
			super(channel, entity);
			setTransmittedStrength(15);
		}
	}
}
