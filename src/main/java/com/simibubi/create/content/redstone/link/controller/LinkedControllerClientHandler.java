package com.simibubi.create.content.redstone.link.controller;

import java.util.BitSet;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.controller.packet.LinkedControllerCopyChannelPacket;
import com.simibubi.create.content.redstone.link.controller.packet.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.packet.LinkedControllerStopLecternPacket;
import com.simibubi.create.content.redstone.link.interfaces.ILinkableBlockEntity;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class LinkedControllerClientHandler {

	public static Mode MODE = Mode.IDLE;
	public static final int PACKET_RATE = 10;
	public static BitSet currentlyPressed = new BitSet(6);
	private static @Nullable BlockPos lecternPos;
	private static BlockPos selectedLocation = BlockPos.ZERO;
	private static int packetCooldown = LinkedControllerClientHandler.PACKET_RATE;

	public static void activateInLectern(final BlockPos worldPosition) {
		if (LinkedControllerClientHandler.MODE == Mode.IDLE) {
			LinkedControllerClientHandler.MODE = Mode.ACTIVE;
			LinkedControllerClientHandler.lecternPos = worldPosition;
		}
	}

	public static void tick() {
		LinkedControllerItemRenderer.tick();

		if (LinkedControllerClientHandler.MODE == Mode.IDLE) return;
		if (LinkedControllerClientHandler.packetCooldown > 0) LinkedControllerClientHandler.packetCooldown--;

		final Minecraft mc = Minecraft.getInstance();
		final LocalPlayer player = mc.player;
		assert player != null;

		if (LinkedControllerClientHandler.shouldReset(player, mc)) {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
			return;
		}

		if (LinkedControllerClientHandler.inLectern() && AllBlocks.LECTERN_CONTROLLER.get().getBlockEntityOptional(mc.level, LinkedControllerClientHandler.lecternPos).map(be -> !be.isUsedBy(mc.player)).orElse(true)) {
			LinkedControllerClientHandler.deactivateInLectern();
			return;
		}

		final List<KeyMapping> controls = ControlsUtil.getControls();
		final BitSet pressedKeys = new BitSet(6);

		for (int i = 0; i < controls.size(); i++) {
			if (ControlsUtil.isActuallyPressed(controls.get(i))) {
				pressedKeys.set(i);
			}
		}

		final boolean changed = pressedKeys.hashCode() != LinkedControllerClientHandler.currentlyPressed.hashCode();

		if (LinkedControllerClientHandler.MODE == Mode.ACTIVE) {
			if (changed) {
				CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(pressedKeys, LinkedControllerClientHandler.lecternPos));
				LinkedControllerClientHandler.packetCooldown = LinkedControllerClientHandler.PACKET_RATE;
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1.0f, 0.5f, true);
			} else if (LinkedControllerClientHandler.packetCooldown == 0 && !pressedKeys.isEmpty()) {
				CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(pressedKeys, LinkedControllerClientHandler.lecternPos));
				LinkedControllerClientHandler.packetCooldown = LinkedControllerClientHandler.PACKET_RATE;
			}
		}

		if (LinkedControllerClientHandler.MODE == Mode.BIND) {
			assert mc.level != null;
			final VoxelShape shape = mc.level.getBlockState(LinkedControllerClientHandler.selectedLocation).getShape(mc.level, LinkedControllerClientHandler.selectedLocation);
			if (!shape.isEmpty()) {
				Outliner.getInstance().showAABB("controller", shape.bounds().move(LinkedControllerClientHandler.selectedLocation)).colored(0xB73C2D).lineWidth(1 / 16.0f);
			}

			if (changed) {
				final int set = pressedKeys.nextSetBit(0);
				if (set > -1) {
					final BlockEntity be = mc.level.getBlockEntity(LinkedControllerClientHandler.selectedLocation);
					if (be instanceof ILinkableBlockEntity) {
						CatnipServices.NETWORK.sendToServer(new LinkedControllerCopyChannelPacket(set, LinkedControllerClientHandler.selectedLocation));
						CreateLang.translate("linked_controller.key_bound", controls.get(set).getTranslatedKeyMessage().getString()).sendStatus(mc.player);
					}
					LinkedControllerClientHandler.MODE = Mode.IDLE;
				}
			}
		}

		LinkedControllerClientHandler.currentlyPressed = pressedKeys;
		controls.forEach(kb -> kb.setDown(false));
	}

	private static boolean shouldReset(final LocalPlayer player, final Minecraft mc) {
		return (player.isSpectator() || mc.screen != null || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE) || (!LinkedControllerClientHandler.inLectern() && !AllItems.LINKED_CONTROLLER.isIn(player.getMainHandItem()) && !AllItems.LINKED_CONTROLLER.isIn(player.getOffhandItem())));
	}

	public static void deactivateInLectern() {
		if (LinkedControllerClientHandler.MODE == Mode.ACTIVE && LinkedControllerClientHandler.inLectern()) {
			LinkedControllerClientHandler.MODE = Mode.IDLE;
			LinkedControllerClientHandler.onReset();
		}
	}

	public static void toggleBindMode(final BlockPos pos) {
		if (LinkedControllerClientHandler.MODE == Mode.IDLE) {
			LinkedControllerClientHandler.MODE = Mode.BIND;
			LinkedControllerClientHandler.selectedLocation = pos;
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
			CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(new BitSet(6), null));
		}
		LinkedControllerClientHandler.currentlyPressed.clear();

		LinkedControllerItemRenderer.resetButtons();
	}
}
