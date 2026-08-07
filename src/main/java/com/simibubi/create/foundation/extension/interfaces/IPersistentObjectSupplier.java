package com.simibubi.create.foundation.extension.interfaces;

import java.util.UUID;

import com.simibubi.create.foundation.persistent.PersistentBlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

public interface IPersistentObjectSupplier {

	default @Nullable PersistentBlockBoundObject create$getPersistentBlockBoundObject(final BlockPos pos) {
		return null;
	}

	default @Nullable PersistentObject create$getPersistentObject(final UUID uuid) {
		return null;
	}

	default @Nullable <T extends PersistentBlockBoundObject> T create$getPersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		return null;
	}

	default @Nullable <T extends PersistentBlockBoundObject> T create$getOrCreatePersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		return null;
	}

	default @Nullable <T extends PersistentObject> T create$getPersistentObject(final UUID uuid, final PersistentObjectType<T> type) {
		return null;
	}
}
