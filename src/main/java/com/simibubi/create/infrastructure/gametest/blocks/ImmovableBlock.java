package com.simibubi.create.infrastructure.gametest.blocks;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.infrastructure.gametest.blockEntities.AllTestBlockEntityTypes;
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
public class ImmovableBlock extends Block implements IBE<DummyBlockEntity> {

	@Override
	public Class<DummyBlockEntity> getBlockEntityClass() {
		return DummyBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DummyBlockEntity> getBlockEntityType() {
		return AllTestBlockEntityTypes.DUMMY.get();
	}

	@SuppressWarnings("RedundantMethodOverride")
	@Override
	public boolean isBEPushable() {
		return false;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		this.onIBERemove(state, level, pos, newState, movedByPiston);
	}

	public ImmovableBlock(final Properties properties) {
		super(properties);
	}
}
