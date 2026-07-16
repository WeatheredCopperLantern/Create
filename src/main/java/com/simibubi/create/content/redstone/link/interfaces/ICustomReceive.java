package com.simibubi.create.content.redstone.link.interfaces;

import java.util.Set;

import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkable;

@FunctionalInterface
public interface ICustomReceive {

	void calculateSignal(Set<RedstoneLinkable> sendersInRange);
}
