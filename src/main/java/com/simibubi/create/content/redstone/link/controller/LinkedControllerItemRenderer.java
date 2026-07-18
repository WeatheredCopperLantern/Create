package com.simibubi.create.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import javax.annotation.ParametersAreNonnullByDefault;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LinkedControllerItemRenderer extends CustomRenderedItemModelRenderer {

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

	private static boolean isHandContext(final ItemDisplayContext transformType) {
		return transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	protected static void renderNormal(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, final int light) {
		final Player player = Minecraft.getInstance().player;
		assert player != null;
		final boolean isHandContext = isHandContext(transformType);
		final boolean controllerInMain = isHandContext && AllItems.LINKED_CONTROLLERS.contains(player.getMainHandItem());
		final boolean controllerInOffhand = !controllerInMain && isHandContext && AllItems.LINKED_CONTROLLERS.contains(player.getOffhandItem());
		final boolean potentiallyActiveController = (controllerInMain && stack == player.getMainHandItem()) || (controllerInOffhand && stack == player.getOffhandItem());

		boolean animatePosition = potentiallyActiveController && !CreateClient.LINKED_CONTROLLER_HANDLER.inLectern();
		boolean active = animatePosition && CreateClient.LINKED_CONTROLLER_HANDLER.mode != Mode.IDLE;

		LinkedControllerItemRenderer.render((LinkedControllerItem) stack.getItem(), model, renderer, transformType, ms, light, RenderType.NORMAL, active, true, animatePosition);
	}

	public static void renderInLectern(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, final int light, final boolean active, final boolean renderDepression) {
		LinkedControllerItemRenderer.render((LinkedControllerItem) stack.getItem(), model, renderer, transformType, ms, light, RenderType.LECTERN, active, renderDepression, false);
	}

	protected static void render(final LinkedControllerItem item, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, int light, final RenderType renderType, final boolean active, final boolean renderDepression, final boolean animatePosition) {
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
		renderer.render(active ? AllPartialModels.DYED_LINKED_CONTROLLERS_POWERED.get(item.color).get() : model.getOriginalModel(), light);

		if (!active) {
			ms.popPose();
			return;
		}

		final float pixelSize = 1 / 16.0f;
		final float depressionDepth = renderDepression ? -0.05f : 0;
		int index = 0;

		if (renderType == RenderType.NORMAL && CreateClient.LINKED_CONTROLLER_HANDLER.mode == Mode.BIND) {
			final int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4.0f) + 1) / 2, 5, 15);
			light = i << 20;
		}

		ms.pushPose();
		msr.translate(2 * pixelSize, 0, 8 * pixelSize);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, depressionDepth * LinkedControllerItemRenderer.buttons[index].getValue(pt));
		index++;
		msr.translate(4 * pixelSize, 0, 0);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, depressionDepth * LinkedControllerItemRenderer.buttons[index].getValue(pt));
		index++;
		msr.translate(-2 * pixelSize, 0, 2 * pixelSize);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, depressionDepth * LinkedControllerItemRenderer.buttons[index].getValue(pt));
		index++;
		msr.translate(0, 0, -4 * pixelSize);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, depressionDepth * LinkedControllerItemRenderer.buttons[index].getValue(pt));
		index++;
		ms.popPose();

		msr.translate(3 * pixelSize, 0, 3 * pixelSize);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, depressionDepth * LinkedControllerItemRenderer.buttons[index].getValue(pt));
		index++;
		msr.translate(2 * pixelSize, 0, 0);
		LinkedControllerItemRenderer.renderButton(renderer, ms, light, depressionDepth * LinkedControllerItemRenderer.buttons[index].getValue(pt));

		ms.popPose();
	}

	protected static void renderButton(final PartialItemModelRenderer renderer, final PoseStack ms, final int light, final float depressionDepth) {
		ms.pushPose();
		ms.translate(0, depressionDepth, 0);
		renderer.renderSolid(AllPartialModels.LINKABLE_CONTROLLER_BUTTON.get(), light);
		ms.popPose();
	}

	protected enum RenderType {
		NORMAL, LECTERN
	}
}
