package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllBlocks;

import com.simibubi.create.Create;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class RedstoneLinkItem extends BlockItem {

	public RedstoneLinkItem(final Properties builder) {
		super(AllBlocks.REDSTONE_LINK.get(), builder);
	}

	@Override
	protected boolean updateCustomBlockEntityTag(final @NonNull BlockPos pos, final @NonNull Level level, @Nullable final Player player, final @NonNull ItemStack stack, final @NonNull BlockState state) {
		if (level.isClientSide) return false;

		final CustomData customdata = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
		if (!customdata.isEmpty()) {
			final BlockEntity blockentity = level.getBlockEntity(pos);
			if (blockentity != null) {
				final CompoundTag tag = customdata.copyTag();

				tag.remove("uuid");

				return CustomData.of(tag).loadInto(blockentity, level.registryAccess());
			}
		}
		return true;
	}

	@Override
	public @NonNull InteractionResult interactLivingEntity(@NonNull ItemStack stack, @NonNull Player player, @NonNull LivingEntity interactionTarget, @NonNull InteractionHand usedHand) {
		if (!player.level().isClientSide && interactionTarget.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			Create.LOGGER.warn("aaa");
			interactionTarget.setItemSlot(EquipmentSlot.HEAD, stack.copy());

			new RedstoneEntityLinkableImp(Couple.create(Frequency.EMPTY, Frequency.EMPTY), interactionTarget);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}
}
