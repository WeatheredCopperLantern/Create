package com.simibubi.create.foundation.mixin;

import java.util.HashMap;
import java.util.List;

import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.extension.interfaces.PistonMovingBlockEntityAccessor;

import net.createmod.catnip.data.TriState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PistonBaseBlock.class)
public class PistonBaseBlockMixin {

	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private static void create$isPushablePatches(final BlockState state, final Level level, final BlockPos pos, final Direction movementDirection, final boolean allowDestroy, final Direction pistonFacing, final CallbackInfoReturnable<Boolean> cir) {
		final Block block = state.getBlock();
		final TriState isBEPushable = (block instanceof final IBE<?> ibe) ? ibe.isBEPushable() ? TriState.TRUE : TriState.FALSE : TriState.DEFAULT;
		final TriState isBBOPushable = (block instanceof final IBBO<?> ibbo) ? ibbo.isBBOPushable() ? TriState.TRUE : TriState.FALSE : TriState.DEFAULT;

		// Block is neither a IBE nor a IBBO so we do nothing
		if (isBEPushable == TriState.DEFAULT && isBBOPushable == TriState.DEFAULT) return;
		// If either don't want to be moved make non-movable
		if (isBEPushable == TriState.FALSE || isBBOPushable == TriState.FALSE) {
			cir.setReturnValue(false);
		} else {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "moveBlocks", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 2), order = Integer.MIN_VALUE)
	private void create$onMove(final Level level, final BlockPos pos, final Direction facing, final boolean extending, final CallbackInfoReturnable<Boolean> cir, @Local(name = "list") final List<BlockPos> list, @Share("blockEntities") LocalRef<HashMap<BlockPos, SmartBlockEntity>> blockEntities) {
		for (final BlockPos blockpos : list) {
			if (level.getBlockEntity(blockpos) instanceof final SmartBlockEntity smartBE) {
				HashMap<BlockPos, SmartBlockEntity> beCache = blockEntities.get();
				if (beCache == null) blockEntities.set(HashMap.newHashMap(PistonStructureResolver.MAX_PUSH_DEPTH));
				blockEntities.get().put(blockpos.relative(extending ? facing : facing.getOpposite()), smartBE);
				smartBE.setBeingPushedByPiston(true);
				level.removeBlockEntity(blockpos);
			}
		}
	}

	//Based on https://github.com/gnembon/fabric-carpet/blob/22a71faa30c0e91916e1a03bc543ee688ca4b199/src/main/java/carpet/mixins/PistonBaseBlock_movableBEMixin.java#L113-L122
	@WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)Lnet/minecraft/world/level/block/entity/BlockEntity;", ordinal = 0))
	private BlockEntity create$storeBEintoPistonMovingBE(final BlockPos blockPos, final BlockState blockState, final BlockState blockState2, final Direction direction, final boolean bl, final boolean bl2, final Operation<BlockEntity> original, @Local(argsOnly = true) final Level level, @Share("blockEntities") LocalRef<HashMap<BlockPos, SmartBlockEntity>> blockEntities) {
		final BlockEntity originalBE = original.call(blockPos, blockState, blockState2, direction, bl, bl2);
		if (blockEntities.get() == null) return originalBE;

		if (originalBE instanceof final PistonMovingBlockEntity pistonMovingBlockEntity && blockEntities.get().get(blockPos) instanceof final SmartBlockEntity smartBE) {
			((PistonMovingBlockEntityAccessor) pistonMovingBlockEntity).create$setBlockEntity(smartBE);
		}
		return originalBE;
	}
}
