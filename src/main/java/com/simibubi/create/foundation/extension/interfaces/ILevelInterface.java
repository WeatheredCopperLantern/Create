package com.simibubi.create.foundation.extension.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface ILevelInterface {

	// Allows setting a new Block with an existing Block Entity instead of creating a new one.
	default boolean create$setBlockWithBlockEntity(final BlockPos blockPos, final BlockState blockState, final int i, final BlockEntity movedBE){
		return false;
	}
}
