package com.simibubi.create.content.redstone.link.dummy.controller;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.platform.CatnipServices;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LecternControllerBlockEntity extends SmartBlockEntity {

	private ItemContainerContents controllerData = ItemContainerContents.EMPTY;
	private UUID user;
	private UUID prevUser;    // used only on client
	private boolean deactivatedThisTick;    // used only on server

	public LecternControllerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, this.controllerData).orElseThrow());
		if (this.user != null) compound.putUUID("User", this.user);
	}

	@Override
	public void writeSafe(final CompoundTag compound, final HolderLookup.Provider registries) {
		super.writeSafe(compound, registries);
		compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, this.controllerData).orElseThrow());
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(compound, registries, clientPacket);

		this.controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, registries, compound.get("ControllerData")).orElse(ItemContainerContents.EMPTY);
		this.user = compound.hasUUID("User") ? compound.getUUID("User") : null;
	}

	public ItemStack getController() {
		return this.createLinkedController();
	}

	public boolean hasUser() {
		return this.user != null;
	}

	public boolean isUsedBy(final Player player) {
		return this.hasUser() && this.user.equals(player.getUUID());
	}

	public void tryStartUsing(final Player player) {
		if (!this.deactivatedThisTick && !this.hasUser() && !LecternControllerBlockEntity.playerIsUsingLectern(player) && LecternControllerBlockEntity.playerInRange(player, this.level, this.worldPosition)) {
			this.startUsing(player);
		}
	}

	public void tryStopUsing(final Player player) {
		if (this.isUsedBy(player)) this.stopUsing(player);
	}

	private void startUsing(final Player player) {
		this.user = player.getUUID();
		player.getPersistentData().putBoolean("IsUsingLecternController", true);
		this.sendData();
	}

	private void stopUsing(final Player player) {
		this.user = null;
		if (player != null) player.getPersistentData().remove("IsUsingLecternController");
		this.deactivatedThisTick = true;
		this.sendData();
	}

	public static boolean playerIsUsingLectern(final Player player) {
		return player.getPersistentData().contains("IsUsingLecternController");
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level.isClientSide) {
			CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tryToggleActive);
			this.prevUser = this.user;
		}

		if (!this.level.isClientSide) {
			this.deactivatedThisTick = false;

			if (!(this.level instanceof ServerLevel)) return;
			if (this.user == null) return;

			final Entity entity = ((ServerLevel) this.level).getEntity(this.user);
			if (!(entity instanceof final Player player)) {
				this.stopUsing(null);
				return;
			}

			if (!LecternControllerBlockEntity.playerInRange(player, this.level, this.worldPosition) || !LecternControllerBlockEntity.playerIsUsingLectern(player)) {
				this.stopUsing(player);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void tryToggleActive() {
		if (this.user == null && Minecraft.getInstance().player.getUUID().equals(this.prevUser)) {
			LinkedControllerClientHandler.deactivateInLectern();
		} else if (this.prevUser == null && Minecraft.getInstance().player.getUUID().equals(this.user)) {
			LinkedControllerClientHandler.activateInLectern(this.worldPosition);
		}
	}

	public void setController(final ItemStack newController) {
		if (newController != null) {
			this.controllerData = newController.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
			AllSoundEvents.CONTROLLER_PUT.playOnServer(this.level, this.worldPosition);
		}
	}

	public void swapControllers(final ItemStack stack, final Player player, final InteractionHand hand, final BlockState state) {
		final ItemStack newController = stack.copy();
		stack.setCount(0);
		if (player.getItemInHand(hand).isEmpty()) {
			player.setItemInHand(hand, this.createLinkedController());
		} else {
			this.dropController(state);
		}
		this.setController(newController);
	}

	public void dropController(final BlockState state) {
		final Entity entity = ((ServerLevel) this.level).getEntity(this.user);
		if (entity instanceof final Player player) this.stopUsing(player);

		final Direction dir = state.getValue(LecternBlock.FACING);
		final double x = this.worldPosition.getX() + 0.5 + 0.25 * dir.getStepX();
		final double y = this.worldPosition.getY() + 1;
		final double z = this.worldPosition.getZ() + 0.5 + 0.25 * dir.getStepZ();
		final ItemEntity itementity = new ItemEntity(this.level, x, y, z, this.createLinkedController());
		itementity.setDefaultPickUpDelay();
		this.level.addFreshEntity(itementity);
		this.controllerData = ItemContainerContents.EMPTY;
	}

	public static boolean playerInRange(final Player player, final Level world, final BlockPos pos) {
		//double modifier = world.isRemote ? 0 : 1.0;
		final double reach = 0.4 * player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);// + modifier;
		return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
	}

	private ItemStack createLinkedController() {
		final ItemStack stack = ItemStack.EMPTY; //AllItems.LINKED_CONTROLLER.asStack();
		stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, this.controllerData);
		return stack;
	}
}
