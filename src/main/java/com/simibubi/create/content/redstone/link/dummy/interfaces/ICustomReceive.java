package com.simibubi.create.content.redstone.link.dummy.interfaces;

import java.util.Set;

public interface ICustomReceive {

	void calculateSignal(Set<IRedstoneLinkable> sendersInRange);
}
