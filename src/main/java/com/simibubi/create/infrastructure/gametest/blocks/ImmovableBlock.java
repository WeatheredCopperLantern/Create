package com.simibubi.create.infrastructure.gametest.blocks;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.infrastructure.gametest.blockEntities.AllTestBlockEntityTypes;
import com.simibubi.create.infrastructure.gametest.blockEntities.DummyBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

	public ImmovableBlock(final Properties properties) {
		super(properties);
	}
}
