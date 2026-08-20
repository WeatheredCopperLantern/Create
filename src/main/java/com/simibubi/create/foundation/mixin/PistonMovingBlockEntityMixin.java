package com.simibubi.create.foundation.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.extension.interfaces.ILevelInterface;
import com.simibubi.create.foundation.extension.interfaces.PistonMovingBlockEntityAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Based on https://github.com/gnembon/fabric-carpet/blob/22a71faa30c0e91916e1a03bc543ee688ca4b199/src/main/java/carpet/mixins/PistonMovingBlockEntity_movableBEMixin.java
@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin extends BlockEntity implements PistonMovingBlockEntityAccessor {

	@Shadow
	private BlockState movedState;
	@Unique
	private SmartBlockEntity create$be;

	@Override
	public void create$setBlockEntity(final SmartBlockEntity blockEntity) {
		this.create$be = blockEntity;
	}

	@Override
	public SmartBlockEntity create$getBlockEntity() {
		return this.create$be;
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private static boolean create$finalizeMovement(final Level instance, final BlockPos blockPos, final BlockState blockState, final int i, final Operation<Boolean> original, @Local(argsOnly = true) final PistonMovingBlockEntity blockEntity) {
		final SmartBlockEntity movedBE = ((PistonMovingBlockEntityAccessor) blockEntity).create$getBlockEntity();
		if (movedBE != null) {
			movedBE.updateWorldPosition(blockPos);
			final boolean result = ((ILevelInterface) instance).create$setBlockWithBlockEntity(blockPos, blockState, i, movedBE);
			movedBE.setBeingPushedByPiston(false);
			return result;
		}
		return original.call(instance, blockPos, blockState, i);
	}

	@WrapOperation(method = "finalTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean create$finalizeMovement2(final Level level, final BlockPos blockPos, final BlockState blockState, final int i, final Operation<Boolean> original) {
		final SmartBlockEntity movedBE = this.create$getBlockEntity();
		if (movedBE != null) {
			movedBE.updateWorldPosition(blockPos);
			final boolean result = ((ILevelInterface) level).create$setBlockWithBlockEntity(blockPos, blockState, i, movedBE);
			movedBE.setBeingPushedByPiston(false);
			return result;
		}
		return original.call(level, blockPos, blockState, i);
	}

	@Inject(method = "saveAdditional", at = @At(value = "RETURN"))
	private void create$saveMovingBE(final CompoundTag tag, final HolderLookup.Provider registries, final CallbackInfo ci) {
		final SmartBlockEntity movedBE = this.create$getBlockEntity();
		if (movedBE != null) {
			tag.put("create$movingBE", movedBE.saveWithoutMetadata(registries));
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void create$loadMovingBE(final CompoundTag tag, final HolderLookup.Provider registries, final CallbackInfo ci) {
		final CompoundTag beCompound = tag.getCompound("create$movingBE");
		if (beCompound.isEmpty() || !(this.movedState.getBlock() instanceof final EntityBlock entityBlock)) return;
		final BlockEntity be = entityBlock.newBlockEntity(this.getBlockPos(), this.movedState);
		if (be instanceof SmartBlockEntity smartBE) {
			smartBE.loadWithComponents(beCompound, registries);
			this.create$setBlockEntity(smartBE);
		}
	}

	public PistonMovingBlockEntityMixin(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState) {
		super(type, pos, blockState);
	}
}
