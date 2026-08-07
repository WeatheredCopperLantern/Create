package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPersistentObjectTypes;
import com.simibubi.create.content.redstone.link.linkable.LinkableBehaviour;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkableSnapshot;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkBlockLinkableBehaviour extends LinkableBehaviour<RedstoneLinkBlockBoundLinkable> {

	public static final BehaviourType<RedstoneLinkBlockLinkableBehaviour> TYPE = new BehaviourType<>();

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	@Override
	public void updateFromWorld() {
		AllBlocks.REDSTONE_LINK.get().updateFromWorld(this.getWorld(), blockEntity.getBlockPos(), blockEntity.getBlockState());
	}

	@Override
	public void channelChanged(@Nullable final RedstoneLinkableSnapshot snapshot) {
		this.channel = this.linkable.getChannel();
		blockEntity.sendData();
	}

	@Override
	public void modeChanged(@Nullable final RedstoneLinkableSnapshot snapshot) {
		assert this.blockEntity.getLevel() != null;
		blockEntity.getLevel().setBlock(blockEntity.getBlockPos(), blockEntity.getBlockState().setValue(RedstoneLinkBlock.RECEIVER, this.linkable.isReceiver()), Block.UPDATE_ALL);
		this.updateNeighbours();
	}

	@Override
	public void signalChanged(@Nullable final RedstoneLinkableSnapshot snapshot) {
		assert this.blockEntity.getLevel() != null;
		blockEntity.getLevel().setBlock(blockEntity.getBlockPos(), blockEntity.getBlockState().setValue(RedstoneLinkBlock.POWERED, this.linkable.getSignal() > 0), Block.UPDATE_ALL);
		if (this.linkable.isReceiver()) {
			this.updateNeighbours();
		}
	}

	@Override
	public void readFromLinkable(@Nullable final RedstoneLinkableSnapshot snapshot) {
		final BlockState state = this.blockEntity.getBlockState();
		if (state.getValue(RedstoneLinkBlock.RECEIVER) != this.linkable.isReceiver()) {
			this.modeChanged(snapshot);
		}
		if (state.getValue(RedstoneLinkBlock.POWERED) != this.linkable.getSignal() > 0) {
			this.signalChanged(snapshot);
		}
		this.channel = this.linkable.getChannel();
	}

	public RedstoneLinkBlockLinkableBehaviour(final SmartBlockEntity be) {
		super(be, AllPersistentObjectTypes.REDSTONE_LINK.get());
	}
}
