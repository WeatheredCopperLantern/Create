package com.simibubi.create.infrastructure.gametest.blockEntities;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.infrastructure.gametest.blocks.AllTestBlocks;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class AllTestBlockEntityTypes {

	private static final CreateRegistrate REGISTRATE = Create.registrate();

	public static final BlockEntityEntry<DummyBlockEntity> DUMMY = REGISTRATE.blockEntity("gametest_dummy_be", DummyBlockEntity::new).validBlocks(AllTestBlocks.IMMOVABLE_BE, AllTestBlocks.MOVABLE_BE).register();
	public static final BlockEntityEntry<DummyBBOBlockEntity> DUMMY_BBO = REGISTRATE.blockEntity("gametest_dummy_bbobe", DummyBBOBlockEntity::new).validBlock(AllTestBlocks.BBO).register();

	public static void register() {
	}
}
