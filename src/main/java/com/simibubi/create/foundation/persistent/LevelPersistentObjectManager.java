package com.simibubi.create.foundation.persistent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;

import net.createmod.catnip.nbt.NBTHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LevelPersistentObjectManager extends SavedData {

	private static final String DATA_STORAGE_NAME = "create_persitent_objects";

	private final ServerLevel level;

	private final Map<BlockPos, BlockBoundObject> persistentBlockBoundObjects;
	private final Map<UUID, PersistentObject> persistentObjects;

	public void addPersistentObject(final PersistentObject persistentObject) {
		this.setDirty(true);
		persistentObjects.put(persistentObject.uuid, persistentObject);
		if (persistentObject instanceof final BlockBoundObject bbo) {
			persistentBlockBoundObjects.put(bbo.blockPos, bbo);
		}
	}

	public @Nullable <T extends BlockBoundObject> T getPersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		final Class<T> tClass = type.getPersistentObjectClass();
		BlockBoundObject blockBoundObject = this.getPersistentBlockBoundObject(pos);

		if (!tClass.isInstance(blockBoundObject)) {
			return null;
		}
		return tClass.cast(blockBoundObject);
	}

	public @Nullable BlockBoundObject getPersistentBlockBoundObject(final BlockPos pos) {
		return this.persistentBlockBoundObjects.get(pos);
	}

	public @Nullable <T extends PersistentObject> T getPersistentObject(final UUID uuid, final PersistentObjectType<T> type) {
		final Class<T> tClass = type.getPersistentObjectClass();
		PersistentObject persistentObject = this.getPersistentObject(uuid);

		if (!tClass.isInstance(persistentObject)) {
			return null;
		}
		return tClass.cast(persistentObject);
	}

	public @Nullable PersistentObject getPersistentObject(final UUID uuid) {
		return this.persistentObjects.get(uuid);
	}

	public @Nullable BlockBoundObject removePersistentBlockBoundObject(final BlockPos pos) {
		this.setDirty(true);
		final BlockBoundObject bbo = this.persistentBlockBoundObjects.remove(pos);
		if(bbo != null){
			this.persistentObjects.remove(bbo.uuid);
			bbo.setRemoved();
		}
		return bbo;
	}

	@Override
	public CompoundTag save(final CompoundTag compoundTag, final HolderLookup.Provider provider) {
		this.setDirty(false);
		final ListTag listTag = new ListTag(persistentObjects.size());

		persistentObjects.forEach((uuid, persistentObject) -> {
			final ResourceLocation resourceLocation = CreateBuiltInRegistries.PERSISTENT_OBJECT_TYPE.getKey(persistentObject.getType());
			if (resourceLocation == null) {
				Create.LOGGER.error("{} has no valid PersistentObjectType, it will not be saved", persistentObject.getClass().getName());
				return;
			}

			final CompoundTag tag = new CompoundTag(3);
			tag.putString("ResourceLocation", resourceLocation.toString());
			tag.put("PersistentObject", persistentObject.save(new CompoundTag(), provider));

			listTag.add(tag);
		});

		compoundTag.put(DATA_STORAGE_NAME, listTag);

		return compoundTag;
	}

	private void load(final CompoundTag tag, final HolderLookup.Provider provider) {
		if (!(tag.get(DATA_STORAGE_NAME) instanceof final ListTag listTag)) throw new RuntimeException("Unexpected Tag Type");

		listTag.forEach(tag1 -> {
			if (!(tag1 instanceof final CompoundTag compoundTag)) throw new RuntimeException("Unexpected Tag Type");
			final ResourceLocation resourceLocation = NBTHelper.readResourceLocation(compoundTag, "ResourceLocation");
			final Optional<PersistentObjectType<?>> typeOptional = CreateBuiltInRegistries.PERSISTENT_OBJECT_TYPE.getOptional(resourceLocation);
			if (typeOptional.isEmpty()) {
				Create.LOGGER.error("PersistentObjectType with name {} does not exist, PersistentObject will not be loaded", resourceLocation);
				return;
			}
			typeOptional.ifPresent(persistentObjectType -> {
				Create.PERSISTENT_OBJECTS.getManager(this.level.dimension()).addPersistentObject(persistentObjectType.load(compoundTag.getCompound("PersistentObject"), provider, level));
			});
		});
	}

	public void load() {
		level.getDataStorage().computeIfAbsent(LevelPersistentObjectManager.factory(level), DATA_STORAGE_NAME);
	}

	private static SavedData.@NonNull Factory<LevelPersistentObjectManager> factory(final ServerLevel level) {
		return new SavedData.Factory<>(() -> new LevelPersistentObjectManager(level), (compoundTag, provider) -> {
			final LevelPersistentObjectManager manager = new LevelPersistentObjectManager(level);
			manager.load(compoundTag, provider);
			return manager;
		});
	}

	protected LevelPersistentObjectManager(ServerLevel level) {
		this.level = level;
		this.persistentObjects = HashMap.newHashMap(16);
		this.persistentBlockBoundObjects = HashMap.newHashMap(16);
		Create.PERSISTENT_OBJECTS.setManager(level.dimension(), this);
	}
}