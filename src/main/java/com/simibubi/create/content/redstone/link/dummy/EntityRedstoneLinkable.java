package com.simibubi.create.content.redstone.link.dummy;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.createmod.catnip.data.Couple;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class EntityRedstoneLinkable extends AbstractRedstoneLinkable {

	private final Entity entity;
	private Vec3 lastPos;

	protected EntityRedstoneLinkable(final Couple<RedstoneLinkNetworkHandler.Frequency> channel, final Mode mode, final IntConsumer signalCallback, final IntSupplier transmission, final Entity entity) {
		this(channel.getFirst(), channel.getSecond(), mode, signalCallback, transmission, entity);
	}

	protected EntityRedstoneLinkable(final RedstoneLinkNetworkHandler.Frequency first, final RedstoneLinkNetworkHandler.Frequency last, final Mode mode, final IntConsumer signalCallback, final IntSupplier transmission, final Entity entity) {
		super(first, last, mode, signalCallback, transmission);
		this.entity = entity;
		this.lastPos = this.getLocation();
		//setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(entity));
	}

	public void tick() {
		if (this.getNetwork() == null) return;
		if (this.entity == null || !this.entity.isAlive() || !this.entity.isAddedToLevel() || this.entity.isRemoved()) {
			this.getNetwork().remove(this);
		} else if (!this.lastPos.equals(Vec3.atCenterOf(this.entity.blockPosition()))) {
			this.getNetwork().linkMoved(this, this.lastPos);
			this.lastPos = Vec3.atCenterOf(this.entity.blockPosition());
		}
	}

	@Override
	public Vec3 getLocation() {
		return Vec3.atCenterOf(this.entity.blockPosition());
	}

	@Override
	public Level getLevel() {
		return this.entity.level();
	}
}
