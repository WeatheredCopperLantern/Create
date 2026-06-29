package com.simibubi.create.content.redstone.link;

import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GlobalRedstoneLinkNetworksManager {

	public Map<ResourceKey<Level>, RedstoneLinkNetwork> networks;
	public Map<UUID, RedstoneLinkable> linkables;

	private RedstoneLinkNetworksSavedData savedData;

	public void levelLoaded(final @NonNull LevelAccessor level) {
		final MinecraftServer server = level.getServer();
		if (server == null || server.overworld() != level) return;
		this.savedData = RedstoneLinkNetworksSavedData.load(server);
		this.networks = this.savedData.networks;
		this.linkables = this.savedData.linkables;
	}

	public void setDirty() {
		this.savedData.setDirty(true);
	}

	public void tick() {
		this.networks.values().forEach(RedstoneLinkNetwork::tick);
	}

	public @Nullable RedstoneLinkable getLinkable(final @NonNull UUID uuid) {
		return this.linkables.get(uuid);
	}

	public @NonNull RedstoneLinkNetwork getNetwork(final @NonNull Level level) {
		return this.networks.get(level.dimension());
	}

	public @NonNull RedstoneLinkNetwork getNetwork(final @NonNull BlockEntity blockEntity) {
		final Level level = blockEntity.getLevel();
		if (level == null) {
			throw new IllegalArgumentException("Blockentity: " + blockEntity + " does not have a level set.");
		}
		return this.getNetwork(level);
	}

	public @NonNull RedstoneLinkNetwork getNetwork(final @NonNull Entity entity) {
		return this.getNetwork(entity.level());
	}
}
