package com.simibubi.create.content.contraptions.behaviour;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.mixin.accessor.ButtonBlockAccessor;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ButtonMovementBehaviour implements MovementBehaviour {

	@Override
	public boolean mustTickWhileDisabled() {
		return true;
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide || !context.state.getValue(ButtonBlock.POWERED)) return;
		if (context.contraption.disassembled) {
			cleanup(context);
			return;
		}
		if (tickWaitTime(context) <= 0) {
			context.temporaryData = null;
			context.contraption.entity.setBlock(context.localPos, new StructureTemplate.StructureBlockInfo(context.localPos, context.state.cycle(ButtonBlock.POWERED), context.blockEntityData));
			context.contraption.forEachActor(null, (behaviour, ctx) -> {
				//if (ctx.localPos.closerThan(context.localPos, 2.1) && behaviour instanceof RedstoneLinkMovementBehaviour linkBehaviour) {
				//	IRedstoneLinkable link = linkBehaviour.getLink(ctx);
				//	if (link != null && !link.isListening()) link.queueUpdate();
				//}
			});
		}
	}

	private int tickWaitTime(MovementContext context) {
		if (context.temporaryData instanceof Integer waitTime) {
			context.temporaryData = waitTime - 1;
		} else {
			context.temporaryData = ((ButtonBlockAccessor) context.state.getBlock()).create$getTicksToStayPressed();
		}
		return (Integer) context.temporaryData;
	}

	@Override
	public void stopMoving(MovementContext context) {
		cleanup(context);
	}

	private void cleanup(MovementContext context) {
		if (context.world.isClientSide || !context.state.getValue(ButtonBlock.POWERED)) return;
		context.contraption.entity.setBlock(context.localPos, new StructureTemplate.StructureBlockInfo(context.localPos, context.state.cycle(ButtonBlock.POWERED), context.blockEntityData));
	}
}
