package com.simibubi.create.content.redstone.link.redstoneLink;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.link.interfaces.ILinkableBlockEntity;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.RedstoneLinkable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, MenuProvider, ILinkableBlockEntity {

	public RedstoneLinkLinkable linkable;
	public Couple<Frequency> channel = Couple.create(Frequency.EMPTY, Frequency.EMPTY);

	public FactoryPanelSupportBehaviour panelSupport;

	public ItemStackHandler getFrequencyItems() {
		final ItemStackHandler newInv = new ItemStackHandler(2);
		newInv.setStackInSlot(0, this.channel.get(true).stack);
		newInv.setStackInSlot(1, this.channel.get(false).stack);
		return newInv;
	}

	@Override
	public void remove() {
		assert this.level != null;
		if (this.level.isClientSide) return;
		this.linkable.destroy();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		assert this.level != null;
		if (this.level.isClientSide) return;
		this.linkable.blockEntityUnloaded();
	}

	@Override
	public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (!this.initialized) return;
		super.write(compound, registries, false);
		if (clientPacket) {
			compound.put("FrequencyFirst", this.channel.getFirst().write());
			compound.put("FrequencyLast", this.channel.getSecond().write());
		} else {
			compound.putUUID("uuid", this.linkable.uuid);
		}
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		if (!this.initialized && !level.isClientSide) this.initialize();
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
			if (tmp instanceof final RedstoneLinkLinkable linkable) {
				this.linkable = linkable;
				this.readFromLinkable();
				this.initialized = true;
			} else {
				Create.LOGGER.error("RedstoneLinkLinkable for RedstoneLinkBlockEntity at {} not found.", this.worldPosition);
			}
		} else if (compound.contains("FrequencyFirst")) {
			this.channel = Couple.create(Frequency.read(compound.getCompound("FrequencyFirst"), registries), Frequency.read(compound.getCompound("FrequencyLast"), registries));
			this.linkable = new RedstoneLinkLinkable(this.channel, this);
			if (compound.getByte("Transmitter") == 0) {
				this.linkable.setMode(false);
				this.linkable.setReceivedStrength(compound.getInt("Receive"));
			} else {
				this.linkable.setTransmittedStrength(compound.getInt("Transmit"));
			}
			this.initialized = true;
		} else {
			Create.LOGGER.error("RedstoneLinkBlockEntity at {} did not have recognizable SaveData", this.worldPosition);
		}
		if (!this.initialized) {
			Create.LOGGER.warn("Falling back to creating new RedstoneLinkLinkable");
		}
	}

	@Override
	public void initialize() {
		this.initialized = true;
		this.channel = Couple.create(Frequency.EMPTY, Frequency.EMPTY);
		this.linkable = new RedstoneLinkLinkable(this.channel, this);
	}

	private void readFromLinkable() {
		assert this.linkable.network != null;
		this.level = this.linkable.network.level; //Don't use #setLevel, this#initialized is still false here
		this.channel = this.linkable.channel;
		this.linkable.setBlockEntity(this);
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
		//TODO: refactor Factory Panels to run serverside only and sync data for visuals to client
		behaviours.add(this.panelSupport = new FactoryPanelSupportBehaviour(this));
	}

	public RedstoneLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public Component getDisplayName() {
		return AllBlocks.REDSTONE_LINK.get().getName();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(final int i, final Inventory inventory, final Player player) {
		return RedstoneLinkMenu.create(i, inventory, this);
	}

	@Override
	public RedstoneLinkable getLinkable() {
		return this.linkable;
	}
}
