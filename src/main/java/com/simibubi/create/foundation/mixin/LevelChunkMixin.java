package com.simibubi.create.foundation.mixin;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.persistent.BlockBoundObject;
import com.simibubi.create.foundation.persistent.IBBOBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

	@Shadow
	@Final
	Level level;

	@Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z", ordinal = 0))
	private void create$removeBlockBoundObject(final BlockPos pos, final BlockState newState, final boolean isMoving, final CallbackInfoReturnable<BlockState> cir, @Local final Block newBlock, @Local(ordinal = 1) final BlockState oldState) {
		if (level.isClientSide()) return;
		if (oldState.getBlock() != newBlock && oldState.create$hasBlockBoundObject()) {
			this.create$removeBlockBoundObject(pos);
		}
	}

	@Unique
	public void create$removeBlockBoundObject(BlockPos pos) {
		final BlockBoundObject bbo = Create.PERSISTENT_OBJECTS.getManager(this.level.dimension()).removePersistentBlockBoundObject(pos);
		if (bbo != null) {
			bbo.setRemoved();
		}
	}

	@Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z", ordinal = 1))
	private void create$addNewBlockBoundObject(final BlockPos pos, final BlockState newState, final boolean isMoving, final CallbackInfoReturnable<BlockState> cir, @Local final Block newBlock, @Local(ordinal = 1) final BlockState oldState) {
		if (!(this.level instanceof final ServerLevel serverLevel)) return;
		if (newState.create$hasBlockBoundObject()) {
			BlockBoundObject bbo = Create.PERSISTENT_OBJECTS.getManager(this.level.dimension()).getPersistentBlockBoundObject(pos);
			if (bbo != null && !bbo.isValidBlockState(newState)) {
				this.create$removeBlockBoundObject(pos);
				bbo = null;
			}
			if (bbo == null) {
				bbo = ((IBBO<?>) newBlock).newBlockBoundObject(serverLevel, pos, newState);
				if (bbo != null) {
					Create.PERSISTENT_OBJECTS.getManager(this.level.dimension()).addPersistentObject(bbo);
				}
			} else {
				bbo.setBlockState(newState);
			}
		}
	}
}
