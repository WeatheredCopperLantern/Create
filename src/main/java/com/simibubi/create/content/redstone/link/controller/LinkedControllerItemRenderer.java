package com.simibubi.create.content.redstone.link.controller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LinkedControllerItemRenderer  extends CustomRenderedItemModelRenderer {

	protected static final PartialModel POWERED = PartialModel.of(Create.asResource("item/linked_controller/powered"));
	protected static final PartialModel BUTTON = PartialModel.of(Create.asResource("item/linked_controller/button"));

	static LerpedFloat equipProgress;
	static List<LerpedFloat> buttons;

	static {
		LinkedControllerItemRenderer.equipProgress = LerpedFloat.linear().startWithValue(0);
		LinkedControllerItemRenderer.buttons = new ArrayList<>(6);
		for (int i = 0; i < 6; i++) {
			LinkedControllerItemRenderer.buttons.add(LerpedFloat.linear().startWithValue(0));
		}
	}

	static void tick() {
		if (Minecraft.getInstance().isPaused()) return;

		final boolean active = LinkedControllerClientHandler.MODE != Mode.IDLE;
		LinkedControllerItemRenderer.equipProgress.chase(active ? 1 : 0, 0.2f, LerpedFloat.Chaser.EXP);
		LinkedControllerItemRenderer.equipProgress.tickChaser();

		if (!active) return;

		for (int i = 0; i < LinkedControllerItemRenderer.buttons.size(); i++) {
			final LerpedFloat lerpedFloat = LinkedControllerItemRenderer.buttons.get(i);
			lerpedFloat.chase(LinkedControllerClientHandler.currentlyPressed.get(i) ? 1 : 0, 0.4f, LerpedFloat.Chaser.EXP);
			lerpedFloat.tickChaser();
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
		LinkedControllerItemRenderer.render(stack, model, renderer, transformType, ms, light, LinkedControllerItemRenderer.RenderType.NORMAL, false, false);
	}

	public static void renderInLectern(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, final int light, final boolean active, final boolean renderDepression) {
		LinkedControllerItemRenderer.render(stack, model, renderer, transformType, ms, light, LinkedControllerItemRenderer.RenderType.LECTERN, active, renderDepression);
	}

	protected static void render(final ItemStack stack, final CustomRenderedItemModel model, final PartialItemModelRenderer renderer, final ItemDisplayContext transformType, final PoseStack ms, int light, final LinkedControllerItemRenderer.RenderType renderType, boolean active, boolean renderDepression) {
		final float pt = AnimationTickHolder.getPartialTicks();
		final var msr = TransformStack.of(ms);

		ms.pushPose();

		if (renderType == LinkedControllerItemRenderer.RenderType.NORMAL) {
			final Minecraft mc = Minecraft.getInstance();
			final boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
			final ItemDisplayContext mainHand = rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			final ItemDisplayContext offHand = rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

			active = false;
			final boolean noControllerInMain = !AllItems.LINKED_CONTROLLER.isIn(mc.player.getMainHandItem());

			if (transformType == mainHand || (transformType == offHand && noControllerInMain)) {
				final float equip = LinkedControllerItemRenderer.equipProgress.getValue(pt);
				final int handModifier = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
				msr.translate(0, equip / 4, equip / 4 * handModifier);
				msr.rotateYDegrees(equip * -30 * handModifier);
				msr.rotateZDegrees(equip * -30);
				active = true;
			}

			if (transformType == ItemDisplayContext.GUI) {
				if (stack == mc.player.getMainHandItem()) active = true;
				if (stack == mc.player.getOffhandItem() && noControllerInMain) active = true;
			}

			active &= LinkedControllerClientHandler.MODE != Mode.IDLE;

			renderDepression = true;
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

		if (renderType == LinkedControllerItemRenderer.RenderType.NORMAL) {
			if (LinkedControllerClientHandler.MODE == Mode.BIND) {
				final int i = (int) Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4.0f) + 1) / 2, 5, 15);
				light = i << 20;
			}
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
		index++;

		ms.popPose();
	}

	protected static void renderButton(final PartialItemModelRenderer renderer, final PoseStack ms, final int light, final float pt, final BakedModel button, final float b, final int index, final boolean renderDepression) {
		ms.pushPose();
		if (renderDepression) {
			final float depression = b * LinkedControllerItemRenderer.buttons.get(index).getValue(pt);
			ms.translate(0, depression, 0);
		}
		renderer.renderSolid(button, light);
		ms.popPose();
	}

	protected enum RenderType {
		NORMAL, LECTERN
	}
}
