package com.simibubi.create.foundation.mixin;

import com.simibubi.create.foundation.persistent.BlockBoundObject;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

	@Shadow
	public ServerPlayer player;

	@Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;saveToItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/HolderLookup$Provider;)V"))
	private void create$blockBoundObjectSaveToItem(final ServerboundSetCreativeModeSlotPacket packet, final CallbackInfo ci, final @Local BlockPos blockPos, final @Local ItemStack itemStack) {
		final BlockBoundObject bbo = this.player.level().create$getBlockBoundObject(blockPos);
		if (bbo != null) {
			bbo.saveToItem(itemStack);
		}
	}
}
