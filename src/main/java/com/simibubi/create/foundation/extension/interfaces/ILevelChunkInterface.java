package com.simibubi.create.foundation.extension.interfaces;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface ILevelChunkInterface {

	default void create$forceAddAndRegisterBlockEntity(BlockEntity blockEntity) { }
}
