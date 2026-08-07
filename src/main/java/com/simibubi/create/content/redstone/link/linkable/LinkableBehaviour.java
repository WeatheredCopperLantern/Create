package com.simibubi.create.content.redstone.link.linkable;

import java.util.function.Consumer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.link.redstoneLink.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.redstoneLink.RedstoneLinkMenu;
import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.persistent.PersistentBlockBoundObject;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.ImmutableCouple;
import net.createmod.catnip.data.Iterate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
//TODO: add notice about needing to implement ILinkableBlockEntity
public abstract class LinkableBehaviour<T extends BlockBoundRedstoneLinkable> extends BlockEntityBehaviour implements MenuProvider, ClipboardCloneable {

	public static final String CLIPBOARD_KEY = "RedstoneLinkable";
	public static final String FIRST_FREQUENCY = "FrequencyFirst";
	public static final String SECOND_FREQUENCY = "FrequencyLast";

	protected final PersistentObjectType<T> persistentObjectType;
	protected T linkable;

	public ImmutableCouple<Frequency> channel = ImmutableCouple.create(Frequency.EMPTY, Frequency.EMPTY);

	public ItemStackHandler getFrequencyItems() {
		final ItemStackHandler newInv = new ItemStackHandler(2);
		newInv.setStackInSlot(0, this.channel.get(true).stack);
		newInv.setStackInSlot(1, this.channel.get(false).stack);
		return newInv;
	}

	public Couple<ValueBoxTransform> getSlots() {
		return RedstoneLinkBlock.SLOTS;
	}

	public abstract void updateFromWorld();

	public abstract void channelChanged(@Nullable final RedstoneLinkableSnapshot snapshot);

	public abstract void modeChanged(@Nullable final RedstoneLinkableSnapshot snapshot);

	public abstract void signalChanged(@Nullable final RedstoneLinkableSnapshot snapshot);

	public abstract void readFromLinkable(@Nullable final RedstoneLinkableSnapshot snapshot);

	public boolean testHit(final Level level, final BlockState state, final BlockPos pos, final Boolean first, final Vec3 hit) {
		final Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(pos));
		return (getSlots().get(first)).testHit(level, pos, state, localHit);
	}

	@Override
	public String getClipboardKey() {
		return CLIPBOARD_KEY;
	}

	@Override
	public boolean writeToClipboard(final HolderLookup.Provider registries, final CompoundTag tag, final Direction side) {
		tag.put(FIRST_FREQUENCY, this.channel.getFirst().write(registries));
		tag.put(SECOND_FREQUENCY, this.channel.getSecond().write(registries));
		return true;
	}

	@Override
	public boolean readFromClipboard(final HolderLookup.Provider registries, final CompoundTag tag, final Player player, final Direction side, final boolean simulate) {
		if (simulate) return true;

		final BlockState blockState = blockEntity.getBlockState();
		final Level world = blockEntity.getLevel();
		final BlockPos pos = blockEntity.getBlockPos();
		if ((blockState.getBlock() instanceof final IBBO<?> ibbo && ibbo.getBlockBoundObject(world, pos) instanceof BlockBoundRedstoneLinkable blockBoundRedstoneLinkable)) {
			boolean playSound = false;
			for (final boolean first : Iterate.trueAndFalse) {
				final Tag tmpTag = tag.get(first ? FIRST_FREQUENCY : SECOND_FREQUENCY);
				if (tmpTag != null && blockBoundRedstoneLinkable.setFrequency(first, Frequency.read(tmpTag, registries))) {
					playSound = true;
				}
			}
			if (playSound) {
				world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1f);
			}
		}

		return true;
	}

	@Override
	public void initialize() {
		if (blockEntity.getLevel() instanceof ServerLevel serverLevel && this.blockEntity.getBlockState().getBlock() instanceof final IBBO<?> ibbo) {
			if (ibbo.getBlockBoundObject(serverLevel, this.blockEntity.getBlockPos()) instanceof final BlockBoundRedstoneLinkable blockBoundRedstoneLinkable) {
				blockBoundRedstoneLinkable.setBehaviour(this);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <D extends BlockBoundRedstoneLinkable> void setLinkable(D linkable) {
		this.linkable = (T) linkable;
	}

	protected void updateNeighbours() {
		final Level level = this.blockEntity.getLevel();
		if (level == null) return;
		final BlockState state = this.blockEntity.getBlockState();
		final BlockPos pos = this.blockEntity.getBlockPos();

		level.updateNeighborsAt(pos, state.getBlock());
		if (state.hasProperty(DirectionalBlock.FACING)) {
			level.updateNeighborsAt(pos.relative(state.getValue(DirectionalBlock.FACING).getOpposite()), state.getBlock());
		}
	}

	@Override
	public void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (!clientPacket) return;
		this.channel = ImmutableCouple.createWithContext(aBoolean -> Frequency.read(tag.get(aBoolean ? FIRST_FREQUENCY : SECOND_FREQUENCY), registries));
	}

	@Override
	public void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (!clientPacket) return;
		channel.forEachWithParams((frequency, key) -> tag.put(key, frequency.write(registries)), Couple.create(FIRST_FREQUENCY, SECOND_FREQUENCY));
	}

	@Override
	public void unload() {
		withLinkableDo(BlockBoundRedstoneLinkable::blockUnloaded);
	}

	@Override
	public void destroy() {
		withLinkableDo(t -> {
			t.blockUnloaded();
			t.blockDestroyed();
			this.linkable = null;
		});
	}

	@Override
	public Component getDisplayName() {
		return blockEntity.getBlockState().getBlock().getName();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(final int i, final Inventory inventory, final Player player) {
		return RedstoneLinkMenu.create(i, inventory, this);
	}

	public LinkableBehaviour(final SmartBlockEntity be, final PersistentObjectType<T> persistentObjectType) {
			super(be);
		this.persistentObjectType = persistentObjectType;
	}

	/**
	 * The action can only ever run on the Server
	 */
	protected void withLinkableDo(Consumer<T> action) {
		if (this.linkable != null) action.accept(this.linkable);
	}
}
