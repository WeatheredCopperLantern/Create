package com.simibubi.create.foundation.persistent;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.ApiStatus;

public class GlobalPersistentObjectManager {

	private final Map<ResourceKey<Level>, LevelPersistentObjectManager> managers;

	public void tick(final ServerTickEvent.Pre event) {

	}

	public LevelPersistentObjectManager getManager(final ResourceKey<Level> dimension) {
		return this.managers.get(dimension);
	}

	protected void setManager(final ResourceKey<Level> dimension, final LevelPersistentObjectManager manager) {
		this.managers.put(dimension, manager);
	}

	public void levelLoaded(final LevelAccessor level) {
		if (level instanceof final ServerLevel serverLevel) {
			final LevelPersistentObjectManager manager = new LevelPersistentObjectManager(serverLevel);
			this.managers.put(serverLevel.dimension(), manager);
			manager.load();
		}
	}

	public GlobalPersistentObjectManager() {
		this.managers = HashMap.newHashMap(3);
	}
}