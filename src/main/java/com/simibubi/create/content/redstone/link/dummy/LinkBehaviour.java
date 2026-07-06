package com.simibubi.create.content.redstone.link.dummy;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.link.dummy.interfaces.IRedstoneLinkable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.data.Couple;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class LinkBehaviour extends BlockEntityBehaviour implements ClipboardCloneable {

	public static final BehaviourType<LinkBehaviour> TYPE = new BehaviourType<>();

	public final IRedstoneLinkable link;

	ValueBoxTransform firstSlot;
	ValueBoxTransform secondSlot;

	public LinkBehaviour(final SmartBlockEntity be, final Pair<ValueBoxTransform, ValueBoxTransform> slots, final IntConsumer signalCallback, final IntSupplier transmission, final IRedstoneLinkable.Mode mode) {
		super(be);
		this.firstSlot = slots.getLeft();
		this.secondSlot = slots.getRight();
		this.link = new LinkBehaviourRedstoneLinkable(RedstoneLinkNetworkHandler.Frequency.EMPTY, RedstoneLinkNetworkHandler.Frequency.EMPTY, mode, signalCallback, transmission, this);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (this.getWorld().isClientSide) return;
		//link.setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(blockEntity));
		if (this.link.isListening()) {
			((RedstoneLinkBlockEntity) this.blockEntity).checkAntenna();
		} else {
			this.link.queueUpdate();
		}
	}

	@Override
	public void unload() {
		super.unload();
		if (this.getWorld().isClientSide) return;
		this.link.clearNetwork(1);
	}

	public boolean testHit(final Boolean first, final Vec3 hit) {
		final BlockState state = this.blockEntity.getBlockState();
		final Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(this.blockEntity.getBlockPos()));
		return (first ? this.firstSlot : this.secondSlot).testHit(this.getWorld(), this.getPos(), state, localHit);
	}

	@Override
	public String getClipboardKey() {
		return "Frequencies";
	}

	@Override
	public boolean writeToClipboard(@NotNull final HolderLookup.Provider registries, final CompoundTag tag, final Direction side) {
		final Couple<RedstoneLinkNetworkHandler.Frequency> channel = this.link.getChannelKey();
		tag.put("First", channel.getFirst().getStack().saveOptional(registries));
		tag.put("Last", channel.getSecond().getStack().saveOptional(registries));
		return true;
	}

	@Override
	public boolean readFromClipboard(@NotNull final HolderLookup.Provider registries, final CompoundTag tag, final Player player, final Direction side, final boolean simulate) {
		if (!tag.contains("First") || !tag.contains("Last")) return false;
		if (simulate) return true;
		this.link.setFrequency(true, ItemStack.parseOptional(registries, tag.getCompound("First")));
		this.link.setFrequency(false, ItemStack.parseOptional(registries, tag.getCompound("Last")));
		return true;
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.write(nbt, registries, clientPacket);
		final Couple<RedstoneLinkNetworkHandler.Frequency> channel = this.link.getChannelKey();
		nbt.put("FrequencyFirst", channel.getFirst().getStack().saveOptional(registries));
		nbt.put("FrequencyLast", channel.getSecond().getStack().saveOptional(registries));
	}

	@Override
	public void read(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(nbt, registries, clientPacket);
		this.link.setFrequency(true, ItemStack.parseOptional(registries, nbt.getCompound("FrequencyFirst")));
		this.link.setFrequency(false, ItemStack.parseOptional(registries, nbt.getCompound("FrequencyLast")));
	}

	@Override
	public BehaviourType<?> getType() {
		return LinkBehaviour.TYPE;
	}

	private static class LinkBehaviourRedstoneLinkable extends AbstractRedstoneLinkable {

		private final LinkBehaviour behaviour;
		private final Vec3 location;

		public LinkBehaviourRedstoneLinkable(final RedstoneLinkNetworkHandler.Frequency first, final RedstoneLinkNetworkHandler.Frequency last, final IRedstoneLinkable.Mode mode, final IntConsumer signalCallback, final IntSupplier transmission, final LinkBehaviour behaviour) {
			super(first, last, mode, signalCallback, transmission);
			this.behaviour = behaviour;
			this.location = Vec3.atCenterOf(behaviour.getPos());
		}

		@Override
		protected boolean shouldSetMode(final IRedstoneLinkable.Mode newMode) {
			return true;
		}

		@Override
		protected boolean shouldSetFrequency(final boolean first, final ItemStack stack) {
			return true;
		}

		@Override
		protected void onModeChanged(final IRedstoneLinkable.Mode newMode) {
		}

		@Override
		protected void onFrequencyChanged(final boolean first, final ItemStack stack) {
			this.behaviour.blockEntity.sendData();
		}

		@Override
		public void delayedUpdate() {
			super.delayedUpdate();
			if (this.behaviour.blockEntity instanceof final RedstoneLinkBlockEntity be) {
				be.delayedUpdate();
			}
		}

		@Override
		public Vec3 getLocation() {
			return this.location;
		}

		@Override
		public Level getLevel() {
			return this.behaviour.getWorld();
		}
	}
}
