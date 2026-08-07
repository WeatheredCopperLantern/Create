package com.simibubi.create.foundation.block;

import java.util.Optional;
import java.util.function.Consumer;

import com.simibubi.create.foundation.persistent.PersistentBlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface IBBO<T extends PersistentBlockBoundObject> {

	PersistentObjectType<T> getPersistentBlockBoundObjectType();

	/**
	 * Does create a BBO if it doesn't exist
	 */
	default void withBlockBoundObjectDo(final BlockGetter level, final BlockPos pos, Consumer<T> action) {
		final T bbo = this.getBlockBoundObject(level, pos);
		if (bbo != null) action.accept(bbo);
	}

	/**
	 * Does not create a BBO if it doesn't exist
	 */
	default Optional<T> getBlockBoundObjectOptional(final BlockGetter level, final BlockPos pos) {
		return Optional.ofNullable(level.create$getPersistentBlockBoundObject(pos, getPersistentBlockBoundObjectType()));
	}

	/**
	 * Does create a BBO if it doesn't exist
	 */
	default T getBlockBoundObject(BlockGetter level, BlockPos pos) {
		return level.create$getOrCreatePersistentBlockBoundObject(pos, getPersistentBlockBoundObjectType());
	}
}
