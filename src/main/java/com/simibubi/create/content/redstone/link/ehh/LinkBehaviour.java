package com.simibubi.create.content.redstone.link.ehh;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.ehh.interfaces.IRedstoneLinkable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.link.ehh.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.redstone.link.ehh.interfaces.IRedstoneLinkable.Mode;
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

public class LinkBehaviour extends BlockEntityBehaviour implements ClipboardCloneable {

	public static final BehaviourType<LinkBehaviour> TYPE = new BehaviourType<>();

	public final IRedstoneLinkable link;

	ValueBoxTransform firstSlot;
	ValueBoxTransform secondSlot;

	public LinkBehaviour(SmartBlockEntity be, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		IntConsumer signalCallback, IntSupplier transmission, Mode mode) {
		super(be);
		firstSlot = slots.getLeft();
		secondSlot = slots.getRight();
		link = new LinkBehaviourRedstoneLinkable(Frequency.EMPTY, Frequency.EMPTY, mode, signalCallback, transmission, this);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (getWorld().isClientSide) return;
		link.setNetwork(Create.REDSTONE_LINK_NETWORK_HANDLER.findNetwork(blockEntity));
		if (link.isListening()) ((RedstoneLinkBlockEntity) blockEntity).checkAntenna();
		else link.queueUpdate();
	}

	@Override
	public void unload() {
		super.unload();
		if (getWorld().isClientSide) return;
		link.clearNetwork(1);
	}

	public boolean testHit(Boolean first, Vec3 hit) {
		BlockState state = blockEntity.getBlockState();
		Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
		return (first ? firstSlot : secondSlot).testHit(getWorld(), getPos(), state, localHit);
	}

	@Override
	public String getClipboardKey() {
		return "Frequencies";
	}

	@Override
	public boolean writeToClipboard(@NotNull HolderLookup.Provider registries, CompoundTag tag, Direction side) {
		Couple<Frequency> channel = link.getChannelKey();
		tag.put("First", channel.getFirst().getStack().saveOptional(registries));
		tag.put("Last", channel.getSecond().getStack().saveOptional(registries));
		return true;
	}

	@Override
	public boolean readFromClipboard(@NotNull HolderLookup.Provider registries, CompoundTag tag, Player player,
		Direction side, boolean simulate) {
		if (!tag.contains("First") || !tag.contains("Last")) return false;
		if (simulate) return true;
		link.setFrequency(true, ItemStack.parseOptional(registries, tag.getCompound("First")));
		link.setFrequency(false, ItemStack.parseOptional(registries, tag.getCompound("Last")));
		return true;
	}

	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(nbt, registries, clientPacket);
		Couple<Frequency> channel = link.getChannelKey();
		nbt.put("FrequencyFirst", channel.getFirst().getStack().saveOptional(registries));
		nbt.put("FrequencyLast", channel.getSecond().getStack().saveOptional(registries));
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(nbt, registries, clientPacket);
		link.setFrequency(true, ItemStack.parseOptional(registries, nbt.getCompound("FrequencyFirst")));
		link.setFrequency(false, ItemStack.parseOptional(registries, nbt.getCompound("FrequencyLast")));
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	private static class LinkBehaviourRedstoneLinkable extends AbstractRedstoneLinkable {

		private final LinkBehaviour behaviour;
		private final Vec3 location;

		public LinkBehaviourRedstoneLinkable(Frequency first, Frequency last, Mode mode, IntConsumer signalCallback,
			IntSupplier transmission, LinkBehaviour behaviour) {
			super(first, last, mode, signalCallback, transmission);
			this.behaviour = behaviour;
			location = Vec3.atCenterOf(behaviour.getPos());
		}

		@Override
		protected boolean shouldSetMode(Mode newMode) {
			return true;
		}

		@Override
		protected boolean shouldSetFrequency(boolean first, ItemStack stack) {
			return true;
		}

		@Override
		protected void onModeChanged(Mode newMode) {
		}

		@Override
		protected void onFrequencyChanged(boolean first, ItemStack stack) {
			behaviour.blockEntity.sendData();
		}

		@Override
		public void delayedUpdate() {
			super.delayedUpdate();
			if (behaviour.blockEntity instanceof RedstoneLinkBlockEntity be) {
				be.delayedUpdate();
			}
		}

		@Override
		public Vec3 getLocation() {
			return location;
		}

		@Override
		public Level getLevel() {
			return behaviour.getWorld();
		}
	}
}
