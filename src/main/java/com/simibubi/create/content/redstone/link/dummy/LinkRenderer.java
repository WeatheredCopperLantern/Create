package com.simibubi.create.content.redstone.link.dummy;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class LinkRenderer extends SafeBlockEntityRenderer<RedstoneLinkBlockEntity> {

	private static final Pair<ValueBoxTransform, ValueBoxTransform> SLOTS = ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);

	public LinkRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(RedstoneLinkBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
		BlockState blockState = be.getBlockState();
		//if (!AllBlocks.REDSTONE_LINK.has(blockState)) return;

		renderItemsOnBlockEntity(be, ms, bufferSource, light, overlay);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource bufferSource) {
		int light = LevelRenderer.getLightColor(context.world, BlockPos.containing(context.position.x(), context.position.y(), context.position.z()));
		BlockState blockState = context.state;
		//if (!AllBlocks.REDSTONE_LINK.has(blockState)) return;
		if (context.temporaryData instanceof Couple<?> frequencies) {
			renderItemsInContraption(context.localPos, context.position, matrices.getModelViewProjection(), bufferSource, light, OverlayTexture.NO_OVERLAY, (Couple<RedstoneLinkNetworkHandler.Frequency>) frequencies, context.world, blockState);
		}
	}

	private static void renderItems(Couple<RedstoneLinkNetworkHandler.Frequency> frequencies, PoseStack ms, BlockPos pos, BlockPos renderOffset, Level level, BlockState state, MultiBufferSource buffer, int light, int overlay) {
		for (boolean first : Iterate.trueAndFalse) {
			ValueBoxTransform transform = first ? SLOTS.getLeft() : SLOTS.getRight();
			ItemStack stack = frequencies.get(first).getStack();

			ms.pushPose();
			ms.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());
			transform.transform(level, pos, state, ms);

			ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
			ms.popPose();
		}
	}

	public static void renderItemsOnBlockEntity(SmartBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be == null || be.isRemoved()) return;

		LinkBehaviour behaviour = be.getBehaviour(LinkBehaviour.TYPE);
		if (behaviour == null) return;

		Entity cameraEntity = Minecraft.getInstance().cameraEntity;
		float max = AllConfigs.client().filterItemRenderDistance.getF();
		if (cameraEntity != null && cameraEntity.position().distanceToSqr(VecHelper.getCenterOf(be.getBlockPos())) > (max * max))
			return;

		renderItems(behaviour.link.getChannelKey(), ms, be.getBlockPos(), BlockPos.ZERO, be.getLevel(), be.getBlockState(), buffer, light, overlay);
	}

	public static void renderItemsInContraption(BlockPos pos, Vec3 globalPos, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Couple<RedstoneLinkNetworkHandler.Frequency> frequencies, Level level, BlockState state) {
		Entity cameraEntity = Minecraft.getInstance().cameraEntity;
		float max = AllConfigs.client().filterItemRenderDistance.getF();
		if (cameraEntity != null && cameraEntity.position().distanceToSqr(globalPos) > (max * max)) return;

		renderItems(frequencies, ms, pos, pos, level, state, buffer, light, overlay);
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();

		HitResult target = mc.hitResult;
		if ((mc.cameraEntity instanceof LocalPlayer player && player.isSpectator()) || target == null || !(target instanceof BlockHitResult result))
			return;

		ClientLevel world = mc.level;
		BlockPos pos = result.getBlockPos();

		LinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, LinkBehaviour.TYPE);
		if (behaviour == null)
			return;

		Component freq1 = CreateLang.translateDirect("logistics.firstFrequency");
		Component freq2 = CreateLang.translateDirect("logistics.secondFrequency");

		for (boolean first : Iterate.trueAndFalse) {
			AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25f);
			Component label = first ? freq1 : freq2;
			boolean hit = behaviour.testHit(first, target.getLocation());
			ValueBoxTransform transform = first ? behaviour.firstSlot : behaviour.secondSlot;

			ValueBox box = new ValueBox(label, bb, pos).passive(!hit);
			boolean empty = behaviour.link.getChannelKey()
				.get(first)
				.getStack()
				.isEmpty();

			if (!empty)
				box.wideOutline();

			Outliner.getInstance().showOutline(com.mojang.datafixers.util.Pair.of(Boolean.valueOf(first), pos), box.transform(transform))
				.highlightFace(result.getDirection());

			if (!hit)
				continue;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(
				CreateLang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}
}
