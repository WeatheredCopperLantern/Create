package com.simibubi.create.content.redstone.link.dummy;

import java.util.function.IntConsumer;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
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

public class RedstoneLinkMovementBehaviour implements MovementBehaviour {

	public IRedstoneLinkable getLink(final MovementContext context) {
		return this.ensureLink(context);
	}

	private LinkMovementBehaviourRedstoneLinkable ensureLink(final MovementContext context) {
		final Level world = context.world;
		if (context.temporaryData instanceof final LinkMovementBehaviourRedstoneLinkable link) {
			return link;
		} else {
			final BlockPos pos;
			if (context.contraption.entity != null) {
				pos = BlockPos.containing(context.contraption.entity.toGlobalVector(VecHelper.getCenterOf(context.localPos), 1));
			} else {
				pos = context.localPos.offset(context.contraption.anchor);
			}
			final LinkMovementBehaviourRedstoneLinkable link = new LinkMovementBehaviourRedstoneLinkable(context.blockEntityData, world, Vec3.atCenterOf(pos), value -> {
			});
			//link.setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(Vec3.atCenterOf(pos), world));
			link.shouldUpdate = true;
			context.temporaryData = link;
			return link;
		}
	}

	private void cleanup(final MovementContext context) {
		if (context.temporaryData instanceof final LinkMovementBehaviourRedstoneLinkable link) {
			link.clearNetwork(1);
		}
		context.temporaryData = null;
	}

	@Override
	public void startMoving(final MovementContext context) {
		if (context.world.isClientSide) return;
		this.ensureLink(context);
	}

	@Override
	public void stopMoving(final MovementContext context) {
		this.cleanup(context);
	}

	@Override
	public void tick(final MovementContext context) {
		if (context.world.isClientSide) return;
		if (context.contraption.disassembled) {
			this.cleanup(context);
			return;
		}
		final LinkMovementBehaviourRedstoneLinkable link = this.ensureLink(context);
		if (link.shouldUpdate) {
			link.shouldUpdate = false;
			final int signal = context.contraption.getBestNeighborSignal(context.localPos);
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
	public void renderInContraption(final MovementContext context, final VirtualRenderWorld renderWorld, final ContraptionMatrices matrices, final MultiBufferSource buffer) {
		LinkRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	public void visitNewPosition(final MovementContext context, final BlockPos pos) {
		final Level world = context.world;
		if (world.isClientSide) {
			if (context.temporaryData == null) {
				context.temporaryData = Couple.create(RedstoneLinkNetworkHandler.Frequency.of(ItemStack.parseOptional(context.world.registryAccess(), context.blockEntityData.getCompound("FrequencyFirst"))), RedstoneLinkNetworkHandler.Frequency.of(ItemStack.parseOptional(context.world.registryAccess(), context.blockEntityData.getCompound("FrequencyLast"))));
			}
			return;
		}
		if (context.disabled) return;

		final LinkMovementBehaviourRedstoneLinkable link = this.ensureLink(context);

		link.updatePos(Vec3.atCenterOf(pos));
		if (link.getNetwork() == null) {
			//link.setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(Vec3.atCenterOf(pos), world));
		}
	}

	@Override
	public void onDisabledByControls(final MovementContext context) {
		MovementBehaviour.super.onDisabledByControls(context);
		if (context.world.isClientSide) return;

		final LinkMovementBehaviourRedstoneLinkable link = this.ensureLink(context);
		link.clearNetwork();
	}

	private static class LinkMovementBehaviourRedstoneLinkable extends AbstractRedstoneLinkable {

		private Vec3 location;
		private final Level level;
		protected boolean shouldUpdate;
		private int signal;

		public LinkMovementBehaviourRedstoneLinkable(final CompoundTag beData, final Level world, final Vec3 pos, final IntConsumer signalCallback) {
			super(RedstoneLinkNetworkHandler.Frequency.of(ItemStack.parseOptional(world.registryAccess(), beData.getCompound("FrequencyFirst"))), RedstoneLinkNetworkHandler.Frequency.of(ItemStack.parseOptional(world.registryAccess(), beData.getCompound("FrequencyLast"))), beData.getInt("Receive") == 0 ? Mode.TRANSMIT : Mode.RECEIVE, signalCallback, null);
			this.transmission = this::getSignal;
			this.level = world;
			this.location = pos;
		}

		public void updatePos(final Vec3 pos) {
			if (this.location == pos) return;
			final Vec3 old = this.location;
			this.location = pos;
			if (this.getNetwork() != null) this.getNetwork().linkMoved(this, old);
		}

		public int getSignal() {
			return this.signal;
		}

		protected void setSignal(final int signal) {
			if (this.signal == signal) return;
			this.signal = signal;
			this.notifySignalChange();
		}

		@Override
		protected boolean shouldSetMode(final Mode newMode) {
			return false;
		}

		@Override
		protected boolean shouldSetFrequency(final boolean first, final ItemStack stack) {
			return false;
		}

		@Override
		protected void onModeChanged(final Mode newMode) {

		}

		@Override
		protected void onFrequencyChanged(final boolean first, final ItemStack stack) {

		}

		@Override
		public void queueUpdate() {
			this.shouldUpdate = true;
		}

		@Override
		public Vec3 getLocation() {
			return this.location;
		}

		@Override
		public Level getLevel() {
			return this.level;
		}
	}
}
