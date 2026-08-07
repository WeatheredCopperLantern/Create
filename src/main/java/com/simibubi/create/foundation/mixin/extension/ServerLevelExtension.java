package com.simibubi.create.foundation.mixin.extension;

import java.util.UUID;
import java.util.function.Supplier;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.extension.interfaces.IPersistentObjectSupplier;
import com.simibubi.create.foundation.persistent.PersistentBlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
public abstract class ServerLevelExtension extends Level implements IPersistentObjectSupplier {

	@Override
	public @Nullable PersistentBlockBoundObject create$getPersistentBlockBoundObject(final BlockPos pos) {
		return Create.PERSISTENT_OBJECTS.getManager(this.dimension()).getPersistentBlockBoundObject(pos);
	}

	@Override
	public @Nullable PersistentObject create$getPersistentObject(final UUID uuid) {
		return Create.PERSISTENT_OBJECTS.getManager(this.dimension()).getPersistentObject(uuid);
	}

	@Override
	public @Nullable <T extends PersistentBlockBoundObject> T create$getPersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		return Create.PERSISTENT_OBJECTS.getManager(this.dimension()).getPersistentBlockBoundObject(pos, type);
	}

	@Override
	public @Nullable <T extends PersistentObject> T create$getPersistentObject(final UUID uuid, final PersistentObjectType<T> type) {
		return Create.PERSISTENT_OBJECTS.getManager(this.dimension()).getPersistentObject(uuid, type);
	}

	@Override
	public @Nullable <T extends PersistentBlockBoundObject> T create$getOrCreatePersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		return Create.PERSISTENT_OBJECTS.getManager(this.dimension()).getOrCreatePersistentBlockBoundObject(pos, type);
	}

	private ServerLevelExtension(final WritableLevelData levelData, final ResourceKey<Level> dimension, final RegistryAccess registryAccess, final Holder<DimensionType> dimensionTypeRegistration, final Supplier<ProfilerFiller> profiler, final boolean isClientSide, final boolean isDebug, final long biomeZoomSeed, final int maxChainedNeighborUpdates) {
		super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
	}
}
