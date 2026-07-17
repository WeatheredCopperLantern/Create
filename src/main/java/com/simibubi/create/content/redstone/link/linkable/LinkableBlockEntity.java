package com.simibubi.create.content.redstone.link.linkable;

import java.util.function.Consumer;

import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.link.redstoneLink.RedstoneLinkLinkable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class LinkableBlockEntity<T extends RedstoneLinkable> extends SmartBlockEntity implements MenuProvider, ClipboardCloneable {

	private final Class<T> linkableType;
	public T linkable;
	public Couple<Frequency> channel = Couple.create(Frequency.EMPTY, Frequency.EMPTY);

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		assert this.level != null;
		if (this.level.isClientSide) return;
		this.linkable.representationUnloaded();
	}

	@Override
	public void remove() {
		assert this.level != null;
		if (this.level.isClientSide) return;
		this.linkable.removeFromNetwork();
	}

	@Override
	public String getClipboardKey() {
		return RedstoneLinkable.CLIPBOARD_KEY;
	}

	public T getLinkable() {
		return linkable;
	}

	public abstract void updateFromLinkable();

	@Override
	public boolean writeToClipboard(final HolderLookup.Provider registries, final CompoundTag tag, final Direction side) {
		tag.put(RedstoneLinkable.FIRST_FREQUENCY, channel.getFirst().write(registries));
		tag.put(RedstoneLinkable.SECOND_FREQUENCY, channel.getSecond().write(registries));
		return true;
	}

	@Override
	public boolean readFromClipboard(final HolderLookup.Provider registries, final CompoundTag tag, final Player player, final Direction side, final boolean simulate) {
		if (simulate) return true;

		for (boolean first : Iterate.trueAndFalse) {
			Tag tmpTag = tag.get(first ? RedstoneLinkable.FIRST_FREQUENCY : RedstoneLinkable.SECOND_FREQUENCY);
			if (tmpTag != null) linkable.setFrequency(first, Frequency.read(tmpTag, registries));
		}
		return true;
	}

	public ItemStackHandler getFrequencyItems() {
		final ItemStackHandler newInv = new ItemStackHandler(2);
		newInv.setStackInSlot(0, this.channel.get(true).stack);
		newInv.setStackInSlot(1, this.channel.get(false).stack);
		return newInv;
	}

	@Override
	public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (!this.initialized) return;
		super.write(compound, registries, false);
		if (clientPacket) {
			compound.put("FrequencyFirst", this.channel.getFirst().write(registries));
			compound.put("FrequencyLast", this.channel.getSecond().write(registries));
		} else {
			compound.putUUID("uuid", this.linkable.uuid);
		}
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(compound, registries, false);
		if (clientPacket) {
			this.channel = Couple.create(Frequency.read(compound.getCompound("FrequencyFirst"), registries), Frequency.read(compound.getCompound("FrequencyLast"), registries));
			return;
		}
		if (compound.contains("uuid")) {
			final RedstoneLinkable tmp = Create.REDSTONE_LINK_NETWORK.getLinkable(compound.getUUID("uuid"));
			if (this.linkableType.isInstance(tmp)) {
				this.linkable = this.linkableType.cast(tmp);
				this.readFromLinkable();
				this.initialized = true;
			} else {
				Create.LOGGER.error("{} for LinkableBlockEntity at {} not found.", this.linkableType.getSimpleName(), this.worldPosition);
			}
		} else if (compound.contains("FrequencyFirst")) {
			this.channel = Couple.create(Frequency.read(compound.getCompound("FrequencyFirst"), registries), Frequency.read(compound.getCompound("FrequencyLast"), registries));
			this.linkable = this.createFallbackLinkable();
			if (compound.getByte("Transmitter") == 0) {
				this.linkable.setMode(false);
				this.linkable.setReceivedStrength(compound.getInt("Receive"));
			} else {
				this.linkable.setTransmittedStrength(compound.getInt("Transmit"));
			}
			this.initialized = true;
		} else {
			Create.LOGGER.error("LinkableBlockEntity at {} did not have recognizable SaveData", this.worldPosition);
		}
		if (!this.initialized) {
			Create.LOGGER.warn("Falling back to creating new {}", this.linkableType.getSimpleName());
		}
	}

	@Override
	public void initialize() {
		this.initialized = true;
		this.channel = Couple.create(Frequency.EMPTY, Frequency.EMPTY);
		this.linkable = this.createFallbackLinkable();
	}

	protected void readFromLinkable() {
		assert this.linkable.network != null;
		this.level = this.linkable.network.level; //Don't use #setLevel, this#initialized is still false here
		this.channel = this.linkable.channel;
		this.setSelfOnLinkable();
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!this.initialized && !level.isClientSide) this.initialize();
	}

	public LinkableBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state, final Class<T> linkableType) {
		super(type, pos, state);
		this.linkableType = linkableType;
	}

	protected abstract T createFallbackLinkable();

	protected abstract void setSelfOnLinkable();
}
