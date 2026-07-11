package com.simibubi.create;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.link.*;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import com.simibubi.create.content.redstone.link.interfaces.HexaFunction;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;

public final class AllRedstoneLinkables {

	public static final Holder.Reference<RedstoneLinkableType> REDSTONE_LINK = AllRedstoneLinkables.register("redstone_link", RedstoneLinkLinkable::new);
	public static final Holder.Reference<RedstoneLinkableType> REDSTONE_ENTITY = AllRedstoneLinkables.register("redstone_entity", RedstoneEntityLinkableImp::new);

	public static final Holder.Reference<RedstoneLinkableType> REDSTONE_CONTROLLER = AllRedstoneLinkables.register("redstone_controller", LinkedControllerServerHandler.RedstoneControllerLinkable::new);

	private static Holder.Reference<RedstoneLinkableType> register(final String name, final HexaFunction<CompoundTag, Couple<Frequency>, Boolean, HolderLookup.Provider, DimensionPalette, RedstoneLinkNetwork, ? extends RedstoneLinkable> factory) {
		return Registry.registerForHolder(CreateBuiltInRegistries.REDSTONE_LINKABLE, Create.asResource(name), new RedstoneLinkableType(factory));
	}

	public static void init() {
	}

	private AllRedstoneLinkables() {
	}
}
