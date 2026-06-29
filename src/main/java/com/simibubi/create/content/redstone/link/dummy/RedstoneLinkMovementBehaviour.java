package com.simibubi.create.content.redstone.link.dummy;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.redstone.link.dummy.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.redstone.link.dummy.interfaces.IRedstoneLinkable;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntConsumer;

public class RedstoneLinkMovementBehaviour implements MovementBehaviour {

	public IRedstoneLinkable getLink(MovementContext context) {
		return ensureLink(context);
	}

	private LinkMovementBehaviourRedstoneLinkable ensureLink(MovementContext context) {
		Level world = context.world;
		if (context.temporaryData instanceof LinkMovementBehaviourRedstoneLinkable link) {
			return link;
		} else {
			BlockPos pos;
			if (context.contraption.entity != null) {
				pos = BlockPos.containing(context.contraption.entity.toGlobalVector(VecHelper.getCenterOf(context.localPos), 1));
			} else {
				pos = context.localPos.offset(context.contraption.anchor);
			}
			LinkMovementBehaviourRedstoneLinkable link = new LinkMovementBehaviourRedstoneLinkable(context.blockEntityData, world, Vec3.atCenterOf(pos), value -> {
			});
			//link.setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(Vec3.atCenterOf(pos), world));
			link.shouldUpdate = true;
			context.temporaryData = link;
			return link;
		}
	}

	private void cleanup(MovementContext context) {
		if (context.temporaryData instanceof LinkMovementBehaviourRedstoneLinkable link) {
			link.clearNetwork(1);
		}
		context.temporaryData = null;
	}

	@Override
	public void startMoving(MovementContext context) {
		if (context.world.isClientSide) return;
		ensureLink(context);
	}

	@Override
	public void stopMoving(MovementContext context) {
		cleanup(context);
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide) return;
		if (context.contraption.disassembled) {
			cleanup(context);
			return;
		}
		LinkMovementBehaviourRedstoneLinkable link = ensureLink(context);
		if (link.shouldUpdate) {
			link.shouldUpdate = false;
			int signal = context.contraption.getBestNeighborSignal(context.localPos);
			link.setSignal(signal);
			if (signal > 0 != context.state.getValue(RedstoneLinkBlock.POWERED)) {
				context.contraption.entity.setBlock(context.localPos, new StructureTemplate.StructureBlockInfo(context.localPos, context.state.cycle(RedstoneLinkBlock.POWERED), context.blockEntityData));
			}
		}
	}

	@Override
	public boolean mustTickWhileDisabled() {
		return true;
	}

	@Override
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		LinkRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		Level world = context.world;
		if (world.isClientSide) {
			if (context.temporaryData == null) {
				context.temporaryData = Couple.create(Frequency.of(ItemStack.parseOptional(context.world.registryAccess(), context.blockEntityData.getCompound("FrequencyFirst"))), Frequency.of(ItemStack.parseOptional(context.world.registryAccess(), context.blockEntityData.getCompound("FrequencyLast"))));
			}
			return;
		}
		if (context.disabled) return;

		LinkMovementBehaviourRedstoneLinkable link = ensureLink(context);

		link.updatePos(Vec3.atCenterOf(pos));
		if (link.getNetwork() == null) {
			//link.setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(Vec3.atCenterOf(pos), world));
		}
	}

	@Override
	public void onDisabledByControls(MovementContext context) {
		MovementBehaviour.super.onDisabledByControls(context);
		if (context.world.isClientSide) return;

		LinkMovementBehaviourRedstoneLinkable link = ensureLink(context);
		link.clearNetwork();
	}

	private static class LinkMovementBehaviourRedstoneLinkable extends AbstractRedstoneLinkable {

		private Vec3 location;
		private final Level level;
		protected boolean shouldUpdate;
		private int signal = 0;

		public LinkMovementBehaviourRedstoneLinkable(CompoundTag beData, Level world, Vec3 pos,
			IntConsumer signalCallback) {
			super(Frequency.of(ItemStack.parseOptional(world.registryAccess(), beData.getCompound("FrequencyFirst"))), Frequency.of(ItemStack.parseOptional(world.registryAccess(), beData.getCompound("FrequencyLast"))), beData.getInt("Receive") == 0 ? Mode.TRANSMIT : Mode.RECEIVE, signalCallback, null);
			this.transmission = this::getSignal;
			this.level = world;
			this.location = pos;
		}

		public void updatePos(Vec3 pos) {
			if (location == pos) return;
			Vec3 old = location;
			location = pos;
			if (getNetwork() != null) getNetwork().linkMoved(this, old);
		}

		public int getSignal() {
			return signal;
		}

		protected void setSignal(int signal) {
			if (this.signal == signal) return;
			this.signal = signal;
			notifySignalChange();
		}

		@Override
		protected boolean shouldSetMode(Mode newMode) {
			return false;
		}

		@Override
		protected boolean shouldSetFrequency(boolean first, ItemStack stack) {
			return false;
		}

		@Override
		protected void onModeChanged(Mode newMode) {

		}

		@Override
		protected void onFrequencyChanged(boolean first, ItemStack stack) {

		}

		@Override
		public void queueUpdate() {
			shouldUpdate = true;
		}

		@Override
		public Vec3 getLocation() {
			return location;
		}

		@Override
		public Level getLevel() {
			return level;
		}
	}
}
