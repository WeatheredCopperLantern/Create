package com.simibubi.create.content.redstone.link;

import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe.SupportsItemCopying;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkItem extends BlockItem implements SupportsItemCopying {

	@Override
	public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltipComponents, final TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

		final BlockItemStateProperties state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
		final boolean transmitter = Boolean.FALSE.equals(state.get(RedstoneLinkBlock.RECEIVER));

		tooltipComponents.add(CommonComponents.EMPTY);
		tooltipComponents.add(Component.translatable("item.create.transmitter").append(Component.literal(": " + transmitter)).withStyle(ChatFormatting.GOLD));
	}

	@Override
	public DataComponentType<?> getComponentType() {
		return AllDataComponents.CHANNEL_ITEMS;
	}

	public RedstoneLinkItem(final Block block, final Properties properties) {
		super(block, properties);
	}
}
