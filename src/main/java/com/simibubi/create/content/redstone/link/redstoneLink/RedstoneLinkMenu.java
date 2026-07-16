package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.content.redstone.link.linkable.Frequency;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RedstoneLinkMenu extends GhostItemMenu<RedstoneLinkBlockEntity> {

	public RedstoneLinkMenu(final MenuType<?> type, final int id, final Inventory inv, final RegistryFriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public RedstoneLinkMenu(final MenuType<?> type, final int id, final Inventory inv, final RedstoneLinkBlockEntity contentHolder) {
		super(type, id, inv, contentHolder);
	}

	@Override
	protected RedstoneLinkBlockEntity createOnClient(final RegistryFriendlyByteBuf extraData) {
		assert Minecraft.getInstance().level != null;
		final BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
		if (blockEntity instanceof final RedstoneLinkBlockEntity rlbe) {
			return rlbe;
		}
		return null;
	}

	public static RedstoneLinkMenu create(final int id, final Inventory inv, final RedstoneLinkBlockEntity be) {
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
	protected void saveData(final RedstoneLinkBlockEntity contentHolder) {
		assert contentHolder.getLevel() != null;
		if (contentHolder.getLevel().isClientSide) return;
		contentHolder.linkable.setFrequency(true, Frequency.of(this.ghostInventory.getStackInSlot(0)));
		contentHolder.linkable.setFrequency(false, Frequency.of(this.ghostInventory.getStackInSlot(1)));
	}
}
