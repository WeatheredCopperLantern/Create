package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;
import com.simibubi.create.infrastructure.gametest.blocks.AllTestBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;

@GameTestGroup(path = "bbo")
public class TestBBO {

	/**
	 * Tests if {@link IBBO#isBBOPushable()} ()} is respected.
	 */
	@GameTest(template = "the_block_said_no", timeoutTicks = 3)
	public static void theBlockSaidNo(CreateGameTestHelper helper) {
		helper.pullLever(1, 2, 1);

		helper.succeedWhen(() -> {
			helper.assertBlockPresent(AllTestBlocks.IMMOVABLE_BE.get(), new BlockPos(3, 2, 1));
			helper.assertBlockPresent(AllTestBlocks.MOVABLE_BE.get(), new BlockPos(4, 2, 2));
		});
	}
}
