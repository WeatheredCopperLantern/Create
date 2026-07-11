package com.simibubi.create.content.redstone.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.interfaces.ITickingLinkable;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.UUID;

public abstract class RedstoneEntityLinkable extends RedstoneLinkable implements ITickingLinkable {

	private static final HashMap<UUID, RedstoneEntityLinkable> missing = new HashMap<>(2);

	private Entity entity;
	private UUID uuid;
	private Vector3fc cachedPosition;

	@Override
	public Vector3fc getTransmissionPosition() {
		return cachedPosition;
	}

	@Override
	public void readAdditional(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
		this.uuid = nbt.getUUID("uuid");
		final ListTag tag = nbt.getList("position", Tag.TAG_COMPOUND);
		this.cachedPosition = new Vector3f(tag.getFloat(0), tag.getFloat(1), tag.getFloat(2));

		missing.put(this.uuid, this);
	}

	public static void handleSpawn(EntityJoinLevelEvent event) {
		if (!event.loadedFromDisk() || event.getLevel().isClientSide) return;

		RedstoneEntityLinkable linkable = missing.remove(event.getEntity().getUUID());
		if (linkable != null) linkable.setEntity(event.getEntity());
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
		this.uuid = entity.getUUID();
		this.cachedPosition = entity.position().toVector3f();
	}

	@Override
	public void tick() {
		if (entity == null) return;
		if (entity.isRemoved()) {
			Entity.RemovalReason reason = entity.getRemovalReason();
			assert reason != null;

			if (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
				if (reason == Entity.RemovalReason.CHANGED_DIMENSION) {
					setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(entity));
				}else {
					this.network.removeLinkable(this);
					this.network = null;
				}
			}
		} else {
			Vector3fc pos = entity.position().toVector3f();
			if (pos.distanceSquared(this.cachedPosition) > 0.5 * 0.5) {
				RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
				this.cachedPosition = pos;
				this.network.linkMoved(this, snapshot);
			}
		}
	}

	@Override
	public void writeAdditional(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
		nbt.putUUID("uuid", entity.getUUID());
		final ListTag tag = new ListTag();
		tag.add(FloatTag.valueOf(this.cachedPosition.x()));
		tag.add(FloatTag.valueOf(this.cachedPosition.y()));
		tag.add(FloatTag.valueOf(this.cachedPosition.z()));
		nbt.put("position", tag);
	}

	public RedstoneEntityLinkable(final CompoundTag nbt, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(nbt, channel, receiver, registries, dimensions, network);
	}

	public RedstoneEntityLinkable(Couple<Frequency> channel, Entity entity) {
		super(channel);
		setEntity(entity);
		setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(entity));
	}
}
