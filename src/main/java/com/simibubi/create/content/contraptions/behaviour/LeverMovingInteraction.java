package com.simibubi.create.content.contraptions.behaviour;

import com.simibubi.create.content.contraptions.Contraption;

import com.simibubi.create.content.redstone.link.ehh.RedstoneLinkMovementBehaviour;
import com.simibubi.create.content.redstone.link.ehh.interfaces.IRedstoneLinkable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;

public class LeverMovingInteraction extends SimpleBlockMovingInteraction {

	@Override
	protected BlockState handle(Player player, Contraption contraption, BlockPos pos, BlockState currentState) {
		playSound(player, SoundEvents.LEVER_CLICK, currentState.getValue(LeverBlock.POWERED) ? 0.5f : 0.6f);

		if (!contraption.entity.level().isClientSide) {
			contraption.forEachActor(null, (behaviour, ctx) -> {
				if (ctx.localPos.closerThan(pos, 2.1) && behaviour instanceof RedstoneLinkMovementBehaviour linkBehaviour) {
					IRedstoneLinkable link = linkBehaviour.getLink(ctx);
					if (link != null && !link.isListening()) link.queueUpdate();
				}
			});
		}

		return currentState.cycle(LeverBlock.POWERED);
	}

}
