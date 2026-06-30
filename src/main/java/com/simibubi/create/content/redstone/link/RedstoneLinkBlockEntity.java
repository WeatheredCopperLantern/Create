package com.simibubi.create.content.redstone.link;

import java.util.List;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.data.Couple;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.NonNull;

public class RedstoneLinkBlockEntity extends SmartBlockEntity {

	public RedstoneLinkLinkable linkable;
	private Couple<Frequency> channel;

	public RedstoneLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState) {
		super(type, pos, blockState);
	}

	@Override
	public void initialize() {
		this.initialized = true;
		if(this.linkable == null){
			this.channel = Couple.create(Frequency.EMPTY, Frequency.EMPTY);
			this.linkable = new RedstoneLinkLinkable(this.channel, this);
		}else {
			this.readFromLinkable();
		}
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

	}

	@Override
	public void destroy() {

	}

	public void receive(int strength){
		level.setBlock(getBlockPos(), getBlockState().setValue(RedstoneLinkBlock.POWERED, strength > 0), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
	}

	@Override
	public void saveToItem(final @NonNull ItemStack stack, final HolderLookup.@NonNull Provider registries) {
		//stack.remove(DataComponents.BLOCK_ENTITY_DATA);
		//final CompoundTag compoundtag = this.getSaveToItemData(registries);
		//if (!compoundtag.isEmpty()) {
		//	BlockItem.setBlockEntityData(stack, this.getType(), compoundtag);
		//	stack.applyComponents(this.collectComponents());
		//}
	}

	private CompoundTag getSaveToItemData(final HolderLookup.Provider registries) {
		//channel.forEachWithParams((frequency, name) -> channelNBT.put(name, frequency.write()), Couple.create("Frequency_1", "Frequency_2"));
		return new CompoundTag();
	}

	@Override
	public void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (!this.initialized || clientPacket || (this.level != null && this.level.isClientSide)) return;
		super.write(compound, registries, false);
		compound.putUUID("uuid", this.linkable.uuid);
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		if (clientPacket || (this.level != null && this.level.isClientSide)) return;
		super.read(compound, registries, false);
		if (compound.contains("uuid")) {
			final RedstoneLinkable tmp = Create.REDSTONE_LINK_NETWORK.getLinkable(compound.getUUID("uuid"));
			if (tmp instanceof final RedstoneLinkLinkable linkable) {
				this.channel = linkable.channel;
				linkable.setBlockEntity(this);
				this.linkable = linkable;
			} else {
				Create.LOGGER.error("RedstoneLinkLinkable for RedstoneLinkBlockEntity at {} not found.", this.worldPosition);
			}
		}
	}

	private void readFromLinkable(){
		if(this.linkable.isReceiver()){
			this.receive(this.linkable.getReceivedStrength());
		}
	}

	public void checkAntenna() {
		final BlockState state = this.getBlockState();
		assert this.level != null;
		if (state.getValue(RedstoneLinkBlock.ROTATED_ANTENNA) == (this.level.getBlockState(this.getBlockPos().above()).isAir() && state.getValue(DirectionalBlock.FACING).getAxis() != Direction.Axis.Y)) {
			this.level.setBlock(this.getBlockPos(), state.cycle(RedstoneLinkBlock.ROTATED_ANTENNA), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
		}
	}

	public void delayedUpdate() {
		final BlockState state = this.getBlockState();
		assert this.level != null;
		if (!RedstoneLinkBlock.canSurviveStatic(state, this.level, this.getBlockPos())) {
			this.level.destroyBlock(this.getBlockPos(), true);
			return;
		}

		this.checkAntenna();

		if (this.linkable.isReceiver()) return;
		//final Boolean tri = panelSupport.shouldBePoweredTristate();
		//final int powerFromPanels = (tri == null) ? -1 : (tri) ? 15 : 0;

		// Suppress update if an input panel exists but is not loaded
		//if (powerFromPanels == -1) return;

		int power = this.level.getBestNeighborSignal(this.getBlockPos());
		//power = Math.max(power, powerFromPanels);

		final boolean previouslyPowered = state.getValue(RedstoneLinkBlock.POWERED);
		if (previouslyPowered != power > 0) {
			this.level.setBlock(this.getBlockPos(), this.getBlockState().cycle(RedstoneLinkBlock.POWERED), Block.UPDATE_CLIENTS + Block.UPDATE_KNOWN_SHAPE);
		}

		this.linkable.setTransmittedStrength(power);
	}
}
