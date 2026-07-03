package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRedstoneLinkables;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.createmod.catnip.data.Couple;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class RedstoneLinkLinkable extends RedstoneLinkable {

	private RedstoneLinkBlockEntity blockEntity;
	private Vector3fc cachedPosition;

	@Override
	public void readAdditional(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
		ListTag tag = nbt.getList("position", Tag.TAG_COMPOUND);
		this.cachedPosition = new Vector3f(tag.getFloat(0), tag.getFloat(1), tag.getFloat(2));
	}

	@Override
	public void writeAdditional(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
		ListTag tag = new ListTag();
		tag.add(FloatTag.valueOf(this.cachedPosition.x()));
		tag.add(FloatTag.valueOf(this.cachedPosition.y()));
		tag.add(FloatTag.valueOf(this.cachedPosition.z()));
		nbt.put("position", tag);
	}

	@Override
	public RedstoneLinkableType getType() {
		return AllRedstoneLinkables.REDSTONE_LINK.value();
	}

	@Override
	public Vector3fc getTransmissionPosition() {
		return this.cachedPosition;
	}

	@Override
	protected void onModeChanged(RedstoneLinkableSnapshot snapshot) {
		withBeDo(redstoneLinkBlockEntity -> {
			AllBlocks.REDSTONE_LINK.get().updateFromLinkable(redstoneLinkBlockEntity.getLevel(), redstoneLinkBlockEntity.getBlockPos());
		});
	}

	private void withBeDo(Consumer<RedstoneLinkBlockEntity> action) {
		if (this.blockEntity != null) action.accept(this.blockEntity);
	}

	@Override
	protected boolean shouldSetMode(boolean receiver) {
		return true;
	}

	public RedstoneLinkLinkable(CompoundTag nbt, Couple<Frequency> channel, boolean receiver, HolderLookup.Provider registries, DimensionPalette dimensions, RedstoneLinkNetwork network) {
		super(nbt, channel, receiver, registries, dimensions, network);
	}

	public RedstoneLinkLinkable(final Couple<Frequency> channel, final RedstoneLinkBlockEntity be) {
		super(channel);
		this.setBlockEntity(be);
		assert be.getLevel() != null;
		this.setNetwork(Create.REDSTONE_LINK_NETWORK.getNetwork(be.getLevel()));
	}

	public void setBlockEntity(RedstoneLinkBlockEntity redstoneLinkBlockEntity) {
		this.blockEntity = redstoneLinkBlockEntity;
		this.cachedPosition = redstoneLinkBlockEntity.getBlockPos().getCenter().toVector3f();
	}
}
