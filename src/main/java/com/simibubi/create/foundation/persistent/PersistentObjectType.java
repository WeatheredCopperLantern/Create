package com.simibubi.create.foundation.persistent;

import com.simibubi.create.api.registry.SimpleRegistry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PersistentObjectType<T extends PersistentObject> {

	public static final SimpleRegistry<Block, PersistentObjectType<?>> REGISTRY = SimpleRegistry.create();

	public abstract Class<T> getPersistentObjectClass();

	public abstract T create(final ServerLevel level, final @Nullable BlockPos pos, final @Nullable BlockState state);

	public abstract T load(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel level);
}
