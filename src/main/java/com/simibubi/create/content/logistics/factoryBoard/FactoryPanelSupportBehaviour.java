package com.simibubi.create.content.logistics.factoryBoard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.codecs.CatnipCodecUtils;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public class FactoryPanelSupportBehaviour<T extends SmartBlockEntity & IFactoryPanelSupportBehaviourEventHandler> extends BlockEntityBehaviour {

	public static final BehaviourType<FactoryPanelSupportBehaviour<?>> TYPE = new BehaviourType<>();

	public final T blockEntity;
	private final List<FactoryPanelPosition> linkedPanels;
	private boolean outputPower = false;
	private boolean isOutput = false;

	public List<FactoryPanelPosition> getLinkedPanels() {
		return linkedPanels;
	}

	public void connect(FactoryPanelBehaviour panel) {
		FactoryPanelPosition panelPosition = panel.getPanelPosition();
		if (linkedPanels.contains(panelPosition)) return;
		linkedPanels.add(panelPosition);
		this.blockEntity.onLinkedPanelAdded(panelPosition);
	}

	public void disconnect(FactoryPanelBehaviour panel) {
		this.linkedPanels.remove(panel.getPanelPosition());
		this.blockEntity.onLinkedPanelRemoved(panel.getPanelPosition());
	}

	@Deprecated
	public void notifyChange() {
	}

	public boolean shouldPanelBePowered() {
		return isOutput && outputPower;
	}

	public boolean isOutput() {
		return this.isOutput;
	}

	public boolean isSignaling() {
		return this.outputPower;
	}

	public void notifyPanels() {
		if (getWorld().isClientSide()) return;
		for (Iterator<FactoryPanelPosition> iterator = linkedPanels.iterator(); iterator.hasNext(); ) {
			FactoryPanelPosition panelPos = iterator.next();
			if (!getWorld().isLoaded(panelPos.pos())) continue;
			FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
			if (behaviour == null) {
				iterator.remove();
				notifyChange();
				continue;
			}
			behaviour.checkForRedstoneInput();
		}
	}

	@Deprecated
	@Nullable
	public Boolean shouldBePoweredTristate() {
		for (Iterator<FactoryPanelPosition> iterator = linkedPanels.iterator(); iterator.hasNext(); ) {
			FactoryPanelPosition panelPos = iterator.next();
			if (!getWorld().isLoaded(panelPos.pos())) return null;
			FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
			if (behaviour == null) {
				iterator.remove();
				notifyChange();
				continue;
			}
			if (behaviour.isActive() && behaviour.satisfied && behaviour.count != 0) return true;
		}
		return false;
	}


	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	public FactoryPanelSupportBehaviour(final T be) {
		super(be);
		this.blockEntity = be;
		this.linkedPanels = new ArrayList<>();
	}

	//private final List<FactoryPanelPosition> linkedPanels;
	//
	//private final Supplier<Boolean> outputPower;
	//private final Supplier<Boolean> isOutput;
	//private final Runnable onChange;
	//
	//public FactoryPanelSupportBehaviour(SmartBlockEntity be, Supplier<Boolean> isOutput, Supplier<Boolean> outputPower, Runnable onChange) {
	//	super(be);
	//	this.isOutput = isOutput;
	//	this.outputPower = outputPower;
	//	this.onChange = onChange;
	//	this.linkedPanels = new ArrayList<>();
	//}
	//
	//public boolean shouldPanelBePowered() {
	//	return isOutput() && outputPower.get();
	//}
	//
	//public boolean isOutput() {
	//	return isOutput.get();
	//}
	//
	//@Override
	//public void destroy() {
	//	for (FactoryPanelPosition panelPos : linkedPanels) {
	//		if (!getWorld().isLoaded(panelPos.pos())) continue;
	//		FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
	//		behaviour.targetedByLinks.remove(getPos());
	//		behaviour.blockEntity.notifyUpdate();
	//	}
	//	super.destroy();
	//}
	//
	//@Override
	//public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
	//	nbt.put("LinkedGauges", CatnipCodecUtils.encode(Codec.list(FactoryPanelPosition.CODEC), registries, linkedPanels).orElseThrow());
	//}
	//
	//@Override
	//public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
	//	linkedPanels.clear();
	//	CatnipCodecUtils.decode(Codec.list(FactoryPanelPosition.CODEC), registries, nbt.get("LinkedGauges")).ifPresent(linkedPanels::addAll);
	//}
	//
}
