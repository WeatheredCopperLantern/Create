package com.simibubi.create.infrastructure.gametest.blockBoundObjects;

import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DummyBlockBoundObjectType extends PersistentObjectType<DummyBlockBoundObject> {

	@Override
	public Class<DummyBlockBoundObject> getPersistentObjectClass() {
		return DummyBlockBoundObject.class;
	}

	@Override
	public DummyBlockBoundObject create(final ServerLevel level, final @Nullable BlockPos pos, final @Nullable BlockState state) {
		return DummyBlockBoundObject.create(level, pos, state);
	}

	@Override
	public DummyBlockBoundObject load(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel level) {
		return DummyBlockBoundObject.create(compoundTag, registries, level);
	}
}
