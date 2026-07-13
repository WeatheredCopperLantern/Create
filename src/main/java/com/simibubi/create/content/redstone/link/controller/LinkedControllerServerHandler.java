package com.simibubi.create.content.redstone.link.controller;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.content.redstone.link.Frequency;
import com.simibubi.create.foundation.utility.ControlsUtil;
import net.createmod.catnip.data.Couple;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerServerHandler {

//region Fields
	private static final Map<Entity, Map<Integer, RedstoneControllerLinkable>> activeSignals;
	private static final int TIMEOUT = 30;

	static {
		activeSignals = HashMap.newHashMap(5);
	}
//endregion

	public static void handle(final BitSet keys, final Entity sender, final ItemStack controllerItem) {
		final Map<Integer, RedstoneControllerLinkable> senderMap = LinkedControllerServerHandler.activeSignals.computeIfAbsent(sender, $ -> new HashMap<>());

		for (int i = 0; i < ControlsUtil.getControls().size(); i++) {
			final boolean pressed = keys.get(i);
			final Couple<Frequency> channel = LinkedControllerItem.toFrequency(controllerItem, i);
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
