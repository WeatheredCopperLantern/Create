package com.simibubi.create.foundation.persistent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class GlobalPersistentObjectManager {

	private final Map<ResourceKey<Level>, LevelPersistentObjectManager> managers;

	public LevelPersistentObjectManager getManager(final Level level) {
		return this.managers.get(level.dimension());
	}

	protected void setManager(final ResourceKey<Level> levelResourceKey, final LevelPersistentObjectManager manager) {
		this.managers.put(levelResourceKey, manager);
	}

	public LevelPersistentObjectManager getManager(final ResourceKey<Level> levelResourceKey) {
		return this.managers.get(levelResourceKey);
	}

	public Set<Map.Entry<ResourceKey<Level>, LevelPersistentObjectManager>> getManagers() {
		return this.managers.entrySet();
	}

	public void levelLoaded(final LevelAccessor level) {
		if (level instanceof final ServerLevel serverLevel) {
			LevelPersistentObjectManager.load(serverLevel);
		}
	}

	public void tick(ServerTickEvent.Pre event) {
		if (event.getServer().tickRateManager().isFrozen() && !event.getServer().tickRateManager().isSteppingForward()) return;
		this.managers.values().forEach(LevelPersistentObjectManager::tick);
	}

	public GlobalPersistentObjectManager() {
		this.managers = HashMap.newHashMap(3);
	}
}
