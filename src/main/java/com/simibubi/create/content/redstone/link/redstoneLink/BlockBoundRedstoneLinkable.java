package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.ChannelItemsData;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.foundation.persistent.BlockBoundObject;

import net.createmod.catnip.data.ImmutableCouple;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockBoundRedstoneLinkable extends BlockBoundObject {

	private ImmutableCouple<Frequency> channel;

	protected BlockBoundRedstoneLinkable(final Block block, final BlockPos blockPos, final BlockState state, final ServerLevel serverLevel) {
		super(block, blockPos, state, serverLevel);
		channel = ImmutableCouple.create(Frequency.EMPTY, Frequency.EMPTY);
	}

	protected BlockBoundRedstoneLinkable(final Block block, final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		super(block, compoundTag, registries, serverLevel);
		channel = ImmutableCouple.create(Frequency.EMPTY, Frequency.EMPTY);
	}

	public ImmutableCouple<Frequency> getChannel() {
		return this.channel;
	}

	@OverridingMethodsMustInvokeSuper
	@Override
	public void applyComponents(final ItemStack itemStack) {
		final ChannelItemsData channelItemsData = itemStack.get(AllDataComponents.CHANNEL_ITEMS);
		if (channelItemsData != null) {
			this.channel = channelItemsData.channel();
		}
	}

	@OverridingMethodsMustInvokeSuper
	@Override
	protected void collectComponents(final DataComponentMap.Builder builder) {
		builder.set(AllDataComponents.CHANNEL_ITEMS, new ChannelItemsData(this.channel));
	}
}
