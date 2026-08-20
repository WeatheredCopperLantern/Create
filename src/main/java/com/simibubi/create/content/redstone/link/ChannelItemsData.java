package com.simibubi.create.content.redstone.link;

import java.util.function.Consumer;

import com.simibubi.create.content.redstone.link.linkable.Frequency;
import net.createmod.catnip.data.ImmutableCouple;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import com.mojang.serialization.Codec;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChannelItemsData(ImmutableCouple<Frequency> channel) implements TooltipProvider {

	public static final Codec<ChannelItemsData> CODEC = ImmutableCouple.codec(Frequency.CODEC).xmap(ChannelItemsData::new, ChannelItemsData::channel);

	public static final StreamCodec<RegistryFriendlyByteBuf, ChannelItemsData> STREAM_CODEC = ImmutableCouple.streamCodec(Frequency.STREAM_CODEC).map(ChannelItemsData::new, ChannelItemsData::channel);

	@Override
	public void addToTooltip(final Item.TooltipContext tooltipContext, final Consumer<Component> consumer, final TooltipFlag tooltipFlag) {
		consumer.accept(CommonComponents.EMPTY);
		consumer.accept(Component.literal("Channel Items:").withStyle(ChatFormatting.GRAY));
		channel.forEach(frequency -> consumer.accept(Component.empty().append(frequency.stack.getHoverName()).withStyle(ChatFormatting.DARK_AQUA)));
	}
}
