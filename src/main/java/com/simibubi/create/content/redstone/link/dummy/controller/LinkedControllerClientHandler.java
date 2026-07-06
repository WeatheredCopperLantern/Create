package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.dummy.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

public class LinkedControllerClientHandler {

	public static final LayeredDraw.Layer OVERLAY = LinkedControllerClientHandler::renderOverlay;

	public static Mode MODE = Mode.IDLE;
	public static int PACKET_RATE = 5;
	public static Collection<Integer> currentlyPressed = new HashSet<>();
	private static BlockPos lecternPos;
	private static BlockPos selectedLocation = BlockPos.ZERO;
	private static int packetCooldown;

	public static void toggleBindMode(final BlockPos location) {
		if (LinkedControllerClientHandler.MODE == Mode.IDLE) {
			LinkedControllerClientHandler.MODE = Mode.BIND;
			LinkedControllerClientHandler.selectedLocation = location;
		} else {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
		}
	}

	public static void toggle() {
		if (LinkedControllerClientHandler.MODE == Mode.IDLE) {
			LinkedControllerClientHandler.MODE = Mode.ACTIVE;
			LinkedControllerClientHandler.lecternPos = null;
		} else {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
		}
	}

	public static void activateInLectern(final BlockPos lecternAt) {
		if (LinkedControllerClientHandler.MODE == Mode.IDLE) {
			LinkedControllerClientHandler.MODE = Mode.ACTIVE;
			LinkedControllerClientHandler.lecternPos = lecternAt;
		}
	}

	public static void deactivateInLectern() {
		if (LinkedControllerClientHandler.MODE == Mode.ACTIVE && LinkedControllerClientHandler.inLectern()) {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
		}
	}

	public static boolean inLectern() {
		return LinkedControllerClientHandler.lecternPos != null;
	}

	protected static void onReset() {
		ControlsUtil.getControls().forEach(kb -> kb.setDown(ControlsUtil.isActuallyPressed(kb)));
		LinkedControllerClientHandler.packetCooldown = 0;
		LinkedControllerClientHandler.selectedLocation = BlockPos.ZERO;

		if (LinkedControllerClientHandler.inLectern()) {
			CatnipServices.NETWORK.sendToServer(new LinkedControllerStopLecternPacket(LinkedControllerClientHandler.lecternPos));
		}
		LinkedControllerClientHandler.lecternPos = null;

		if (!LinkedControllerClientHandler.currentlyPressed.isEmpty()) {
			CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(LinkedControllerClientHandler.currentlyPressed, false));
		}
		LinkedControllerClientHandler.currentlyPressed.clear();

		LinkedControllerItemRenderer.resetButtons();
	}

	public static void tick() {
		LinkedControllerItemRenderer.tick();

		if (LinkedControllerClientHandler.MODE == Mode.IDLE) return;
		if (LinkedControllerClientHandler.packetCooldown > 0) LinkedControllerClientHandler.packetCooldown--;

		final Minecraft mc = Minecraft.getInstance();
		final LocalPlayer player = mc.player;
		final ItemStack heldItem = player.getMainHandItem();

		if (player.isSpectator()) {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
			return;
		}

		//if (!inLectern() && !AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
		//	heldItem = player.getOffhandItem();
		//	if (!AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
		//		MODE = Mode.IDLE;
		//		onReset();
		//		return;
		//	}
		//}

		//if (inLectern() && AllBlocks.LECTERN_CONTROLLER.get()
		//	.getBlockEntityOptional(mc.level, lecternPos)
		//	.map(be -> !be.isUsedBy(mc.player))
		//	.orElse(true)) {
		//	deactivateInLectern();
		//	return;
		//}

		if (mc.screen != null) {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
			return;
		}

		if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
			return;
		}

		final List<KeyMapping> controls = ControlsUtil.getControls();
		final Collection<Integer> pressedKeys = new HashSet<>();
		for (int i = 0; i < controls.size(); i++) {
			if (ControlsUtil.isActuallyPressed(controls.get(i))) pressedKeys.add(i);
		}

		final Collection<Integer> newKeys = new HashSet<>(pressedKeys);
		final Collection<Integer> releasedKeys = LinkedControllerClientHandler.currentlyPressed;
		newKeys.removeAll(releasedKeys);
		releasedKeys.removeAll(pressedKeys);

		if (LinkedControllerClientHandler.MODE == Mode.ACTIVE) {
			// Released Keys
			if (!releasedKeys.isEmpty()) {
				CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(releasedKeys, false, LinkedControllerClientHandler.lecternPos));
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1.0f, 0.5f, true);
			}

			// Newly Pressed Keys
			if (!newKeys.isEmpty()) {
				CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(newKeys, true, LinkedControllerClientHandler.lecternPos));
				LinkedControllerClientHandler.packetCooldown = LinkedControllerClientHandler.PACKET_RATE;
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1.0f, 0.75f, true);
			}

			// Keepalive Pressed Keys
			if (LinkedControllerClientHandler.packetCooldown == 0) {
				if (!pressedKeys.isEmpty()) {
					CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(pressedKeys, true, LinkedControllerClientHandler.lecternPos));
					LinkedControllerClientHandler.packetCooldown = LinkedControllerClientHandler.PACKET_RATE;
				}
			}
		}

		if (LinkedControllerClientHandler.MODE == Mode.BIND) {
			final VoxelShape shape = mc.level.getBlockState(LinkedControllerClientHandler.selectedLocation).getShape(mc.level, LinkedControllerClientHandler.selectedLocation);
			if (!shape.isEmpty()) {
				Outliner.getInstance().showAABB("controller", shape.bounds().move(LinkedControllerClientHandler.selectedLocation)).colored(0xB73C2D).lineWidth(1 / 16.0f);
			}

			for (final Integer integer : newKeys) {
				final LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(mc.level, LinkedControllerClientHandler.selectedLocation, LinkBehaviour.TYPE);
				if (linkBehaviour != null) {
					CatnipServices.NETWORK.sendToServer(new LinkedControllerBindPacket(integer, LinkedControllerClientHandler.selectedLocation));
					CreateLang.translate("linked_controller.key_bound", controls.get(integer).getTranslatedKeyMessage().getString()).sendStatus(mc.player);
				}
				LinkedControllerClientHandler.MODE = Mode.IDLE;
				break;
			}
		}

		LinkedControllerClientHandler.currentlyPressed = pressedKeys;
		controls.forEach(kb -> kb.setDown(false));
	}

	public static void renderOverlay(final GuiGraphics guiGraphics, final DeltaTracker deltaTracker) {
		final int width1 = guiGraphics.guiWidth();
		final int height1 = guiGraphics.guiHeight();
		final Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;

		if (LinkedControllerClientHandler.MODE != Mode.BIND) return;

		final PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		final Screen tooltipScreen = new Screen(CommonComponents.EMPTY) {
		};
		tooltipScreen.init(mc, width1, height1);

		final Object[] keys = new Object[6];
		final List<KeyMapping> controls = ControlsUtil.getControls();
		for (int i = 0; i < controls.size(); i++) {
			final KeyMapping keyBinding = controls.get(i);
			keys[i] = keyBinding.getTranslatedKeyMessage().getString();
		}

		final List<Component> list = new ArrayList<>();
		list.add(CreateLang.translateDirect("linked_controller.bind_mode").withStyle(ChatFormatting.GOLD));
		list.addAll(TooltipHelper.cutTextComponent(CreateLang.translateDirect("linked_controller.press_keybind", keys), FontHelper.Palette.ALL_GRAY));

		int width = 0;
		final int height = list.size() * mc.font.lineHeight;
		for (final Component iTextComponent : list) {
			width = Math.max(width, mc.font.width(iTextComponent));
		}
		final int x = (width1 / 3) - width / 2;
		final int y = height1 - height - 24;

		// TODO
		guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, list, x, y);

		poseStack.popPose();
	}

	public enum Mode {
		IDLE, ACTIVE, BIND
	}
}
