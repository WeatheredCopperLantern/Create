package com.simibubi.create.content.redstone.link.redstoneLink;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;

public class RedstoneLinkFrequencySlot extends ValueBoxTransform.Dual {

	public RedstoneLinkFrequencySlot(final boolean first) {
		super(first);
	}

	@Override
	public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
		final Direction facing = state.getValue(DirectionalBlock.FACING);
		Vec3 location = VecHelper.voxelSpace(8.0f, 3.01f, 5.5f);

		if (facing.getAxis().isHorizontal()) {
			location = VecHelper.voxelSpace(8.0f, 5.5f, 3.01f);
			if (this.isFirst()) location = location.add(0, 5 / 16.0f, 0);
			return this.rotateHorizontally(state, location);
		}

		if (this.isFirst()) location = location.add(0, 0, 5 / 16.0f);
		location = VecHelper.rotateCentered(location, facing == Direction.DOWN ? 180 : 0, Direction.Axis.X);
		return location;
	}

	@Override
	public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
		final Direction facing = state.getValue(DirectionalBlock.FACING);
		final float yRot = facing.getAxis().isVertical() ? 0 : AngleHelper.horizontalAngle(facing) + 180;
		final float xRot = facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0;
		TransformStack.of(ms).rotateYDegrees(yRot).rotateXDegrees(xRot);
	}

	@Override
	public float getScale() {
		return 0.4975f;
	}
}