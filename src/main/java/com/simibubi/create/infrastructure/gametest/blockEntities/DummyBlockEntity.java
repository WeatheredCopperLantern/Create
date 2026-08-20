package com.simibubi.create.infrastructure.gametest.blockEntities;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DummyBlockEntity extends SmartBlockEntity {

	private int value;

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

	}

	public int getValue() {
		return this.value;
	}

	public void setValue(final int val) {
		this.value = val;
	}

	@Override
	protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("value", this.value);
	}

	@Override
	protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		this.value = tag.getInt("value");
	}

	public DummyBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void destroy() {
		super.destroy();
		Block.popResource(this.level, this.worldPosition, new ItemStack(Items.STONE));
	}
}
