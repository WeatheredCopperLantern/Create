package com.simibubi.create.content.redstone.link.network;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.simibubi.create.content.redstone.link.linkable.IRedstoneLinkable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GlobalRedstoneLinkNetworksManager {

	public Map<ResourceKey<Level>, RedstoneLinkNetwork> networks;
	public Map<UUID, IRedstoneLinkable> linkables;

	private RedstoneLinkNetworksSavedData savedData;

	public void levelLoaded(final LevelAccessor level) {
		final MinecraftServer server = level.getServer();
		if (server == null) return;
		if (level == server.overworld()) {
			this.savedData = RedstoneLinkNetworksSavedData.load(server);
			this.networks = this.savedData.networks;
			this.linkables = this.savedData.linkables;
		} else {
			server.levelKeys().forEach(levelKey -> this.networks.computeIfAbsent(levelKey, u -> new RedstoneLinkNetwork(Objects.requireNonNull(server.getLevel(u)))));
		}
	}

	public void setDirty() {
		this.savedData.setDirty(true);
	}

	public void tick(ServerTickEvent.Pre event) {
		if (event.getServer().tickRateManager().isFrozen() && !event.getServer().tickRateManager().isSteppingForward()) return;
		this.networks.values().forEach(RedstoneLinkNetwork::tick);
	}

	public @Nullable IRedstoneLinkable getLinkable(final UUID uuid) {
		return this.linkables.get(uuid);
	}

	public RedstoneLinkNetwork getNetwork(final Level level) {
		return this.networks.get(level.dimension());
	}

	public RedstoneLinkNetwork getNetwork(final BlockEntity blockEntity) {
		final Level level = blockEntity.getLevel();
		if (level == null) {
			throw new IllegalArgumentException("Blockentity: " + blockEntity + " does not have a level set.");
		}
		return this.getNetwork(level);
	}

	public RedstoneLinkNetwork getNetwork(final Entity entity) {
		return this.getNetwork(entity.level());
	}
}
