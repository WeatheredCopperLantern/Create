package com.simibubi.create.infrastructure.gametest.blockBoundObjects;

import com.simibubi.create.foundation.persistent.BlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;
import com.simibubi.create.infrastructure.gametest.blocks.AllTestBlocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DummyBlockBoundObject extends BlockBoundObject {

	int value;

	public void setValue(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public Tag save(final CompoundTag compoundTag, final HolderLookup.Provider provider) {
		super.save(compoundTag, provider);
		compoundTag.putInt("value", this.value);
		return compoundTag;
	}

	@Override
	public PersistentObjectType<?> getType() {
		return AllTestBlockBoundObjectTypes.DUMMY.get();
	}

	@Override
	public boolean isValidBlockState(final BlockState state) {
		return state.getBlock() == AllTestBlocks.BBO.get();
	}

	@Override
	public void setBlockState(final BlockState state) {
		super.setBlockState(state);
		this.setValue(this.level.random.nextInt());
	}

	public static DummyBlockBoundObject create(final ServerLevel level, final BlockPos pos, final BlockState state) {
		final DummyBlockBoundObject dummyBlockBoundObject = new DummyBlockBoundObject(pos, level);
		dummyBlockBoundObject.setValue(level.random.nextInt());
		return dummyBlockBoundObject;
	}

	public static DummyBlockBoundObject create(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		final DummyBlockBoundObject dummyBlockBoundObject = new DummyBlockBoundObject(compoundTag, registries, serverLevel);
		return dummyBlockBoundObject;
	}

	protected DummyBlockBoundObject(final BlockPos blockPos, final ServerLevel serverLevel) {
		super(AllTestBlocks.BBO.get(), blockPos, serverLevel.getBlockState(blockPos), serverLevel);
	}

	protected DummyBlockBoundObject(final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		super(AllTestBlocks.BBO.get(), compoundTag, registries, serverLevel);
		this.value = compoundTag.getInt("value");
	}
}
