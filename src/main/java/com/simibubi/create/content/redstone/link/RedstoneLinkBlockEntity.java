package com.simibubi.create.content.redstone.link;

import java.util.List;

import com.simibubi.create.Create;
import com.simibubi.create.CreateBuildInfo;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.data.Couple;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.NonNull;

public class RedstoneLinkBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public RedstoneLinkLinkable linkable;
	private Couple<Frequency> channel;

	@Override
	public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (!this.initialized || clientPacket) return;
		super.write(compound, registries, false);
		compound.putUUID("uuid", this.linkable.uuid);
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if(!initialized && !level.isClientSide) this.initialize();
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (clientPacket) return;
		super.read(compound, registries, false);
		if (compound.contains("uuid")) {
			final RedstoneLinkable tmp = Create.REDSTONE_LINK_NETWORK.getLinkable(compound.getUUID("uuid"));
			if (tmp instanceof final RedstoneLinkLinkable linkable) {
				this.linkable = linkable;
				readFromLinkable();
				this.initialized = true;
			} else {
				Create.LOGGER.error("RedstoneLinkLinkable for RedstoneLinkBlockEntity at {} not found.", this.worldPosition);
			}
		} else if (compound.contains("FrequencyFirst")) {
			this.channel = Couple.create(Frequency.read(compound.getCompound("FrequencyFirst"), registries), Frequency.read(compound.getCompound("FrequencyLast"), registries));
			this.linkable = new RedstoneLinkLinkable(this.channel, this);
			if(compound.getByte("Transmitter") == 0){
				this.linkable.setMode(false);
				this.linkable.setReceivedStrength(compound.getInt("Receive"));
			}else {
				this.linkable.setTransmittedStrength(compound.getInt("Transmit"));
			}
			this.initialized = true;
		}else {
			Create.LOGGER.error("RedstoneLinkBlockEntity at {} did not have recognizable SaveData", this.worldPosition);
		}
		if(!this.initialized){
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
		this.level = this.linkable.network.level; //Don't use #setLevel, this#initialized is still false here
		this.channel = linkable.channel;
		this.linkable.setBlockEntity(this);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

	}

	public RedstoneLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
