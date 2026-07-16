package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LinkedControllerScreen extends AbstractSimiContainerScreen<LinkedControllerMenu> {

	protected static final AllGuiTextures BG = AllGuiTextures.LINKED_CONTROLLER;

	@Override
	protected void init() {
		this.setWindowSize(LinkedControllerScreen.BG.getWidth(), LinkedControllerScreen.BG.getHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		this.setWindowOffset(1, 0);
		super.init();

		final int x = this.leftPos;
		final int y = this.topPos;

		final IconButton resetButton = new IconButton(x + LinkedControllerScreen.BG.getWidth() - 62, y + LinkedControllerScreen.BG.getHeight() - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			this.menu.clearContents();
			this.menu.sendClearPacket();
		});
		this.addRenderableWidget(resetButton);

		final IconButton confirmButton = new IconButton(x + LinkedControllerScreen.BG.getWidth() - 33, y + LinkedControllerScreen.BG.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			assert Objects.requireNonNull(this.minecraft).player != null;
			this.minecraft.player.closeContainer();
		});
		this.addRenderableWidget(confirmButton);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
		final int invX = this.getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		final int invY = this.topPos + LinkedControllerScreen.BG.getHeight() + 4;
		this.renderPlayerInventory(guiGraphics, invX, invY);

		final int x = this.leftPos;
		final int y = this.topPos;

		LinkedControllerScreen.BG.render(guiGraphics, x, y);
		guiGraphics.drawString(this.font, this.title, x + 15, y + 4, 0x592424, false);

		GuiGameElement.of(this.menu.contentHolder).<GuiGameElement.GuiRenderBuilder>at(x + LinkedControllerScreen.BG.getWidth() - 4, y + LinkedControllerScreen.BG.getHeight() - 56, -200).scale(5).render(guiGraphics);

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
	protected void containerTick() {
		if (!ItemStack.matches(this.menu.player.getMainHandItem(), this.menu.contentHolder)) this.menu.player.closeContainer();

		super.containerTick();
	}

	public LinkedControllerScreen(LinkedControllerMenu container, Inventory inv, Component title) {
		super(container, inv, title);
	}
}
