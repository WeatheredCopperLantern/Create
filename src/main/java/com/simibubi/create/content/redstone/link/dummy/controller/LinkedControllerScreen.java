package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.element.GuiGameElement;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.google.common.collect.ImmutableList;
import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

public class LinkedControllerScreen extends AbstractSimiContainerScreen<LinkedControllerMenu> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public LinkedControllerScreen(final LinkedControllerMenu menu, final Inventory inv, final Component title) {
		super(menu, inv, title);
		this.background = AllGuiTextures.LINKED_CONTROLLER;
	}

	@Override
	protected void init() {
		this.setWindowSize(this.background.getWidth(), this.background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
		this.setWindowOffset(1, 0);
		super.init();

		final int x = this.leftPos;
		final int y = this.topPos;

		this.resetButton = new IconButton(x + this.background.getWidth() - 62, y + this.background.getHeight() - 24, AllIcons.I_TRASH);
		this.resetButton.withCallback(() -> {
			this.menu.clearContents();
			this.menu.sendClearPacket();
		});
		this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
		this.confirmButton.withCallback(() -> {
			this.minecraft.player.closeContainer();
		});

		this.addRenderableWidget(this.resetButton);
		this.addRenderableWidget(this.confirmButton);

		this.extraAreas = ImmutableList.of(new Rect2i(x + this.background.getWidth() + 4, y + this.background.getHeight() - 44, 64, 56));
	}

	@Override
	protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
		final int invX = this.getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		final int invY = this.topPos + this.background.getHeight() + 4;
		this.renderPlayerInventory(graphics, invX, invY);

		final int x = this.leftPos;
		final int y = this.topPos;

		this.background.render(graphics, x, y);
		graphics.drawString(this.font, this.title, x + 15, y + 4, 0x592424, false);

		GuiGameElement.of(this.menu.contentHolder).<GuiGameElement.GuiRenderBuilder>at(x + this.background.getWidth() - 4, y + this.background.getHeight() - 56, -200).scale(5).render(graphics);
	}

	@Override
	protected void containerTick() {
		if (!ItemStack.matches(this.menu.player.getMainHandItem(), this.menu.contentHolder)) this.menu.player.closeContainer();

		super.containerTick();
	}

	@Override
	protected void renderTooltip(final GuiGraphics graphics, final int x, final int y) {
		if (!this.menu.getCarried().isEmpty() || this.hoveredSlot == null || this.hoveredSlot.container == this.menu.playerInventory) {
			super.renderTooltip(graphics, x, y);
			return;
		}

		List<Component> list = new LinkedList<>();
		if (this.hoveredSlot.hasItem()) list = this.getTooltipFromContainerItem(this.hoveredSlot.getItem());

		graphics.renderComponentTooltip(this.font, this.addToTooltip(list, this.hoveredSlot.getSlotIndex()), x, y);
	}

	private List<Component> addToTooltip(final List<Component> list, final int slot) {
		if (slot < 0 || slot >= 12) return list;
		list.add(CreateLang.translateDirect("linked_controller.frequency_slot_" + ((slot % 2) + 1), ControlsUtil.getControls().get(slot / 2).getTranslatedKeyMessage().getString()).withStyle(ChatFormatting.GOLD));
		return list;
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return this.extraAreas;
	}
}
