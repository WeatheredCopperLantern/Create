package com.simibubi.create.content.redstone.link.ehh.interfaces;

import java.util.Set;

public interface ICustomReceive {

	public void calculateSignal(Set<IRedstoneLinkable> sendersInRange);
}
