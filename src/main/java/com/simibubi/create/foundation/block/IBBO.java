package com.simibubi.create.foundation.block;

import java.util.function.Consumer;

import com.simibubi.create.foundation.persistent.BlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

public interface IBBO<T extends BlockBoundObject> {

	default T newBlockBoundObject(ServerLevel level, BlockPos pos, BlockState state) {
		return getBlockBoundObjectType().create(level, pos, state);
	}

	PersistentObjectType<T> getBlockBoundObjectType();

	default boolean isBBOPushable() {
		return true;
	}

	/**
	 * Does create a BBO if it doesn't exist
	 */
	default void withBlockBoundObjectDo(final BlockGetter level, final BlockPos pos, Consumer<T> action) {
		final T bbo = this.getBlockBoundObject(level, pos);
		if (bbo != null) action.accept(bbo);
	}

	/**
	 * Does create a BBO if it doesn't exist
	 */
	default @Nullable T getBlockBoundObject(BlockGetter level, BlockPos pos) {
		return level.create$getBlockBoundObject(pos, getBlockBoundObjectType());
	}
}
