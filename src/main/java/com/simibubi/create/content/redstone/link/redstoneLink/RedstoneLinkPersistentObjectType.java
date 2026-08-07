package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkPersistentObjectType extends PersistentObjectType<RedstoneLinkBlockBoundLinkable> {

	@Override
	public Class<RedstoneLinkBlockBoundLinkable> getPersistentObjectClass() {
		return RedstoneLinkBlockBoundLinkable.class;
	}

	@Override
	public RedstoneLinkBlockBoundLinkable create(final ServerLevel level, final @Nullable BlockPos pos) {
		return RedstoneLinkBlockBoundLinkable.create(level, pos);
	}

	@Override
	public RedstoneLinkBlockBoundLinkable load(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel level) {
		return RedstoneLinkBlockBoundLinkable.create(compoundTag, registries, level);
	}
}
