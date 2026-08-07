package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPersistentObjectTypes;
import com.simibubi.create.content.redstone.link.linkable.BlockBoundRedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableSnapshot;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.createmod.catnip.data.ImmutableCouple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkBlockBoundLinkable extends BlockBoundRedstoneLinkable {

	public static RedstoneLinkBlockBoundLinkable create(final ServerLevel level, final BlockPos pos) {
		final RedstoneLinkBlockBoundLinkable linkBlockBoundLinkable = new RedstoneLinkBlockBoundLinkable(pos, level);
		linkBlockBoundLinkable.initialize();
		return linkBlockBoundLinkable;
	}

	public static RedstoneLinkBlockBoundLinkable create(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		final RedstoneLinkBlockBoundLinkable linkBlockBoundLinkable = new RedstoneLinkBlockBoundLinkable(compoundTag, registries, serverLevel);
		linkBlockBoundLinkable.initialize();
		return linkBlockBoundLinkable;
	}

	@Override
	public PersistentObjectType<?> getType() {
		return AllPersistentObjectTypes.REDSTONE_LINK.get();
	}

	@Override
	protected void channelChanged(final RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected void modeChanged(final RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected void signalChanged(final RedstoneLinkableSnapshot snapshot) {

	}

	private RedstoneLinkBlockBoundLinkable(final BlockPos blockPos, final ServerLevel serverLevel) {
		super(AllBlocks.REDSTONE_LINK.get(), blockPos, serverLevel.getBlockState(blockPos), false, ImmutableCouple.create(Frequency.EMPTY, Frequency.EMPTY), serverLevel);
	}

	private RedstoneLinkBlockBoundLinkable(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		super(AllBlocks.REDSTONE_LINK.get(), compoundTag, registries, serverLevel);
	}
}
