package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.mixin.accessor.UseOnContextAccessor;
import com.simibubi.create.foundation.persistent.BlockBoundObject;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void create$fixDeployerPlacement(BlockPlaceContext pContext, CallbackInfoReturnable<InteractionResult> cir) {
		BlockState state = pContext.getLevel().getBlockState(((UseOnContextAccessor) pContext).create$getHitResult().getBlockPos());
		if (!state.canBeReplaced() && pContext.getPlayer() instanceof DeployerFakePlayer) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "updateBlockEntityComponents", at = @At("HEAD"))
	private static void create$updateBlockBoundObject(final Level level, final BlockPos blockPos, final ItemStack itemStack, final CallbackInfo ci){
		final BlockBoundObject bbo = level.create$getBlockBoundObject(blockPos);
		if (bbo != null) {
			bbo.applyComponents(itemStack);
		}
	}

}
