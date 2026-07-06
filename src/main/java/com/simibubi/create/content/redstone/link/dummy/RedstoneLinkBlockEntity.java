package com.simibubi.create.content.redstone.link.dummy;

import java.util.List;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.link.dummy.interfaces.IRedstoneLinkable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.apache.commons.lang3.tuple.Pair;
import static com.simibubi.create.content.redstone.link.dummy.RedstoneLinkBlock.POWERED;

public class RedstoneLinkBlockEntity extends SmartBlockEntity {

	private int signal;
	public LinkBehaviour behaviour;
	private boolean transmitter;

	public FactoryPanelSupportBehaviour panelSupport;

	public RedstoneLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
		this.transmitter = !this.getBlockState().getValue(RedstoneLinkBlock.RECEIVER);
		final Pair<ValueBoxTransform, ValueBoxTransform> slots = ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);
		behaviours.add(this.behaviour = new LinkBehaviour(this, slots, this::setSignal, this::getSignal, this.transmitter ? IRedstoneLinkable.Mode.TRANSMIT : IRedstoneLinkable.Mode.RECEIVE));
		behaviours.add(this.panelSupport = new FactoryPanelSupportBehaviour(this, () -> !this.transmitter, () -> this.getSignal() > 0, () -> {
		}/*AllBlocks.REDSTONE_LINK.get().queueUpdate(level, worldPosition)*/));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.initialized && this.hasLevel()) {
			this.initialize();
			this.initialized = true;
		}
		this.panelSupport.tick();
	}

	public int getSignal() {
		return this.transmitter ? this.signal : 0;
	}

	public void setSignal(final int power) {
		if (!this.transmitter && power != this.signal /*&& AllBlocks.REDSTONE_LINK.has(level.getBlockState(worldPosition))*/) {
			this.signal = power;
			if (this.level.isClientSide) return;

			final BlockState blockState = this.getBlockState();
			if ((power > 0) != blockState.getValue(POWERED)) {
				this.level.setBlock(this.worldPosition, blockState.cycle(POWERED), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
			}
			this.updateSelfAndAttached();
		}
	}

	public void checkAntenna() {
		final BlockState state = this.getBlockState();
		if (state.getValue(RedstoneLinkBlock.ROTATED_ANTENNA) == (this.level.getBlockState(this.getBlockPos().above()).isAir() && state.getValue(DirectionalBlock.FACING).getAxis() != Direction.Axis.Y)) {
			this.level.setBlock(this.getBlockPos(), state.cycle(RedstoneLinkBlock.ROTATED_ANTENNA), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
		}
	}

	public void delayedUpdate() {
		final BlockState state = this.getBlockState();
		if (!RedstoneLinkBlock.canSurviveStatic(state, this.level, this.getBlockPos())) {
			this.level.destroyBlock(this.getBlockPos(), true);
			return;
		}

		this.checkAntenna();

		if (this.behaviour.link.isListening()) return;
		final Boolean tri = this.panelSupport.shouldBePoweredTristate();
		final int powerFromPanels = (tri == null) ? -1 : (tri) ? 15 : 0;

		// Suppress update if an input panel exists but is not loaded
		if (powerFromPanels == -1) return;

		int power = this.level.getBestNeighborSignal(this.getBlockPos());
		power = Math.max(power, powerFromPanels);

		final boolean previouslyPowered = state.getValue(POWERED);
		if (previouslyPowered != power > 0) {
			this.level.setBlock(this.getBlockPos(), this.getBlockState().cycle(POWERED), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
		}

		this.transmit(power);
	}

	public void setMode(final boolean receiver) {
		if (receiver != this.transmitter) return;
		this.behaviour.link.setMode(receiver ? IRedstoneLinkable.Mode.RECEIVE : IRedstoneLinkable.Mode.TRANSMIT);
		this.transmitter = !receiver;
		this.signal = 0;
		if (this.transmitter) {
			this.updateSelfAndAttached();
			this.transmit(this.level.getBestNeighborSignal(this.getBlockPos()));
		}
	}

	public void transmit(final int strength) {
		if (!this.transmitter || this.signal == strength) return;
		this.signal = strength;
		this.behaviour.link.notifySignalChange();
	}

	public void updateSelfAndAttached() {
		final BlockState blockState = this.getBlockState();
		final Direction attachedFace = blockState.getValue(DirectionalBlock.FACING).getOpposite();
		final BlockPos attachedPos = this.worldPosition.relative(attachedFace);
		this.level.blockUpdated(this.worldPosition, blockState.getBlock());
		this.level.blockUpdated(attachedPos, this.level.getBlockState(attachedPos).getBlock());
		this.panelSupport.notifyPanels();
	}

	@Override
	public void remove() {
		super.remove();
		this.updateSelfAndAttached();
	}

	public int getReceivedSignal() {
		return this.transmitter ? 0 : this.signal;
	}

	@Override
	public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		compound.putBoolean("Transmitter", this.transmitter);
		compound.putInt("Signal", this.signal);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		this.transmitter = compound.getBoolean("Transmitter");

		if (!this.transmitter || (this.level == null || this.level.isClientSide)) {
			this.signal = compound.getInt("Signal");
		}
	}
}
