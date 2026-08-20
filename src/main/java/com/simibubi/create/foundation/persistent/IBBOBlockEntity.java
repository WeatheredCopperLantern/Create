package com.simibubi.create.foundation.persistent;

import java.util.function.Consumer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IBBOBlockEntity<T extends BlockBoundObject> {

	default @Nullable T getBlockBoundObject(BlockGetter level, BlockPos pos) {
		return level.create$getBlockBoundObject(pos, getBlockBoundObjectType());
	}

	default void withBlockBoundObjectDo(final BlockGetter level, final BlockPos pos, Consumer<T> action) {
		final T bbo = this.getBlockBoundObject(level, pos);
		if (bbo != null) action.accept(bbo);
	}

	PersistentObjectType<T> getBlockBoundObjectType();
}
