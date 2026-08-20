package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPersistentObjectTypes;
import com.simibubi.create.foundation.persistent.BlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkBlockBoundLinkable extends BlockBoundRedstoneLinkable {

	public static RedstoneLinkBlockBoundLinkable create(final ServerLevel level, final BlockPos pos, final BlockState state) {
		final RedstoneLinkBlockBoundLinkable linkBlockBoundLinkable = new RedstoneLinkBlockBoundLinkable(pos, level);
		//linkBlockBoundLinkable.initialize();
		return linkBlockBoundLinkable;
	}

	public static RedstoneLinkBlockBoundLinkable create(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		final RedstoneLinkBlockBoundLinkable linkBlockBoundLinkable = new RedstoneLinkBlockBoundLinkable(compoundTag, registries, serverLevel);
		//linkBlockBoundLinkable.initialize();
		return linkBlockBoundLinkable;
	}

	@Override
	public Tag save(final CompoundTag compoundTag, final HolderLookup.Provider provider) {
		return null;
	}

	@Override
	public PersistentObjectType<?> getType() {
		return AllPersistentObjectTypes.REDSTONE_LINK.get();
	}

	@Override
	public boolean isValidBlockState(final BlockState state) {
		return state.getBlock() == AllBlocks.REDSTONE_LINK.get();
	}

	protected RedstoneLinkBlockBoundLinkable(final BlockPos blockPos, final ServerLevel serverLevel) {
		super(AllBlocks.REDSTONE_LINK.get(), blockPos, serverLevel.getBlockState(blockPos), serverLevel);
	}

	protected RedstoneLinkBlockBoundLinkable(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		super(AllBlocks.REDSTONE_LINK.get(), compoundTag, registries, serverLevel);
	}
}
