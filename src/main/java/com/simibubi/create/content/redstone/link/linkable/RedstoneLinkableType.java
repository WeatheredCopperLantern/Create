package com.simibubi.create.content.redstone.link.linkable;

import com.simibubi.create.content.redstone.link.interfaces.HexaFunction;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public record RedstoneLinkableType(
		HexaFunction<CompoundTag, Couple<Frequency>, Boolean, HolderLookup.Provider, DimensionPalette, RedstoneLinkNetwork, ? extends RedstoneLinkable> factory) {
}
