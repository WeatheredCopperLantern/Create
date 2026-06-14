package com.simibubi.create.content.redstone.link;

import java.util.List;

import net.minecraft.core.HolderLookup;

import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.content.redstone.link.interfaces.IRedstoneLinkable.Mode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import static com.simibubi.create.content.redstone.link.RedstoneLinkBlock.POWERED;

public class RedstoneLinkBlockEntity extends SmartBlockEntity {

	private int signal;
	public LinkBehaviour behaviour;
	private boolean transmitter;

	public FactoryPanelSupportBehaviour panelSupport;

	public RedstoneLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		transmitter = !getBlockState().getValue(RedstoneLinkBlock.RECEIVER);
		Pair<ValueBoxTransform, ValueBoxTransform> slots = ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);
		behaviours.add(behaviour = new LinkBehaviour(this, slots, this::setSignal, this::getSignal, transmitter ? Mode.TRANSMIT : Mode.RECEIVE));
		behaviours.add(panelSupport = new FactoryPanelSupportBehaviour(this, () -> !transmitter, () -> getSignal() > 0, () -> AllBlocks.REDSTONE_LINK.get().queueUpdate(level, worldPosition)));
	}

	public int getSignal() {
		return transmitter ? signal : 0;
	}

	public void setSignal(int power) {
		if (!transmitter) {
			signal = power;
			if (level.isClientSide) return;

			BlockState blockState = getBlockState();
			if (!AllBlocks.REDSTONE_LINK.has(blockState)) return;

			if ((power > 0) != blockState.getValue(POWERED)) {
				level.setBlockAndUpdate(worldPosition, blockState.cycle(POWERED));
			}
			updateSelfAndAttached();
		}
	}

	public void checkAntenna() {
		BlockState state = getBlockState();
		if (state.getValue(RedstoneLinkBlock.ROTATED_ANTENNA) == (level.getBlockState(getBlockPos().above()).isAir() && state.getValue(RedstoneLinkBlock.FACING).getAxis() != Direction.Axis.Y)) {
			level.setBlock(getBlockPos(), state.cycle(RedstoneLinkBlock.ROTATED_ANTENNA), Block.UPDATE_CLIENTS);
		}
	}

	public void delayedUpdate() {
		if (level.isClientSide) return;

		BlockState state = getBlockState();
		if (!((RedstoneLinkBlock) state.getBlock()).canSurvive(state, level, getBlockPos())) {
			level.destroyBlock(getBlockPos(), true);
			return;
		}

		checkAntenna();

		if (behaviour.link.isListening()) return;
		int power = level.getBestNeighborSignal(getBlockPos());
		Boolean tri = panelSupport.shouldBePoweredTristate();
		int powerFromPanels = (tri == null) ? -1 : (tri) ? 15 : 0;

		// Suppress update if an input panel exists but is not loaded
		if (powerFromPanels == -1) return;

		power = Math.max(power, powerFromPanels);

		boolean previouslyPowered = state.getValue(POWERED);
		if (previouslyPowered != power > 0) level.setBlock(getBlockPos(), getBlockState().cycle(POWERED), Block.UPDATE_CLIENTS);

		transmit(power);
	}

	public void setMode(boolean receiver) {
		if (receiver != transmitter) return;
		behaviour.link.setMode(receiver ? Mode.RECEIVE : Mode.TRANSMIT);
		transmitter = !receiver;
		signal = 0;
		if (transmitter) {
			updateSelfAndAttached();
			transmit(level.getBestNeighborSignal(getBlockPos()));
		}
	}

	public void transmit(int strength) {
		if (!transmitter || signal == strength) return;
		signal = strength;
		behaviour.link.notifySignalChange();
	}

	public void updateSelfAndAttached() {
		BlockState blockState = getBlockState();
		Direction attachedFace = blockState.getValue(RedstoneLinkBlock.FACING).getOpposite();
		BlockPos attachedPos = worldPosition.relative(attachedFace);
		level.blockUpdated(worldPosition, blockState.getBlock());
		level.blockUpdated(attachedPos, level.getBlockState(attachedPos).getBlock());
		panelSupport.notifyPanels();
	}

	@Override
	public void remove() {
		super.remove();
		updateSelfAndAttached();
	}

	public int getReceivedSignal() {
		return transmitter ? 0 : signal;
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putBoolean("Transmitter", transmitter);
		compound.putInt("Signal", signal);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		transmitter = compound.getBoolean("Transmitter");

		if (!transmitter || (level == null || level.isClientSide)) {
			signal = compound.getInt("Signal");
		}
	}

}
