package com.simibubi.create.content.redstone.link;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.nbt.CompoundTag;

public record RedstoneLinkableType(
	PropertyDispatch.PentaFunction<CompoundTag, Couple<Frequency>, Boolean, HolderLookup.Provider, DimensionPalette, ? extends RedstoneLinkable> factory) {

}
