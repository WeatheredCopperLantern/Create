package com.simibubi.create.foundation.persistent;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.SimpleRegistry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PersistentObjectType<T extends PersistentObject> {

	public static final SimpleRegistry<Block, PersistentObjectType<?>> REGISTRY = SimpleRegistry.create();

	public abstract Class<T> getPersistentObjectClass();

	public abstract T create(final ServerLevel level, final @Nullable BlockPos pos);

	public abstract T load(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel level);

	/**
	 * Utility for use with Registrate builders. Creates a builder transformer
	 * that will register the given PersistentObjectType to a block when ready.
	 */
	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> persistentObject(RegistryEntry<PersistentObjectType<?>, ? extends PersistentObjectType<?>> type) {
		return builder -> builder.onRegisterAfter(CreateRegistries.PERSISTENT_OBJECT_TYPE, block -> REGISTRY.register(block, type.get()));
	}
}
