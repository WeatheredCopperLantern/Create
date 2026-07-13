package com.simibubi.create.content.redstone.link.controller;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;

import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LecternControllerBlockEntity extends SmartBlockEntity {

	//region Fields
	private ItemContainerContents controllerData = ItemContainerContents.EMPTY;
	private @Nullable UUID user;
	//endregion

	@Override
	public void tick() {
		super.tick();

		assert this.level != null;
		if (!this.level.isClientSide && this.user != null) {
			final Entity entity = ((ServerLevel) this.level).getEntity(this.user);
			if (!(entity instanceof final Player player) || !LecternControllerBlockEntity.playerInRange(player, this.level, this.worldPosition)) {
				this.removeUser();
			}
		}
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
	}

	private ItemStack createLinkedController() {
		final ItemStack stack = AllItems.LINKED_CONTROLLER.asStack();
		stack.set(AllDataComponents.LINKED_CONTROLLER_ITEMS, this.controllerData);
		return stack;
	}

	public void dropController(final BlockState state) {
		assert this.level != null;
		this.removeUser();

		final Direction dir = state.getValue(LecternBlock.FACING);
		final double x = this.worldPosition.getX() + 0.5 + 0.25 * dir.getStepX();
		final double y = this.worldPosition.getY() + 1;
		final double z = this.worldPosition.getZ() + 0.5 + 0.25 * dir.getStepZ();
		final ItemEntity itementity = new ItemEntity(this.level, x, y, z, this.createLinkedController());
		itementity.setDefaultPickUpDelay();
		this.level.addFreshEntity(itementity);
		this.controllerData = ItemContainerContents.EMPTY;
	}

	public ItemStack getController() {
		return this.createLinkedController();
	}

	public void setController(final ItemStack newController) {
		this.controllerData = newController.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
		AllSoundEvents.CONTROLLER_PUT.playOnServer(this.level, this.worldPosition);
	}

	public boolean hasUser() {
		return this.user != null;
	}

	public boolean isUsedBy(final Player player) {
		return this.hasUser() && player.getUUID().equals(this.user);
	}

	public static boolean playerInRange(final Player player, final Level world, final BlockPos pos) {
		//double modifier = world.isRemote ? 0 : 1.0;
		final double reach = 0.4 * player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);// + modifier;
		return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
	}

	private void removeUser() {
		this.user = null;
		this.sendData();
	}

	private void setUser(final Player player) {
		this.user = player.getUUID();
		this.sendData();
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

	public void tryStartUsing(final Player player) {
		if (!this.hasUser()) {
			this.setUser(player);
		}
	}

	public void tryStopUsing(final Player player) {
		if (this.isUsedBy(player)) this.removeUser();
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		if (!clientPacket) {
			this.controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, registries, compound.get("ControllerData")).orElse(ItemContainerContents.EMPTY);
			this.user = compound.hasUUID("User") ? compound.getUUID("User") : null;
			return;
		}

		final boolean wasUser = Minecraft.getInstance().getUser().getProfileId().equals(this.user);
		this.user = compound.hasUUID("User") ? compound.getUUID("User") : null;

		if (!wasUser && this.user != null && this.user.equals(Minecraft.getInstance().getUser().getProfileId())) {
			LinkedControllerClientHandler.activateInLectern(this.worldPosition);
		} else if (wasUser && !Minecraft.getInstance().getUser().getProfileId().equals(this.user)) {
			LinkedControllerClientHandler.deactivateInLectern();
		}
	}

	@Override
	protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		if (!clientPacket) {
			compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, this.controllerData).orElseThrow());
		}
		if (this.user != null) compound.putUUID("User", this.user);
	}

	@Override
	public void writeSafe(final CompoundTag compound, final HolderLookup.Provider registries) {
		super.writeSafe(compound, registries);
		compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, this.controllerData).orElseThrow());
	}

	public LecternControllerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}
}
