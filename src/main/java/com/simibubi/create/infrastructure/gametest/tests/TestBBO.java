package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.persistent.BlockBoundObject;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;
import com.simibubi.create.infrastructure.gametest.blockBoundObjects.AllTestBlockBoundObjectTypes;
import com.simibubi.create.infrastructure.gametest.blockEntities.AllTestBlockEntityTypes;
import com.simibubi.create.infrastructure.gametest.blocks.AllTestBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;

@GameTestGroup(path = "bbo")
public class TestBBO {

	/**
	 * Tests if {@link IBBO#isBBOPushable()} is respected.
	 */
	@GameTest(template = "the_block_said_no", timeoutTicks = 3)
	public static void theBlockSaidNo(CreateGameTestHelper helper) {
		helper.pullLever(1, 2, 1);

		helper.succeedWhen(() -> {
			helper.assertBlockPresent(AllTestBlocks.IMMOVABLE_BBO.get(), new BlockPos(3, 2, 1));
			helper.assertBlockPresent(AllTestBlocks.BBO.get(), new BlockPos(4, 2, 2));
		});
	}

	/**
	 * Tests if the {@link BlockBoundObject} is actually moved and not recreated. <br>
	 * Also tests if {@link BlockBoundObject#setBeingPushedByPiston(boolean)} is called when it should be.
	 */
	@GameTest(template = "new_pos_same_be", timeoutTicks = 3)
	public static void newPosSameBE(CreateGameTestHelper helper) {
		// Using a WeakReference is very likely unnecessary, but it doesn't hurt either
		final WeakReference<BlockBoundObject> originalBBo = new WeakReference<>(helper.getBlockBoundObject(AllTestBlockBoundObjectTypes.DUMMY.get(), new BlockPos(3, 2, 1)));
		final boolean[] isBeingPushedByPistonStates = {false, true}; // Default to the opposite of the wanted values
		helper.pullLever(1, 2, 1);
		helper.succeedWhen(() -> {
			BlockBoundObject bbo = originalBBo.get();
			if (bbo == null) helper.fail("NO BBO? :(");
			switch ((int) helper.getTick()) {
				case 1 -> {
					isBeingPushedByPistonStates[0] = bbo.isBeingPushedByPiston();
					helper.fail("Waiting");
				}
				case 3 -> {
					isBeingPushedByPistonStates[1] = bbo.isBeingPushedByPiston();

					if (!isBeingPushedByPistonStates[0] || isBeingPushedByPistonStates[1]) {
						helper.fail("Incorrect isBeingPushedByPistonStates. Expected: {true, false}, Got: {%s, %s}.".formatted(isBeingPushedByPistonStates[0], isBeingPushedByPistonStates[1]));
					}

					final BlockBoundObject movedBBO = helper.getBlockBoundObject(AllTestBlockBoundObjectTypes.DUMMY.get(), new BlockPos(4, 2, 1));

					if (!originalBBo.refersTo(movedBBO)) {
						helper.fail("Original BBO wasn't reused");
					}
					helper.succeed();
				}
				default -> helper.fail("Waiting");
			}
		});
	}
}
