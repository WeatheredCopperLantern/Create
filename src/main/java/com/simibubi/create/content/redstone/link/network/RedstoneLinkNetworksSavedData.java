package com.simibubi.create.content.redstone.link.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkable;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.nbt.NBTHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.NonNull;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkNetworksSavedData extends SavedData {

	public Map<ResourceKey<Level>, RedstoneLinkNetwork> networks;
	public Map<UUID, RedstoneLinkable> linkables;

	@Override
	public CompoundTag save(final CompoundTag nbt, final HolderLookup.@NonNull Provider registries) {
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

	private static RedstoneLinkNetworksSavedData load(final CompoundTag nbt, final HolderLookup.Provider registries, final MinecraftServer server) {
		final Set<ResourceKey<Level>> levelKeys = server.levelKeys();
		final Map<ResourceKey<Level>, RedstoneLinkNetwork> networks = new HashMap<>((int) Math.ceil(levelKeys.size() / 0.7), 0.7f);
		final Map<UUID, RedstoneLinkable> linkables = new HashMap<>((int) Math.ceil(nbt.getInt("Linkables") / 0.7), 0.7f);

		final DimensionPalette dimensions = DimensionPalette.read(nbt);

		NBTHelper.iterateCompoundList(nbt.getList("Networks", Tag.TAG_COMPOUND), tag -> {
			final ResourceKey<Level> levelKey = dimensions.decode(tag.getInt("D"));
			final ServerLevel level = server.getLevel(levelKey);
			assert level != null;
			final RedstoneLinkNetwork network = RedstoneLinkNetwork.read(tag.getCompound("Network"), registries, dimensions, linkables, level);
			networks.put(dimensions.decode(tag.getInt("D")), network);
		});

		levelKeys.forEach(levelKey -> networks.computeIfAbsent(levelKey, u -> new RedstoneLinkNetwork(Objects.requireNonNull(server.getLevel(u)))));

		return new RedstoneLinkNetworksSavedData(networks, linkables);
	}

	public static RedstoneLinkNetworksSavedData load(final MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(RedstoneLinkNetworksSavedData.factory(server), "create_redstone_link_network");
	}

	private static SavedData.@NonNull Factory<RedstoneLinkNetworksSavedData> factory(final MinecraftServer server) {
		return new SavedData.Factory<>(() -> new RedstoneLinkNetworksSavedData(server), (compoundTag, provider) -> RedstoneLinkNetworksSavedData.load(compoundTag, provider, server));
	}

	private RedstoneLinkNetworksSavedData(final Map<ResourceKey<Level>, RedstoneLinkNetwork> networks, final Map<UUID, RedstoneLinkable> linkables) {
		this.networks = networks;
		this.linkables = linkables;
	}

	private RedstoneLinkNetworksSavedData(final MinecraftServer server) {
		final Set<ResourceKey<Level>> levelKeys = server.levelKeys();
		this.networks = new HashMap<>((int) Math.ceil(levelKeys.size() / 0.7), 0.7f);
		levelKeys.forEach(levelKey -> this.networks.computeIfAbsent(levelKey, u -> new RedstoneLinkNetwork(Objects.requireNonNull(server.getLevel(u)))));

		this.linkables = new HashMap<>((int) 92.0, 0.7f);
		this.setDirty(true);
	}
}
