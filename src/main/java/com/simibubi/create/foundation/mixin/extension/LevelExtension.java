package com.simibubi.create.foundation.mixin.extension;

import com.simibubi.create.foundation.extension.interfaces.ILevelChunkInterface;
import com.simibubi.create.foundation.extension.interfaces.ILevelInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Level.class)
public abstract class LevelExtension implements ILevelInterface, LevelAccessor {

	@Shadow public abstract LevelChunk getChunkAt(final BlockPos blockPos);

	@Override
	public boolean create$setBlockWithBlockEntity(final BlockPos blockPos, final BlockState blockState, final int i, final BlockEntity movedBE) {
		final BlockPos blockpos = movedBE.getBlockPos();
		if (!this.isOutsideBuildHeight(blockpos)) {
			((ILevelChunkInterface)this.getChunkAt(blockpos)).create$forceAddAndRegisterBlockEntity(movedBE);
		}
		return this.setBlock(blockPos, blockState, i);
	}
}
