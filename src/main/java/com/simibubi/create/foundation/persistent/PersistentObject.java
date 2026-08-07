package com.simibubi.create.foundation.persistent;

import java.util.UUID;
import java.util.function.Consumer;

import com.simibubi.create.Create;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PersistentObject {

	public final UUID uuid;
	public final ServerLevel level;
	public @Nullable LevelPersistentObjectManager manager;
	private boolean singleTickScheduled = false;

	@OverridingMethodsMustInvokeSuper
	public void tick() {
		singleTickScheduled = false;
	}

	public void awake() {
		withManagerDo(manager -> manager.awakeObject(this));
	}

	public void sleep() {
		withManagerDo(manager -> manager.sleepObject(this));
	}

	public void tickOnce() {
		if (singleTickScheduled) return;
		withManagerDo(manager -> {
			manager.tickObjectOnce(this);
			singleTickScheduled = true;
		});
	}

	@OverridingMethodsMustInvokeSuper
	public Tag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		tag.putUUID("UUID", this.uuid);
		return tag;
	}

	public abstract PersistentObjectType<?> getType();

	protected PersistentObject(final CompoundTag compoundTag, final ServerLevel serverLevel) {
		this.level = serverLevel;
		this.uuid = compoundTag.getUUID("UUID");
	}

	protected PersistentObject(final ServerLevel serverLevel) {
		this.level = serverLevel;
		this.uuid = UUID.randomUUID();
	}

	@OverridingMethodsMustInvokeSuper
	protected void initialize() {
		this.manager = Create.PERSISTENT_OBJECTS.getManager(this.level);
		this.manager.addObject(this);
	}

	protected void setDirty() {
		withManagerDo(SavedData::setDirty);
	}

	protected void withManagerDo(Consumer<LevelPersistentObjectManager> action) {
		if (this.manager != null) action.accept(this.manager);
	}
}
