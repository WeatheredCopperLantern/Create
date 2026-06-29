package com.simibubi.create;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.link.Frequency;
import com.simibubi.create.content.redstone.link.RedstoneLinkLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkableType;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.nbt.CompoundTag;

public final class AllRedstoneLinkables {

	public static final Holder.Reference<RedstoneLinkableType> REDSTONE_LINK = AllRedstoneLinkables.register("redstone_link", RedstoneLinkLinkable::new);

	private static Holder.Reference<RedstoneLinkableType> register(final String name, final PropertyDispatch.PentaFunction<CompoundTag, Couple<Frequency>, Boolean, HolderLookup.Provider, DimensionPalette, ? extends RedstoneLinkable> factory) {
		return Registry.registerForHolder(CreateBuiltInRegistries.REDSTONE_LINKABLE, Create.asResource(name), new RedstoneLinkableType(factory));
	}

	public static void init() {
	}

	private AllRedstoneLinkables() {
	}
}
