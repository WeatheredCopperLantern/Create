package com.simibubi.create.foundation.mixin.compat.jade;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.link.controller.lecternController.LecternControllerBlockEntity;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.addon.vanilla.LecternProvider;
import snownee.jade.api.BlockAccessor;

@Mixin(LecternProvider.class)
public class JadeLecternProviderMixin {

	@Inject(method = "streamData(Lsnownee/jade/api/BlockAccessor;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private static void streamDataInject(final BlockAccessor accessor, final CallbackInfoReturnable<ItemStack> cir) {
		if (accessor.getBlockEntity() instanceof final LecternControllerBlockEntity lecternControllerBlockEntity) {
			cir.setReturnValue(AllItems.LINKED_CONTROLLERS.get(lecternControllerBlockEntity.getColor()).asStack());
		}
	}
}
