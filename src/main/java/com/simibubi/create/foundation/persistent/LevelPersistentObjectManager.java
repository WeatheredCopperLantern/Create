package com.simibubi.create.foundation.persistent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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

	//region Fields

	private static final String DATA_STORAGE_NAME;

	static {
		DATA_STORAGE_NAME = "create_persitent_objects";
	}

	private final Map<BlockPos, PersistentBlockBoundObject> persistentBlockBoundObjects;
	private final Map<UUID, PersistentObject> persistentObjects;
	private final ServerLevel level;

	private final Queue<PersistentObject> singleTickQueue = new ArrayDeque<>(16);
	private final Collection<PersistentObject> tickingObjects = HashSet.newHashSet(16);
	private final Queue<PersistentObject> tickingAdds = new ArrayDeque<>(4);
	private final Queue<PersistentObject> tickingRemovals = new ArrayDeque<>(4);

	//endregion

	public void tick() {
		while (!this.tickingAdds.isEmpty()) {
			this.tickingObjects.add(this.tickingAdds.remove());
		}
		while (!this.tickingRemovals.isEmpty()) {
			this.tickingObjects.remove(this.tickingRemovals.remove());
		}
		while (!this.singleTickQueue.isEmpty()) {
			this.singleTickQueue.remove().tick();
		}
		this.tickingObjects.forEach(PersistentObject::tick);
	}

	public void tickObjectOnce(final PersistentObject persistentObject) {
		singleTickQueue.add(persistentObject);
	}

	public void sleepObject(final PersistentObject persistentObject) {
		tickingRemovals.add(persistentObject);
	}

	public void awakeObject(final PersistentObject persistentObject) {
		tickingAdds.add(persistentObject);
	}

	private static SavedData.@NonNull Factory<LevelPersistentObjectManager> factory(final ServerLevel level) {
		return new SavedData.Factory<>(() -> new LevelPersistentObjectManager(level), (compoundTag, provider) -> new LevelPersistentObjectManager(compoundTag, provider, level));
	}

	public @Nullable <T extends PersistentBlockBoundObject> T getOrCreatePersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		final Class<T> tClass = type.getPersistentObjectClass();
		PersistentBlockBoundObject persistentBlockBoundObject = this.getPersistentBlockBoundObject(pos);

		if (persistentBlockBoundObject == null) {
			return type.create(level, pos);
		} else if (!tClass.isInstance(persistentBlockBoundObject)) {
			return null;
		}
		return tClass.cast(persistentBlockBoundObject);
	}

	public @Nullable <T extends PersistentBlockBoundObject> T getPersistentBlockBoundObject(final BlockPos pos, final PersistentObjectType<T> type) {
		final Class<T> tClass = type.getPersistentObjectClass();
		PersistentBlockBoundObject persistentBlockBoundObject = this.getPersistentBlockBoundObject(pos);

		if (!tClass.isInstance(persistentBlockBoundObject)) {
			return null;
		}
		return tClass.cast(persistentBlockBoundObject);
	}

	public @Nullable PersistentBlockBoundObject getPersistentBlockBoundObject(final BlockPos pos) {
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

	public static void load(final ServerLevel level) {
		level.getDataStorage().computeIfAbsent(LevelPersistentObjectManager.factory(level), DATA_STORAGE_NAME);
	}

	@Override
	public CompoundTag save(final CompoundTag compoundTag, final HolderLookup.Provider registries) {
		this.setDirty(false);
		final ListTag listTag = new ListTag(persistentObjects.size());

		persistentObjects.forEach((uuid, persistentObject) -> {
			final ResourceLocation resourceLocation = CreateBuiltInRegistries.PERSISTENT_OBJECT_TYPE.getKey(persistentObject.getType());
			//TODO: show warning/error
			if (resourceLocation == null) {
				Create.LOGGER.error("{} has no valid PersistentObjectType, it will not be saved", persistentObject.getClass().getName());
				return;
			}

			final CompoundTag tag = new CompoundTag(3);
			tag.putString("ResourceLocation", resourceLocation.toString());
			tag.put("PersistentObject", persistentObject.save(new CompoundTag(), registries));

			listTag.add(tag);
		});

		compoundTag.put(DATA_STORAGE_NAME, listTag);

		return compoundTag;
	}

	private LevelPersistentObjectManager(ServerLevel level) {
		this.level = level;
		this.persistentObjects = HashMap.newHashMap(16);
		this.persistentBlockBoundObjects = HashMap.newHashMap(16);
		Create.PERSISTENT_OBJECTS.setManager(level.dimension(), this);
	}

	private LevelPersistentObjectManager(final CompoundTag tag, final HolderLookup.Provider registries, final ServerLevel level) {
		Create.LOGGER.warn("LevelPersistentObjectManager loading");
		if (!(tag.get(DATA_STORAGE_NAME) instanceof final ListTag listTag)) throw new RuntimeException("Unexpected Tag Type");

		this.level = level;
		this.persistentObjects = HashMap.newHashMap(listTag.size());
		this.persistentBlockBoundObjects = HashMap.newHashMap(16);
		Create.PERSISTENT_OBJECTS.setManager(level.dimension(), this);

		listTag.forEach(tag1 -> {
			if (!(tag1 instanceof final CompoundTag compoundTag)) throw new RuntimeException("Unexpected Tag Type");
			final ResourceLocation resourceLocation = NBTHelper.readResourceLocation(compoundTag, "ResourceLocation");
			final Optional<PersistentObjectType<?>> typeOptional = CreateBuiltInRegistries.PERSISTENT_OBJECT_TYPE.getOptional(resourceLocation);
			if (typeOptional.isEmpty()) {
				Create.LOGGER.error("PersistentObjectType with name {} does not exist, PersistentObject will not be loaded", resourceLocation);
				return;
			}
			typeOptional.ifPresent(persistentObjectType -> {
				persistentObjectType.load(compoundTag.getCompound("PersistentObject"), registries, level);
			});
		});
	}

	public void addObject(final PersistentObject persistentObject) {
		this.persistentObjects.put(persistentObject.uuid, persistentObject);
		if (persistentObject instanceof PersistentBlockBoundObject persistentBlockBoundObject) {
			this.persistentBlockBoundObjects.put(persistentBlockBoundObject.blockPos, persistentBlockBoundObject);
		}
		this.setDirty();
	}

	public void removeObject(final PersistentObject persistentObject) {
		this.persistentObjects.remove(persistentObject.uuid);
		if (persistentObject instanceof PersistentBlockBoundObject persistentBlockBoundObject) {
			this.persistentBlockBoundObjects.remove(persistentBlockBoundObject.blockPos);
		}
		this.setDirty();
	}
}
