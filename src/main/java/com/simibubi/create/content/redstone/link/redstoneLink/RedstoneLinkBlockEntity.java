package com.simibubi.create.content.redstone.link.redstoneLink;

import java.util.List;

import com.simibubi.create.content.redstone.link.linkable.BlockBoundRedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.ILinkableBlockEntity;
import com.simibubi.create.content.redstone.link.linkable.LinkableBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkBlockEntity extends SmartBlockEntity implements ILinkableBlockEntity {

	private RedstoneLinkBlockLinkableBehaviour behaviour;

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
		behaviours.add(this.behaviour = new RedstoneLinkBlockLinkableBehaviour(this));
	}

	@Override
	public LinkableBehaviour<? extends BlockBoundRedstoneLinkable> getLinkableBehaviour() {
		return this.behaviour;
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!this.initialized) this.initialize();
	}

	@Override
	public void initialize() {
		initialized = true;
		super.initialize();
	}

	public RedstoneLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}
}
