package com.simibubi.create.content.redstone.link;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.events.ClientEvents;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;

public class RedstoneLinkRenderer extends SafeBlockEntityRenderer<RedstoneLinkBlockEntity> {

	private static final Couple<Component> freqTexts;
	private static final AABB aabb;

	static {
		freqTexts = Couple.create(CreateLang.translateDirect("logistics.firstFrequency"), CreateLang.translateDirect("logistics.secondFrequency"));
		aabb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.25f);
	}

	@Override
	protected void renderSafe(final RedstoneLinkBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource bufferSource, final int light, final int overlay) {
		final Minecraft mc = Minecraft.getInstance();
		final Entity cameraEntity = mc.cameraEntity;

		if (cameraEntity == null) return;

		//Increase distance on lower fov/usage of zoom mod
		final double max = AllConfigs.client().filterItemRenderDistance.getF() * Math.max(1, Math.tan(Math.toRadians(70.0 * 0.5)) / Math.tan(Math.toRadians(ClientEvents.FOV() * 0.5)));

		if (cameraEntity.position().distanceToSqr(VecHelper.getCenterOf(be.getBlockPos())) <= max * max) {
			RedstoneLinkRenderer.renderItems(be.channel, ms, be.getBlockPos(), BlockPos.ZERO, be.getLevel(), be.getBlockState(), bufferSource, light, overlay);
		}

		final HitResult target = mc.hitResult;
		final BlockPos pos = be.getBlockPos();

		if (cameraEntity.isSpectator() || !(target instanceof final BlockHitResult result) || !pos.equals(result.getBlockPos())) {
			return;
		}

		RedstoneLinkRenderer.renderFrequencySelectionOnBlockEntity(be, pos, result);
	}

	public static void renderFrequencySelectionOnBlockEntity(final RedstoneLinkBlockEntity be, final BlockPos pos, final BlockHitResult target) {
		for (final boolean first : Iterate.trueAndFalse) {
			final boolean hit = RedstoneLinkBlock.testHit(be.getLevel(), be.getBlockState(), pos, first, target.getLocation());
			final ValueBoxTransform transform = first ? RedstoneLinkBlock.SLOTS.getLeft() : RedstoneLinkBlock.SLOTS.getRight();

			final ValueBox box = new ValueBox(Component.empty(), RedstoneLinkRenderer.aabb, pos).passive(!hit);
			final boolean empty = be.channel.get(first).stack.isEmpty();

			if (!empty) box.wideOutline();

			Outliner.getInstance().showOutline(Pair.of(first, pos), box.transform(transform)).highlightFace(target.getDirection());

			if (hit) {
				final List<MutableComponent> tip = new ArrayList<>(2);
				tip.add(RedstoneLinkRenderer.freqTexts.get(first).copy());
				tip.add(CreateLang.translateDirect(empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
				CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
				return;
			}
		}
	}

	private static void renderItems(final Couple<Frequency> frequencies, final PoseStack ms, final BlockPos pos, final BlockPos renderOffset, final Level level, final BlockState state, final MultiBufferSource buffer, final int light, final int overlay) {
		for (final boolean first : Iterate.trueAndFalse) {
			final ValueBoxTransform transform = first ? RedstoneLinkBlock.SLOTS.getLeft() : RedstoneLinkBlock.SLOTS.getRight();
			final ItemStack stack = frequencies.get(first).stack;

			ms.pushPose();
			ms.translate(renderOffset.getX(), renderOffset.getY(), renderOffset.getZ());

			if (Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).isGui3d()) {
				final Vec3i tmp = state.getValue(DirectionalBlock.FACING).getNormal();
				final Vector3f normal = new Vector3f(tmp.getX(), tmp.getY(), tmp.getZ());
				normal.mul(1 / 64.0f);
				ms.translate(normal.x, normal.y, normal.z);
			}

			transform.transform(level, pos, state, ms);

			ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
			ms.popPose();
		}
	}

	public RedstoneLinkRenderer(final BlockEntityRendererProvider.Context context) {
	}
}
