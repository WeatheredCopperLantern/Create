package com.simibubi.create.content.redstone.link.controller.lecternController;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItemRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import net.createmod.catnip.math.AngleHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LecternControllerRenderer extends SafeBlockEntityRenderer<LecternControllerBlockEntity> {

	@Override
	protected void renderSafe(final LecternControllerBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
		assert Minecraft.getInstance().player != null;

		final ItemStack stack = AllItems.LINKED_CONTROLLERS.get(be.getColor()).asStack();
		final ItemDisplayContext transformType = ItemDisplayContext.NONE;
		final CustomRenderedItemModel mainModel = (CustomRenderedItemModel) Minecraft.getInstance().getItemRenderer().getModel(stack, be.getLevel(), null, 0);
		final PartialItemModelRenderer renderer = PartialItemModelRenderer.of(stack, transformType, ms, buffer, overlay);
		final boolean active = be.hasUser();
		final boolean renderDepression = be.isUsedBy(Minecraft.getInstance().player);

		final Direction facing = be.getBlockState().getValue(LecternBlock.FACING);
		final var msr = TransformStack.of(ms);

		ms.pushPose();
		msr.translate(0.5, 1.45, 0.5);
		msr.rotateYDegrees(AngleHelper.horizontalAngle(facing) - 90);
		msr.translate(0.28, 0, 0);
		msr.rotateZDegrees(-22.0f);
		LinkedControllerItemRenderer.renderInLectern(stack, mainModel, renderer, transformType, ms, light, active, renderDepression);
		ms.popPose();
	}

	public LecternControllerRenderer(final BlockEntityRendererProvider.Context ignored) {
	}
}
