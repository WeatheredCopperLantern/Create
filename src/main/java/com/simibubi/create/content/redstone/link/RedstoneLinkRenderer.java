package com.simibubi.create.content.redstone.link;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.link.dummy.LinkBehaviour;
import com.simibubi.create.content.redstone.link.dummy.RedstoneLinkFrequencySlot;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RedstoneLinkRenderer extends SafeBlockEntityRenderer<RedstoneLinkBlockEntity> {

	@Override
	protected void renderSafe(RedstoneLinkBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
		renderItemsOnBlockEntity(be, ms, bufferSource, light, overlay);

		Minecraft mc = Minecraft.getInstance();

		HitResult target = mc.hitResult;
		if ((mc.cameraEntity instanceof LocalPlayer player && player.isSpectator()) || !(target instanceof BlockHitResult result) || result.getBlockPos().equals(be.getBlockPos()))
			return;

		Component freq1 = CreateLang.translateDirect("logistics.firstFrequency");
		Component freq2 = CreateLang.translateDirect("logistics.secondFrequency");

		BlockPos pos = be.getBlockPos();

		for (boolean first : Iterate.trueAndFalse) {
			AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25f);
			Component label = first ? freq1 : freq2;
			boolean hit = RedstoneLinkBlock.testHit(be.getLevel(), be.getBlockState(), pos, first, target.getLocation());
			ValueBoxTransform transform = first ? RedstoneLinkBlock.SLOTS.getLeft() : RedstoneLinkBlock.SLOTS.getRight();

			ValueBox box = new ValueBox(label, bb, pos).passive(!hit);
			boolean empty = be.linkable.channel.get(first).stack.isEmpty();

			if (!empty) box.wideOutline();

			Outliner.getInstance().showOutline(com.mojang.datafixers.util.Pair.of(first, pos), box.transform(transform)).highlightFace(result.getDirection());

			if (!hit) continue;

			List<MutableComponent> tip = new ArrayList<>();
			tip.add(label.copy());
			tip.add(CreateLang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
			CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
		}
	}

	public static void renderItemsOnBlockEntity(SmartBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be != null && !be.isRemoved() && be instanceof RedstoneLinkBlockEntity rlbe) {
			Entity cameraEntity = Minecraft.getInstance().cameraEntity;
			float max = AllConfigs.client().filterItemRenderDistance.getF();
			if (cameraEntity != null && cameraEntity.position().distanceToSqr(VecHelper.getCenterOf(be.getBlockPos())) > (max * max))
				return;

			renderItems(rlbe.channel, ms, be.getBlockPos(), BlockPos.ZERO, be.getLevel(), be.getBlockState(), buffer, light, overlay);
		}
	}

	private static void renderItems(Couple<Frequency> frequencies, PoseStack ms, BlockPos pos, BlockPos renderOffset, Level level, BlockState state, MultiBufferSource buffer, int light, int overlay) {
		for (boolean first : Iterate.trueAndFalse) {
			ValueBoxTransform transform = first ? RedstoneLinkBlock.SLOTS.getLeft() : RedstoneLinkBlock.SLOTS.getRight();
			ItemStack stack = frequencies.get(first).stack;

			ms.pushPose();
			ms.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());
			transform.transform(level, pos, state, ms);

			ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
			ms.popPose();
		}
	}

	public RedstoneLinkRenderer(BlockEntityRendererProvider.Context context) {
	}
}
