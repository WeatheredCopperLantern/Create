package com.simibubi.create.infrastructure.gametest.blocks;

import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.persistent.PersistentObjectType;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.AllTestBlockBoundObjectTypes;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.DummyBlockBoundObject;
import com.simibubi.create.infrastructure.gametest.blockEntities.AllTestBlockEntityTypes;
import com.simibubi.create.infrastructure.gametest.blockEntities.DummyBBOBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BBOBlock extends Block implements IBE<DummyBBOBlockEntity>, IBBO<DummyBlockBoundObject> {

	public BBOBlock(final BlockBehaviour.Properties properties) {
		super(properties);
	}

	@SuppressWarnings("RedundantMethodOverride")
	@Override
	public boolean isBBOPushable() {
		return true;
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
	public Class<DummyBBOBlockEntity> getBlockEntityClass() {
		return DummyBBOBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DummyBBOBlockEntity> getBlockEntityType() {
		return AllTestBlockEntityTypes.DUMMY_BBO.get();
	}
}
