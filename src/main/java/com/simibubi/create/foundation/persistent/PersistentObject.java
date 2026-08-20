package com.simibubi.create.foundation.persistent;

import java.util.UUID;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PersistentObject {

	public final UUID uuid;
	public final ServerLevel level;

	@OverridingMethodsMustInvokeSuper
	public Tag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		tag.putUUID("UUID", this.uuid);
		return tag;
	}

	public void setRemoved() {
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
}
