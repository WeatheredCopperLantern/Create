package com.simibubi.create.foundation.blockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class DummBlockEntity extends BlockEntity {

	public DummBlockEntity(BlockEntityType<?> type, BlockPos pos,
		BlockState blockState) {
		super(type, pos, blockState);
	}

	@Override
	public boolean isValidBlockState(BlockState p_353131_) {
		return true;
	}

	@Override
	public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
	}

	@Override
	public void setChanged() {
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
		HolderLookup.Provider lookupProvider) {
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
	}

	@Override
	public void onLoad() {
	}

	@Override
	public void requestModelDataUpdate() {
	}

	@Override
	public void invalidateCapabilities() {
	}

	@Override
	public <T> boolean hasData(Supplier<AttachmentType<T>> type) {
		return false;
	}

	@Override
	public <T> Optional<T> getExistingData(AttachmentType<T> type) {
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> getExistingData(Supplier<AttachmentType<T>> type) {
		return Optional.empty();
	}

	@Override
	public @Nullable <T> T getExistingDataOrNull(AttachmentType<T> type) {
		return null;
	}

	@Override
	public @Nullable <T> T getExistingDataOrNull(Supplier<AttachmentType<T>> type) {
		return null;
	}

	@Override
	public @Nullable <T> T setData(Supplier<AttachmentType<T>> type, T data) {
		return null;
	}

	@Override
	public @Nullable <T> T removeData(Supplier<AttachmentType<T>> type) {
		return null;
	}

	@Override
	public void syncData(Supplier<? extends AttachmentType<?>> type) {
	}
}
