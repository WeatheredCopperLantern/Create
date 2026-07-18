package com.simibubi.create.content.redstone.link.controller.lecternController;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LecternControllerBlockEntity extends SmartBlockEntity {

	//region Fields
	private DyeColor color = DyeColor.BROWN;
	private ItemContainerContents controllerData = ItemContainerContents.EMPTY;
	private @Nullable UUID user;
	//endregion

	@Override
	public void tick() {
		super.tick();
		if (!(this.level instanceof ServerLevel serverLevel) || this.user == null) return;

		final Entity entity = serverLevel.getEntity(this.user);
		if (entity == null || (entity instanceof LivingEntity livingEntity && !LecternControllerBlockEntity.entityInRange(livingEntity, this.worldPosition))) {
			this.removeUser();
		}
	}

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
	}

	private ItemStack createLinkedController() {
		final ItemStack stack = AllItems.LINKED_CONTROLLERS.get(this.color).asStack();
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
		this.color = DyeColor.BROWN;
	}

	public ItemStackHandler getFrequencyItems() {
		final ItemStackHandler newInv = new ItemStackHandler(12);
		ItemHelper.fillItemStackHandler(this.controllerData, newInv);
		return newInv;
	}

	public DyeColor getColor() {
		return this.color;
	}

	public void setController(final ItemStack newController) {
		if (!(newController.getItem() instanceof LinkedControllerItem linkedControllerItem)) return;

		this.controllerData = newController.getOrDefault(AllDataComponents.LINKED_CONTROLLER_ITEMS, ItemContainerContents.EMPTY);
		this.color = linkedControllerItem.color;
		AllSoundEvents.CONTROLLER_PUT.playOnServer(this.level, this.worldPosition);
		this.sendData();
		this.setChanged();
	}

	public boolean hasUser() {
		return this.user != null;
	}

	public boolean isUsedBy(final Entity entity) {
		return this.hasUser() && entity.getUUID().equals(this.user);
	}

	public static boolean entityInRange(final LivingEntity livingEntity, final BlockPos pos) {
		final double reach = 0.4 * livingEntity.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
		return livingEntity.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
	}

	private void removeUser() {
		if (this.user != null && this.level instanceof ServerLevel serverLevel && serverLevel.getEntity(this.user) instanceof Entity entity) {
			Create.LINKED_CONTROLLER_HANDLER.remove(entity);
		}
		this.user = null;
		this.sendData();
		this.setChanged();
	}

	private void setUser(final Entity entity) {
		this.user = entity.getUUID();
		this.sendData();
		this.setChanged();
	}

	public void swapControllers(final ItemStack stack, final LivingEntity livingEntity, final InteractionHand hand, final BlockState state) {
		final ItemStack newController = stack.copy();
		stack.setCount(0);
		if (livingEntity.getItemInHand(hand).isEmpty()) {
			livingEntity.setItemInHand(hand, this.createLinkedController());
		} else {
			this.dropController(state);
		}
		this.setController(newController);
	}

	public void tryStartUsing(final Entity entity) {
		if (!this.hasUser()) {
			this.setUser(entity);
		}
	}

	public void tryStopUsing(final Entity entity) {
		if (this.isUsedBy(entity)) this.removeUser();
	}

	@Override
	protected void read(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		this.color = compound.contains("color") ? DyeColor.byId(compound.getInt("color")) : DyeColor.BROWN;

		if (!clientPacket) {
			this.user = compound.hasUUID("User") ? compound.getUUID("User") : null;
			this.controllerData = CatnipCodecUtils.decode(ItemContainerContents.CODEC, registries, compound.get("ControllerData")).orElse(ItemContainerContents.EMPTY);
			return;
		}

		final UUID currentUUID = Minecraft.getInstance().getUser().getProfileId();
		final boolean wasUser = currentUUID.equals(this.user);
		this.user = compound.hasUUID("User") ? compound.getUUID("User") : null;

		if (!wasUser && currentUUID.equals(this.user)) {
			CreateClient.LINKED_CONTROLLER_HANDLER.activateInLectern(this.worldPosition);
		} else if (wasUser && !Minecraft.getInstance().getUser().getProfileId().equals(this.user)) {
			CreateClient.LINKED_CONTROLLER_HANDLER.deactivateInLectern();
		}
	}

	@Override
	protected void write(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		if (!clientPacket) {
			compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, this.controllerData).orElseThrow());
		}

		compound.putInt("color", this.color.getId());
		if (this.user != null) compound.putUUID("User", this.user);
	}

	@Override
	public void writeSafe(final CompoundTag compound, final HolderLookup.Provider registries) {
		super.writeSafe(compound, registries);
		compound.putInt("color", this.color.getId());
		compound.put("ControllerData", CatnipCodecUtils.encode(ItemContainerContents.CODEC, registries, this.controllerData).orElseThrow());
	}

	@Override
	public void remove() {
		assert this.level != null;
		if (this.level.isClientSide && Minecraft.getInstance().getUser().getProfileId().equals(this.user)) {
			CreateClient.LINKED_CONTROLLER_HANDLER.deactivateInLectern();
		}
	}

	public LecternControllerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}
}
