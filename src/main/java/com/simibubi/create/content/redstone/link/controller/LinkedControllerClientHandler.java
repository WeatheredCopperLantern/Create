package com.simibubi.create.content.redstone.link.controller;

import java.util.BitSet;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.redstone.link.controller.packet.LinkedControllerCopyChannelPacket;
import com.simibubi.create.content.redstone.link.controller.packet.LinkedControllerInputPacket;
import com.simibubi.create.content.redstone.link.controller.packet.LinkedControllerStopLecternPacket;
import com.simibubi.create.content.redstone.link.linkable.LinkableBlockEntity;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LinkedControllerClientHandler {

	//region Fields
	public static final int PACKET_RATE = 10;

	public Mode mode = Mode.IDLE;
	public BitSet currentlyPressed = new BitSet(6);
	private @Nullable BlockPos lecternPos;
	private @Nullable BlockPos selectedLocation;
	private int packetCooldown = PACKET_RATE;
	//endregion

	public void tick() {
		LinkedControllerItemRenderer.tick();

		if (this.mode == Mode.IDLE) return;
		if (this.packetCooldown > 0) this.packetCooldown--;

		final Minecraft mc = Minecraft.getInstance();
		final LocalPlayer player = mc.player;
		assert player != null;

		if (this.shouldReset(player, mc)) {
			this.reset();
			return;
		}

		if (this.inLectern() && AllBlocks.LECTERN_CONTROLLER.get().getBlockEntityOptional(mc.level, this.lecternPos).map(be -> !be.isUsedBy(mc.player)).orElse(true)) {
			this.deactivateInLectern();
			return;
		}

		final List<KeyMapping> controls = ControlsUtil.getControls();
		final BitSet pressedKeys = new BitSet(6);

		for (int i = 0; i < controls.size(); i++) {
			if (ControlsUtil.isActuallyPressed(controls.get(i))) {
				pressedKeys.set(i);
			}
		}

		final boolean changed = pressedKeys.hashCode() != this.currentlyPressed.hashCode();
		this.currentlyPressed = pressedKeys;

		if (this.mode == Mode.BIND && this.selectedLocation != null) {
			assert mc.level != null;
			final VoxelShape shape = mc.level.getBlockState(this.selectedLocation).getShape(mc.level, this.selectedLocation);
			if (!shape.isEmpty()) {
				Outliner.getInstance().showAABB("controller", shape.bounds().move(this.selectedLocation)).colored(0xB73C2D).lineWidth(1 / 16.0f);
			}
			if (changed) {
				final int set = pressedKeys.nextSetBit(0);
				if (set > -1) {
					final BlockEntity be = mc.level.getBlockEntity(this.selectedLocation);
					if (be instanceof LinkableBlockEntity<?>) {
						CatnipServices.NETWORK.sendToServer(new LinkedControllerCopyChannelPacket(set, this.selectedLocation));
						CreateLang.translate("linked_controller.key_bound", controls.get(set).getTranslatedKeyMessage().getString()).sendStatus(mc.player);
					}
					this.mode = Mode.IDLE;
				}
			}
		} else if (this.mode == Mode.ACTIVE && (changed || (this.packetCooldown <= 0 && !pressedKeys.isEmpty()))) {
			CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(pressedKeys, this.lecternPos));
			this.packetCooldown = PACKET_RATE;
			if (changed) {
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.level(), player.blockPosition(), 1.0f, 0.5f, true);
				LinkedControllerItemRenderer.refreshButtons();
			}
		}

		controls.forEach(kb -> kb.setDown(false));
	}

	public void activateInLectern(final BlockPos worldPosition) {
		this.mode = Mode.ACTIVE;
		this.lecternPos = worldPosition;
	}

	public void deactivateInLectern() {
		if (this.mode == Mode.ACTIVE && this.inLectern()) {
			this.reset();
		}
	}

	public boolean inLectern() {
		return this.lecternPos != null;
	}

	protected void reset() {
		ControlsUtil.getControls().forEach(kb -> kb.setDown(ControlsUtil.isActuallyPressed(kb)));
		this.packetCooldown = 0;
		this.selectedLocation = null;
		this.mode = Mode.IDLE;

		if (this.inLectern()) {
			assert this.lecternPos != null;
			CatnipServices.NETWORK.sendToServer(new LinkedControllerStopLecternPacket(this.lecternPos));
		}
		this.lecternPos = null;

		if (!this.currentlyPressed.isEmpty()) {
			CatnipServices.NETWORK.sendToServer(new LinkedControllerInputPacket(new BitSet(6), null));
		}
		this.currentlyPressed.clear();

		LinkedControllerItemRenderer.resetButtons();
	}

	private boolean shouldReset(final LocalPlayer player, final Minecraft mc) {
		return (player.isSpectator() || mc.screen != null || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE) || (!this.inLectern() && !AllItems.LINKED_CONTROLLERS.contains(player.getMainHandItem()) && !AllItems.LINKED_CONTROLLERS.contains(player.getOffhandItem())));
	}

	public void toggle() {
		if (this.mode == Mode.IDLE) {
			this.mode = Mode.ACTIVE;
			this.lecternPos = null;
		} else {
			this.reset();
		}
	}

	public void toggleBindMode(final BlockPos pos) {
		if (this.mode == Mode.IDLE) {
			this.mode = Mode.BIND;
			this.selectedLocation = pos;
		} else {
			this.reset();
		}
	}
}
