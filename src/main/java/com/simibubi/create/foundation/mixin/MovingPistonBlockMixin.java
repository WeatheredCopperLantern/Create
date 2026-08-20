package com.simibubi.create.foundation.mixin;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.extension.interfaces.PistonMovingBlockEntityAccessor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MovingPistonBlock.class)
public class MovingPistonBlockMixin {

	@WrapOperation(method = "getDrops", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"))
	private List<ItemStack> create$dropBEItems(final BlockState instance, final LootParams.Builder builder, final Operation<List<ItemStack>> original, @Local final PistonMovingBlockEntity pistonmovingblockentity) {
		if (((PistonMovingBlockEntityAccessor) pistonmovingblockentity).create$getBlockEntity() instanceof final SmartBlockEntity smBE) {
			smBE.destroy();
		}
		return original.call(instance, builder);
	}
}
