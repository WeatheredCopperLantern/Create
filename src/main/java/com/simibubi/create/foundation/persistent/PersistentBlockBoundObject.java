package com.simibubi.create.foundation.persistent;

import com.simibubi.create.Create;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PersistentBlockBoundObject extends PersistentObject {

	protected final Block block;
	protected BlockPos blockPos;
	protected BlockState state;

	protected PersistentBlockBoundObject(Block block, BlockPos blockPos, BlockState state, final ServerLevel serverLevel) {
		super(serverLevel);
		this.block = block;
		this.blockPos = blockPos;
		this.state = state;
	}

	protected PersistentBlockBoundObject(Block block, final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
		super(compoundTag, serverLevel);
		this.block = block;
		this.blockPos = NbtUtils.readBlockPos(compoundTag, "BlockPos").orElse(BlockPos.ZERO);
		if (compoundTag.contains("BlockState")) {
			this.state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), compoundTag.getCompound("BlockState"));
			if (state.getBlock() != block) {
				Create.LOGGER.error("Couldn't load state, falling back to defaultBlockState()");
				this.state = null;
			}
		}
		if (this.state == null) {
			this.state = block.defaultBlockState();
		}
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public Tag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		super.save(tag, registries);
		tag.put("BlockPos", NbtUtils.writeBlockPos(this.blockPos));

		if (this.state != this.block.defaultBlockState()) {
			tag.put("BlockState", NbtUtils.writeBlockState(this.state));
		}

		return tag;
	}

	public abstract void blockUnloaded();

	@OverridingMethodsMustInvokeSuper
	public void blockDestroyed() {
		withManagerDo(manager -> manager.removeObject(this));
	}
}
