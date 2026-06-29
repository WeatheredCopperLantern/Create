package com.simibubi.create.content.contraptions.behaviour;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.mixin.accessor.ButtonBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class ButtonMovingInteraction extends SimpleBlockMovingInteraction {

	@Override
	protected BlockState handle(Player player, Contraption contraption, BlockPos pos, BlockState currentState) {
		if (currentState.getValue(ButtonBlock.POWERED)) return currentState;

		BlockSetType type = ((ButtonBlockAccessor) currentState.getBlock()).create$getBlockSetType();
		playSound(player, type.buttonClickOn(), 1);

		if (!contraption.entity.level().isClientSide) {
			contraption.forEachActor(null, (behaviour, ctx) -> {
				//if (ctx.localPos.closerThan(pos, 2.1) && behaviour instanceof RedstoneLinkMovementBehaviour linkBehaviour) {
				//	IRedstoneLinkable link = linkBehaviour.getLink(ctx);
				//	if (link != null && !link.isListening()) link.queueUpdate();
				//}
			});
		}

		return currentState.cycle(ButtonBlock.POWERED);
	}
}
