package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.link.ChannelItemsData;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.redstoneLink.RedstoneLinkBlockBoundLinkable;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.createmod.catnip.data.ImmutableCouple;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;

@GameTestGroup(path = "features")
public class TestFeatures {

	// Tests BBO functionality.
	// This uses the Redstone Link so issues might be there and not with BBOs themselves
	@GameTest(template = "lifecycle")
	public static void lifecycle(CreateGameTestHelper helper) {
		final BlockPos pos = new BlockPos(1, 2, 1);
		final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		final Frequency frequency1 = Frequency.of(new ItemStack(AllBlocks.REDSTONE_LINK));
		final Frequency frequency2 = Frequency.of(new ItemStack(AllBlocks.CLUTCH));

		final ItemStack itemStack = new ItemStack(AllBlocks.REDSTONE_LINK);
		itemStack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(RedstoneLinkBlock.RECEIVER, true));
		itemStack.set(AllDataComponents.CHANNEL_ITEMS, new ChannelItemsData(ImmutableCouple.create(frequency1, frequency2)));

		player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
		helper.placeAt(player, itemStack, pos.below(), Direction.UP);

		// Creation
		final RedstoneLinkBlockEntity be = helper.getBlockEntity(AllBlocks.REDSTONE_LINK.get().getBlockEntityType(), pos);
		if (be == null) helper.fail("BE wasn't created");

		final RedstoneLinkBlockBoundLinkable bbo = helper.getBlockBoundObject(AllBlocks.REDSTONE_LINK.get().getBlockBoundObjectType(), pos);
		if (bbo == null) helper.fail("BBO wasn't created");

		if (bbo.getChannel().getFirst() != frequency1 || bbo.getChannel().getSecond() != frequency2) helper.fail("BBO applyComponents isn't called");

		// Simulate unload

		// Simulate destroy

		// Simulate Contraption pickup

		// Simulate pushed by Piston


		helper.succeed();
	}
}
