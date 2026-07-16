package com.simibubi.create.content.redstone.link.controller;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.foundation.utility.ControlsUtil;
import net.createmod.catnip.data.Couple;

import net.minecraft.world.entity.Entity;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;

@ParametersAreNonnullByDefault
public class LinkedControllerServerHandler {

	//region Fields
	private final Map<Entity, Map<Integer, RedstoneControllerLinkable>> activeSignals;
	private static final int TIMEOUT = 30;

	//endregion

	public LinkedControllerServerHandler() {
		activeSignals = HashMap.newHashMap(5);
	}

	public void handle(final BitSet keys, final Entity sender, final ItemStackHandler frequencyItems) {
		final Map<Integer, RedstoneControllerLinkable> senderMap = this.activeSignals.computeIfAbsent(sender, $ -> new HashMap<>());

		for (int i = 0; i < ControlsUtil.getControls().size(); i++) {
			final boolean pressed = keys.get(i);
			final Couple<Frequency> channel = Couple.create(Frequency.of(frequencyItems.getStackInSlot(i * 2)), Frequency.of(frequencyItems.getStackInSlot(i * 2 + 1)));
			final RedstoneControllerLinkable link = senderMap.computeIfAbsent(i, integer -> {
				if (pressed) return new RedstoneControllerLinkable(channel, sender);
				return null;
			});
			if (link == null) continue;
			if (pressed) {
				link.refresh(LinkedControllerServerHandler.TIMEOUT, channel);
			} else {
				link.destroy();
			}
		}
	}
}
