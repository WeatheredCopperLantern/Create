package com.simibubi.create.content.redstone.link.dummy;

import java.util.Arrays;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.RaycastHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class LinkHandler {

	@SubscribeEvent
	public static void onBlockActivated(final PlayerInteractEvent.RightClickBlock event) {
		final Level world = event.getLevel();
		final BlockPos pos = event.getPos();
		final Player player = event.getEntity();
		final InteractionHand hand = event.getHand();

		if (player.isShiftKeyDown() || player.isSpectator()) return;

		final LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
		if (behaviour == null) return;

		final ItemStack heldItem = player.getItemInHand(hand);
		final BlockHitResult ray = RaycastHelper.rayTraceRange(world, player, 10);
		if (ray == null) return;
		//if (AllItems.LINKED_CONTROLLER.isIn(heldItem))
		//	return;
		if (AllItems.WRENCH.isIn(heldItem)) return;

		final boolean fakePlayer = player instanceof FakePlayer;
		boolean fakePlayerChoice = false;

		if (fakePlayer) {
			final BlockState blockState = world.getBlockState(pos);
			final Vec3 localHit = ray.getLocation().subtract(Vec3.atLowerCornerOf(pos)).add(Vec3.atLowerCornerOf(ray.getDirection().getNormal()).scale(0.25f));
			fakePlayerChoice = localHit.distanceToSqr(behaviour.firstSlot.getLocalOffset(world, pos, blockState)) > localHit.distanceToSqr(behaviour.secondSlot.getLocalOffset(world, pos, blockState));
		}

		for (final boolean first : Arrays.asList(false, true)) {
			if (behaviour.testHit(first, ray.getLocation()) || fakePlayer && fakePlayerChoice == first) {
				if (event.getSide() != LogicalSide.CLIENT) behaviour.link.setFrequency(first, heldItem);
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.SUCCESS);
				world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25f, 0.1f);
			}
		}
	}
}
