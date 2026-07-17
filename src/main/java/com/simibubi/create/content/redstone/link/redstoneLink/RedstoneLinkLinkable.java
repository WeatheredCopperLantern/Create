package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.linkable.BlockEntityRedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableType;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkLinkable extends BlockEntityRedstoneLinkable<RedstoneLinkBlockEntity> {

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_LINK.value();
	}

	public RedstoneLinkLinkable(final CompoundTag tag, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(tag, channel, receiver, registries, dimensions, network);
	}

	public RedstoneLinkLinkable(final Couple<Frequency> channel, final RedstoneLinkBlockEntity be) {
		super(channel);
		this.setBlockEntity(be);
		assert be.getLevel() != null;
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(be.getLevel()));
	}
}
