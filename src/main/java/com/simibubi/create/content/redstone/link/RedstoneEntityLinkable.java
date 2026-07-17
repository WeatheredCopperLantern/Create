package com.simibubi.create.content.redstone.link;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.interfaces.ITickingLinkable;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableSnapshot;
import com.simibubi.create.content.redstone.link.network.RedstoneLinkNetwork;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class RedstoneEntityLinkable extends RedstoneLinkable implements ITickingLinkable {

	private static final HashMap<UUID, RedstoneEntityLinkable> missing = new HashMap<>(2);

	protected Entity entity;
	protected UUID uuid;
	protected Vector3fc cachedPosition;

	@Override
	public Vector3fc getTransmissionPosition() {
		return this.cachedPosition;
	}

	@Override
	public void representationUnloaded() {

	}

	@Override
	protected boolean shouldSetFrequency(final boolean first, final Frequency frequency) {
		return true;
	}

	@Override
	protected void onFrequencyChanged(final boolean first) {

	}

	@Override
	protected void onModeChanged(final RedstoneLinkableSnapshot snapshot) {

	}

	@Override
	protected void onSignalChanged() {

	}

	@Override
	public void readAdditional(final CompoundTag tag, final HolderLookup.@NonNull Provider registries, final DimensionPalette dimensions) {
		this.uuid = tag.getUUID("uuid");
		final ListTag listTag = tag.getList("position", Tag.TAG_COMPOUND);
		this.cachedPosition = new Vector3f(listTag.getFloat(0), listTag.getFloat(1), listTag.getFloat(2));

		RedstoneEntityLinkable.missing.put(this.uuid, this);
	}

	public static void handleSpawn(final EntityJoinLevelEvent event) {
		if (!event.loadedFromDisk() || event.getLevel().isClientSide) return;

		final RedstoneEntityLinkable linkable = RedstoneEntityLinkable.missing.remove(event.getEntity().getUUID());
		if (linkable != null) linkable.setEntity(event.getEntity());
	}

	public void setEntity(final Entity entity) {
		this.entity = entity;
		this.uuid = entity.getUUID();
		this.cachedPosition = entity.position().toVector3f();
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(this.entity));
	}

	@Override
	public void tick() {
		if (this.entity == null || this.network == null) return;
		if (this.entity.isRemoved()) {
			final Entity.RemovalReason reason = this.entity.getRemovalReason();
			assert reason != null;

			if (reason.shouldDestroy() || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
				if (reason == Entity.RemovalReason.CHANGED_DIMENSION) {
					this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(this.entity));
				}else {
					this.network.removeLinkable(this);
					this.network = null;
				}
			}
		} else {
			final Vector3fc pos = this.entity.position().toVector3f();
			if (pos.distanceSquared(this.cachedPosition) > 0.5 * 0.5) {
				final RedstoneLinkableSnapshot snapshot = RedstoneLinkableSnapshot.of(this);
				this.cachedPosition = pos;
				this.network.linkMoved(this, snapshot);
			}
		}
	}

	@Override
	public void writeAdditional(final CompoundTag tag, final HolderLookup.@NonNull Provider registries, final DimensionPalette dimensions) {
		tag.putUUID("uuid", this.entity.getUUID());
		final ListTag listTag = new ListTag();
		listTag.add(FloatTag.valueOf(this.cachedPosition.x()));
		listTag.add(FloatTag.valueOf(this.cachedPosition.y()));
		listTag.add(FloatTag.valueOf(this.cachedPosition.z()));
		tag.put("position", listTag);
	}

	protected RedstoneEntityLinkable(final CompoundTag tag, final Couple<Frequency> channel, final boolean receiver, final HolderLookup.Provider registries, final DimensionPalette dimensions, final RedstoneLinkNetwork network) {
		super(tag, channel, receiver, registries, dimensions, network);
	}

	protected RedstoneEntityLinkable(final Couple<Frequency> channel, final Entity entity) {
		super(channel);
		this.setEntity(entity);
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(entity));
	}
}
