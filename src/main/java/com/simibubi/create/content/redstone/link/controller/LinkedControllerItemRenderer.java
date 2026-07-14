package com.simibubi.create.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LinkedControllerItemRenderer extends CustomRenderedItemModelRenderer {

	protected static final PartialModel POWERED = PartialModel.of(Create.asResource("item/linked_controller/powered"));
	protected static final PartialModel BUTTON = PartialModel.of(Create.asResource("item/linked_controller/button"));

	private static LerpedFloat equipProgress;
	private final static LerpedFloat[] buttons = new LerpedFloat[6];

	static {
		LinkedControllerItemRenderer.equipProgress = LerpedFloat.linear().startWithValue(0);
		for (int i = 0; i < 6; i++) {
			LinkedControllerItemRenderer.buttons[i] = LerpedFloat.linear().startWithValue(0);
		}
	}

	static void tick() {
		if (Minecraft.getInstance().isPaused()) return;

		final boolean active = CreateClient.LINKED_CONTROLLER_HANDLER.mode != Mode.IDLE;
		LinkedControllerItemRenderer.equipProgress.chase(active ? 1 : 0, 0.2f, LerpedFloat.Chaser.EXP);
		LinkedControllerItemRenderer.equipProgress.tickChaser();

		if (!active) return;

		for (int i = 0; i < LinkedControllerItemRenderer.buttons.length; i++) {
			LinkedControllerItemRenderer.buttons[i].tickChaser();
		}
	}

	public static void refreshButtons() {
		for (int i = 0; i < LinkedControllerItemRenderer.buttons.length; i++) {
			LinkedControllerItemRenderer.buttons[i].chase(CreateClient.LINKED_CONTROLLER_HANDLER.currentlyPressed.get(i) ? 1 : 0, 0.4f, LerpedFloat.Chaser.EXP);
		}
	}

	static void resetButtons() {
		for (final LerpedFloat button : LinkedControllerItemRenderer.buttons) {
			button.startWithValue(0);
		}
	}

	@Override
	protected void render(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
		LinkedControllerItemRenderer.renderNormal(stack, model, renderer, transformType, ms, light);
	}

	protected static void renderNormal(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, final int light) {
		boolean active = CreateClient.LINKED_CONTROLLER_HANDLER.mode != Mode.IDLE && !CreateClient.LINKED_CONTROLLER_HANDLER.inLectern();
		boolean animatePosition = transformType != ItemDisplayContext.GUI && !CreateClient.LINKED_CONTROLLER_HANDLER.inLectern();
		if (active) {
			final Player player = Minecraft.getInstance().player;
			assert player != null;
			final boolean controllerInMain = AllItems.BROWN_LINKED_CONTROLLER.isIn(player.getMainHandItem());

			if (stack == player.getOffhandItem() && controllerInMain) {
				active = false;
				animatePosition = false;
			}
		} else if (animatePosition) {
			final Player player = Minecraft.getInstance().player;
			assert player != null;
			final boolean controllerInMain = AllItems.BROWN_LINKED_CONTROLLER.isIn(player.getMainHandItem());

			if (stack == player.getOffhandItem() && controllerInMain) {
				animatePosition = false;
			}
		}

		LinkedControllerItemRenderer.render(model, renderer, transformType, ms, light, RenderType.NORMAL, active, true, animatePosition);
	}

	public static void renderInLectern(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, final int light, final boolean active, final boolean renderDepression) {
		LinkedControllerItemRenderer.render(model, renderer, transformType, ms, light, RenderType.LECTERN, active, renderDepression, false);
	}

	protected static void render(final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, int light, final RenderType renderType, final boolean active, final boolean renderDepression, final boolean animatePosition) {
		final float pt = AnimationTickHolder.getPartialTicks();
		final var msr = TransformStack.of(ms);

		ms.pushPose();

		if (animatePosition) {
			final float equip = LinkedControllerItemRenderer.equipProgress.getValue(pt);
			final int handModifier = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
			msr.translate(0, equip / 4, equip / 4 * handModifier);
			msr.rotateYDegrees(equip * -30 * handModifier);
			msr.rotateZDegrees(equip * -30);
		}

		renderer.render(active ? LinkedControllerItemRenderer.POWERED.get() : model.getOriginalModel(), light);

		if (!active) {
			ms.popPose();
			return;
		}

		final BakedModel button = LinkedControllerItemRenderer.BUTTON.get();
		final float s = 1 / 16.0f;
		final float b = s * -0.75f;
		int index = 0;

		if (renderType == RenderType.NORMAL && CreateClient.LINKED_CONTROLLER_HANDLER.mode == Mode.BIND) {
			final int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4.0f) + 1) / 2, 5, 15);
			light = i << 20;
		}

		ms.pushPose();
		msr.translate(2 * s, 0, 8 * s);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, pt, button, b, index, renderDepression);
		index++;
		msr.translate(4 * s, 0, 0);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, pt, button, b, index, renderDepression);
		index++;
		msr.translate(-2 * s, 0, 2 * s);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, pt, button, b, index, renderDepression);
		index++;
		msr.translate(0, 0, -4 * s);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, pt, button, b, index, renderDepression);
		index++;
		ms.popPose();

		msr.translate(3 * s, 0, 3 * s);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, pt, button, b, index, renderDepression);
		index++;
		msr.translate(2 * s, 0, 0);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, pt, button, b, index, renderDepression);

		ms.popPose();
	}

	protected static void renderButton(final PartialItemModelRenderer renderer, final PoseStack ms, final int light, final float pt, final BakedModel button, final float b, final int index, final boolean renderDepression) {
		ms.pushPose();
		if (renderDepression) {
			final float depression = b * LinkedControllerItemRenderer.buttons[index].getValue(pt);
			ms.translate(0, depression, 0);
		}
		renderer.renderSolid(button, light);
		ms.popPose();
	}

	protected enum RenderType {
		NORMAL, LECTERN
	}
}
