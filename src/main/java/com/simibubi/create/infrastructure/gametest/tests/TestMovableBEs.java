package com.simibubi.create.infrastructure.gametest.tests;

import java.lang.ref.WeakReference;
import java.util.List;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.extension.interfaces.PistonMovingBlockEntityAccessor;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;
import com.simibubi.create.infrastructure.gametest.blockEntities.AllTestBlockEntityTypes;
import com.simibubi.create.infrastructure.gametest.blockEntities.DummyBlockEntity;
import com.simibubi.create.infrastructure.gametest.blocks.AllTestBlocks;

import net.createmod.catnip.registry.RegisteredObjectsHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

@GameTestGroup(path = "movable_be")
public class TestMovableBEs {

	/**
	 * Tests if {@link IBE#isBEPushable()} is respected.
	 */
	@GameTest(template = "the_block_said_no", timeoutTicks = 3)
	public static void theBlockSaidNo(CreateGameTestHelper helper) {
		helper.pullLever(1, 2, 1);

		helper.succeedWhen(() -> {
			helper.assertBlockPresent(AllTestBlocks.IMMOVABLE_BE.get(), new BlockPos(3, 2, 1));
			helper.assertBlockPresent(AllTestBlocks.MOVABLE_BE.get(), new BlockPos(4, 2, 2));
		});
	}

	/**
	 * Tests if the {@link SmartBlockEntity} is actually moved and not recreated. <br>
	 * Also tests if {@link SmartBlockEntity#setBeingPushedByPiston(boolean)} is called when it should be.
	 */
	@GameTest(template = "new_pos_same_be", timeoutTicks = 3)
	public static void newPosSameBE(CreateGameTestHelper helper) {
		// Using a WeakReference is very likely unnecessary, but it doesn't hurt either
		final WeakReference<BlockEntity> originalBe = new WeakReference<>(helper.getBlockEntity(AllTestBlockEntityTypes.DUMMY.get(), new BlockPos(3, 2, 1)));
		final boolean[] isBeingPushedByPistonStates = {false, true}; // Default to the opposite of the wanted values
		helper.pullLever(1, 2, 1);

		helper.succeedWhen(() -> {
			SmartBlockEntity smartBE = (SmartBlockEntity) originalBe.get();
			if (smartBE == null) helper.fail("NO BE? :(");
			switch ((int) helper.getTick()) {
				case 1 -> {
					isBeingPushedByPistonStates[0] = smartBE.isBeingPushedByPiston();
					helper.fail("Waiting");
				}
				case 3 -> {
					isBeingPushedByPistonStates[1] = smartBE.isBeingPushedByPiston();

					if (!isBeingPushedByPistonStates[0] || isBeingPushedByPistonStates[1]) {
						helper.fail("Incorrect isBeingPushedByPistonStates. Expected: {true, false}, Got: {%s, %s}.".formatted(isBeingPushedByPistonStates[0], isBeingPushedByPistonStates[1]));
					}

					final BlockEntity movedBE = helper.getBlockEntity(new BlockPos(4, 2, 1));

					if (!originalBe.refersTo(movedBE)) {
						helper.fail("Original BE wasn't reused");
					}
					helper.succeed();
				}
				default -> helper.fail("Waiting");
			}
		});
	}

	/**
	 * Tests if the {@link SmartBlockEntity} in transit is properly saved and loaded if the parent {@link net.minecraft.world.level.block.piston.PistonMovingBlockEntity PistonMovingBlockEntity} unloads.
	 */
	@GameTest(template = "new_piston_who_dis", timeoutTicks = 2)
	public static void newPistonWhoDis(CreateGameTestHelper helper) {
		final Level level = helper.getLevel();
		final BlockPos bePos = new BlockPos(4, 2, 1);
		final CompoundTag[] tag = new CompoundTag[1];
		final int val = helper.getLevel().random.nextInt();
		helper.pullLever(1, 2, 1);

		helper.succeedWhen(() -> {
			switch ((int) helper.getTick()) {
				case 1 -> {
					final PistonMovingBlockEntity be = helper.getBlockEntity(BlockEntityType.PISTON, bePos);
					if (be == null) helper.fail("NO BE? :(");
					((DummyBlockEntity) ((PistonMovingBlockEntityAccessor) be).create$getBlockEntity()).setValue(val);
					tag[0] = be.saveWithFullMetadata(level.registryAccess());
					level.removeBlockEntity(helper.absolutePos(bePos));
					helper.fail("Waiting");
				}
				case 2 -> {
					final BlockEntity be = BlockEntityType.PISTON.create(helper.absolutePos(bePos), helper.getBlockState(bePos));
					if (be == null) helper.fail("NO BE? :(");
					be.loadWithComponents(tag[0], level.registryAccess());
					level.setBlockEntity(be);
					if (((DummyBlockEntity) ((PistonMovingBlockEntityAccessor) be).create$getBlockEntity()).getValue() != val) {
						helper.fail("values mismatch");
					} else {
						helper.succeed();
					}
				}
				default -> helper.fail("Waiting");
			}
		});
	}

	/**
	 * Test if {@link SmartBlockEntity#destroy()} is called when the {@link net.minecraft.world.level.block.piston.PistonMovingBlockEntity PistonMovingBlockEntity} is exploded.
	 */
	@GameTest(template = "gone_reduced_to_atoms", timeoutTicks = 2)
	public static void goneReducedToAtmos(CreateGameTestHelper helper) {
		helper.pullLever(1, 2, 1);
		helper.succeedWhen(() -> {
			helper.assertTicksPassed(1);
			final List<ItemEntity> entities = helper.getEntities(EntityType.ITEM);
			if (entities.size() != 1) {
				helper.fail("Expected 1 ItemEntity, got %s".formatted(entities.size()));
			}
			final ItemEntity entity = entities.getFirst();
			final Item item = entity.getItem().getItem();
			if (item != Items.STONE) {
				helper.fail("Expected Item [%s] got [%s]".formatted(RegisteredObjectsHelper.getKeyOrThrow(Items.STONE).toString(), RegisteredObjectsHelper.getKeyOrThrow(item)));
			}
		});
	}
}
