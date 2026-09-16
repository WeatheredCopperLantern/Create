package com.simibubi.create.infrastructure.gametest.blocks;

import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.persistent.PersistentObjectType;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.AllTestBlockBoundObjectTypes;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.DummyBlockBoundObject;
import com.simibubi.create.infrastructure.gametest.blockEntities.AllTestBlockEntityTypes;
import com.simibubi.create.infrastructure.gametest.blockEntities.DummyBBOBlockEntity;
import com.simibubi.create.infrastructure.gametest.blockEntities.DummyBlockEntity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ImmovableBlock2 extends Block implements IBE<DummyBBOBlockEntity>, IBBO<DummyBlockBoundObject> {

	@Override
	public Class<DummyBBOBlockEntity> getBlockEntityClass() {
		return DummyBBOBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DummyBBOBlockEntity> getBlockEntityType() {
		return AllTestBlockEntityTypes.DUMMY_BBO.get();
	}

	@Override
	public boolean isBEPushable() {
		return true;
	}

	@Override
	public PersistentObjectType<DummyBlockBoundObject> getBlockBoundObjectType() {
		return AllTestBlockBoundObjectTypes.DUMMY.get();
	}

	@Override
	public boolean isBBOPushable() {
		return false;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		this.onIBERemove(state, level, pos, newState, movedByPiston);
	}

	public ImmovableBlock2(final Properties properties) {
		super(properties);
	}
}
