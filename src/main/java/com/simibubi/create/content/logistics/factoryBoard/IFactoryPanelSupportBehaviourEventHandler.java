package com.simibubi.create.content.logistics.factoryBoard;

public interface IFactoryPanelSupportBehaviourEventHandler {

	void onLinkedPanelAdded(FactoryPanelPosition panelPosition);

	void onLinkedPanelRemoved(final FactoryPanelPosition panelPosition);
}
