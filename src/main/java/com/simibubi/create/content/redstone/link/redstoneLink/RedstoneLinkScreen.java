package com.simibubi.create.content.redstone.link.redstoneLink;

import java.util.Objects;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.GuiGameElement;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.DirectionalBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import org.jspecify.annotations.NonNull;

public class RedstoneLinkScreen extends AbstractSimiContainerScreen<RedstoneLinkMenu> {

	protected static final AllGuiTextures BG;

	static {
		BG = AllGuiTextures.REDSTONE_LINK;
	}

	public RedstoneLinkScreen(final RedstoneLinkMenu container, final Inventory inv, final Component title) {
		super(container, inv, title);
		this.init();
	}

	@Override
	protected void init() {
		this.setWindowSize(30 + RedstoneLinkScreen.BG.getWidth(), RedstoneLinkScreen.BG.getHeight() + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		this.setWindowOffset(-11, 0);
		super.init();

		final IconButton resetButton = new IconButton(this.leftPos + 38, this.topPos + RedstoneLinkScreen.BG.getHeight() - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			this.menu.clearContents();
			this.menu.sendClearPacket();
		});
		this.addRenderableWidget(resetButton);

		final IconButton confirmButton = new IconButton(this.leftPos + 30 + RedstoneLinkScreen.BG.getWidth() - 33, this.topPos + RedstoneLinkScreen.BG.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			assert Objects.requireNonNull(this.minecraft).player != null;
			this.minecraft.player.closeContainer();
		});
		this.addRenderableWidget(confirmButton);
	}

	@Override
	protected void renderBg(final @NonNull GuiGraphics guiGraphics, final float v, final int i, final int i1) {
		final int invX = this.getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth());
		final int invY = this.topPos + RedstoneLinkScreen.BG.getHeight() + 4;
		this.renderPlayerInventory(guiGraphics, invX, invY);

		final int x = this.leftPos + this.imageWidth - RedstoneLinkScreen.BG.getWidth();
		final int y = this.topPos;

		RedstoneLinkScreen.BG.render(guiGraphics, x, y);
		guiGraphics.drawString(this.font, this.title, x + 5, y + 4, 0x592424, false);

		final PoseStack ms = guiGraphics.pose();
		TransformStack.of(ms).pushPose().translate(this.leftPos + this.imageWidth + 40, y + this.imageHeight - AllGuiTextures.PLAYER_INVENTORY.getHeight(), 100).scale(50).rotateXDegrees(-22).rotateYDegrees(-202);

		GuiGameElement.of(this.menu.contentHolder.getBlockState().setValue(DirectionalBlock.FACING, Direction.UP).setValue(RedstoneLinkBlock.ROTATED_ANTENNA, true)).render(guiGraphics);
		ms.popPose();
	}
}
