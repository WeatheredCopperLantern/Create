package com.simibubi.create.foundation.extension.interfaces;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import org.jspecify.annotations.Nullable;

public interface PistonMovingBlockEntityAccessor {

	void create$setBlockEntity(SmartBlockEntity blockEntity);

	@Nullable SmartBlockEntity create$getBlockEntity();
}
