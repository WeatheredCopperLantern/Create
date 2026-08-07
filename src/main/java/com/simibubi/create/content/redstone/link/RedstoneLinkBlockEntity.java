package com.simibubi.create.content.redstone.link;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneLinkBlockEntity extends SmartBlockEntity {

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

	}

	public RedstoneLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}
}
