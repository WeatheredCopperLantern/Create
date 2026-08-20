package com.simibubi.create.infrastructure.gametest.blockEntities;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.BBOBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.persistent.PersistentObjectType;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.AllTestBlockBoundObjectTypes;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.DummyBlockBoundObject;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DummyBBOBlockEntity extends BBOBlockEntity<DummyBlockBoundObject> {

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

	}

	@Override
	public PersistentObjectType<DummyBlockBoundObject> getBlockBoundObjectType() {
		return AllTestBlockBoundObjectTypes.DUMMY.get();
	}

	public DummyBBOBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
	}

	@Override
	protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		this.withBlockBoundObjectDo(bbo -> tag.putInt("Value", bbo.getValue()));
	}

	@Override
	protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(tag, registries, clientPacket);
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Override
	public void remove() {
		super.remove();
	}
}
