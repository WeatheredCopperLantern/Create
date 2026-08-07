package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.content.redstone.link.linkable.BlockBoundRedstoneLinkable;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.content.redstone.link.linkable.ILinkableBlockEntity;
import com.simibubi.create.content.redstone.link.linkable.LinkableBehaviour;
import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RedstoneLinkMenu extends GhostItemMenu<LinkableBehaviour<?>> {

	public RedstoneLinkMenu(final MenuType<?> type, final int id, final Inventory inv, final RegistryFriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public RedstoneLinkMenu(final MenuType<?> type, final int id, final Inventory inv, final LinkableBehaviour<?> contentHolder) {
		super(type, id, inv, contentHolder);
	}

	@Override
	protected LinkableBehaviour<?> createOnClient(final RegistryFriendlyByteBuf extraData) {
		assert Minecraft.getInstance().level != null;
		final BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
		if (blockEntity instanceof final ILinkableBlockEntity lbe) {
			return lbe.getLinkableBehaviour();
		}
		return null;
	}

	public static RedstoneLinkMenu create(final int id, final Inventory inv, final LinkableBehaviour<?> be) {
		return new RedstoneLinkMenu(AllMenuTypes.REDSTONE_LINK.get(), id, inv, be);
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return this.contentHolder.getFrequencyItems();
	}

	@Override
	protected boolean allowRepeats() {
		return true;
	}

	@Override
	protected void addSlots() {
		this.addPlayerSlots(4, 103);

		for (int i = 0; i < 2; i++) {
			this.addSlot(new SlotItemHandler(this.ghostInventory, i, 67 + (i * 18), 25));
		}
	}

	@Override
	protected void saveData(final LinkableBehaviour<?> contentHolder) {
		final BlockEntity be = contentHolder.blockEntity;
		assert be.getLevel() != null;
		if (be.getLevel().isClientSide) return;

		final BlockState blockState = be.getBlockState();
		final Level world = be.getLevel();
		final BlockPos pos = be.getBlockPos();

		if ((blockState.getBlock() instanceof final IBBO<?> ibbo && ibbo.getBlockBoundObject(world, pos) instanceof BlockBoundRedstoneLinkable blockBoundRedstoneLinkable)) {
			if (blockBoundRedstoneLinkable.setFrequency(true, Frequency.of(this.ghostInventory.getStackInSlot(0))) | blockBoundRedstoneLinkable.setFrequency(false, Frequency.of(this.ghostInventory.getStackInSlot(1)))) {
				world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1f);
			}
		}
	}
}
