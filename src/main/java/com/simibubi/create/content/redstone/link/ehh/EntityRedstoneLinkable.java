package com.simibubi.create.content.redstone.link.ehh;

import com.simibubi.create.Create;
import net.createmod.catnip.data.Couple;
import com.simibubi.create.content.redstone.link.ehh.RedstoneLinkNetworkHandler.Frequency;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public abstract class EntityRedstoneLinkable extends AbstractRedstoneLinkable {

	private final Entity entity;
	private Vec3 lastPos;

	public EntityRedstoneLinkable(Couple<Frequency> channel, Mode mode, IntConsumer signalCallback,
		IntSupplier transmission, Entity entity) {
		this(channel.getFirst(), channel.getSecond(), mode, signalCallback, transmission, entity);
	}

	public EntityRedstoneLinkable(Frequency first, Frequency last, Mode mode, IntConsumer signalCallback,
		IntSupplier transmission, Entity entity) {
		super(first, last, mode, signalCallback, transmission);
		this.entity = entity;
		this.lastPos = getLocation();
		setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(entity));
	}

	public void tick() {
		if (getNetwork() == null) return;
		if (entity == null || !entity.isAlive() || !entity.isAddedToLevel() || entity.isRemoved()) {
			getNetwork().remove(this);
		} else if (!lastPos.equals(Vec3.atCenterOf(entity.blockPosition()))) {
			this.getNetwork().linkMoved(this, lastPos);
			lastPos = Vec3.atCenterOf(entity.blockPosition());
		}
	}

	@Override
	public Vec3 getLocation() {
		return Vec3.atCenterOf(entity.blockPosition());
	}

	@Override
	public Level getLevel() {
		return entity.level();
	}
}
