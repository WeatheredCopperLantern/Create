package com.simibubi.create.content.redstone.link.linkable;

public interface ILinkableBlockEntity {

	public LinkableBehaviour<? extends BlockBoundRedstoneLinkable> getLinkableBehaviour();
}
