package com.simibubi.create.content.redstone.link;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.nbt.NBTHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import org.jspecify.annotations.NonNull;

public class RedstoneLinkNetworksSavedData extends SavedData {

	public Map<ResourceKey<Level>, RedstoneLinkNetwork> networks;
	public Map<UUID, RedstoneLinkable> linkables;

	@Override
	public @NonNull CompoundTag save(final @NonNull CompoundTag nbt, final HolderLookup.@NonNull Provider registries) {
		final GlobalRedstoneLinkNetworksManager linkNetworks = Create.REDSTONE_LINK_NETWORK;
		final DimensionPalette dimensions = new DimensionPalette();

		nbt.put("Networks", NBTHelper.writeCompoundList(linkNetworks.networks.entrySet(), set -> {
			final CompoundTag networkNBT = new CompoundTag(2);
			networkNBT.putInt("D", dimensions.encode(set.getKey()));
			networkNBT.put("Network", set.getValue().write(registries, dimensions));
			return networkNBT;
		}));

		nbt.putInt("Linkables", linkNetworks.linkables.size());

		dimensions.write(nbt);
		this.setDirty(false);
		return nbt;
	}

	private static @NonNull RedstoneLinkNetworksSavedData load(final CompoundTag nbt, final HolderLookup.Provider registries, final @NonNull MinecraftServer server) {
		final Set<ResourceKey<Level>> levelKeys = server.levelKeys();
		final Map<ResourceKey<Level>, RedstoneLinkNetwork> networks = new HashMap<>((int) Math.ceil(levelKeys.size() / 0.7), 0.7f);
		final Map<UUID, RedstoneLinkable> linkables = new HashMap<>((int) Math.ceil(nbt.getInt("Linkables") / 0.7), 0.7f);

		final DimensionPalette dimensions = DimensionPalette.read(nbt);

		NBTHelper.iterateCompoundList(nbt.getList("Networks", Tag.TAG_COMPOUND), tag -> {
			final RedstoneLinkNetwork network = RedstoneLinkNetwork.read(tag.getCompound("Network"), registries, dimensions, linkables);
			networks.put(dimensions.decode(tag.getInt("D")), network);
		});

		levelKeys.forEach(levelKey -> networks.computeIfAbsent(levelKey, u -> new RedstoneLinkNetwork()));

		return new RedstoneLinkNetworksSavedData(networks, linkables);
	}

	public static @NonNull RedstoneLinkNetworksSavedData load(final @NonNull MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(RedstoneLinkNetworksSavedData.factory(server), "create_redstone_link_network");
	}

	private static SavedData.@NonNull Factory<RedstoneLinkNetworksSavedData> factory(final @NonNull MinecraftServer server) {
		return new SavedData.Factory<>(() -> new RedstoneLinkNetworksSavedData(server), (compoundTag, provider) -> RedstoneLinkNetworksSavedData.load(compoundTag, provider, server));
	}

	private RedstoneLinkNetworksSavedData(final Map<ResourceKey<Level>, RedstoneLinkNetwork> networks, final Map<UUID, RedstoneLinkable> linkables) {
		this.networks = networks;
		this.linkables = linkables;
	}

	private RedstoneLinkNetworksSavedData(final @NonNull MinecraftServer server) {
		final Set<ResourceKey<Level>> levelKeys = server.levelKeys();
		this.networks = new HashMap<>((int) Math.ceil(levelKeys.size() / 0.7), 0.7f);
		levelKeys.forEach(levelKey -> this.networks.computeIfAbsent(levelKey, u -> new RedstoneLinkNetwork()));

		this.linkables = new HashMap<>((int) Math.ceil(64 / 0.7), 0.7f);
		this.setDirty(true);
	}
}
