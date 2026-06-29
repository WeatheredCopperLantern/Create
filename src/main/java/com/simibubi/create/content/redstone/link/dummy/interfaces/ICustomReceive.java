package com.simibubi.create.content.redstone.link.dummy.interfaces;

import java.util.Set;

public interface ICustomReceive {

	public void calculateSignal(Set<IRedstoneLinkable> sendersInRange);
}
