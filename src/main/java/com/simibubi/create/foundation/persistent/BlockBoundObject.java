package com.simibubi.create.foundation.persistent;

import com.simibubi.create.Create;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockBoundObject extends PersistentObject {

	protected final Block block;
	protected BlockPos blockPos;
	protected BlockState state;

	@OverridingMethodsMustInvokeSuper
	public void setBlockState(final BlockState state) {
		this.state = state;
	}

	public abstract boolean isValidBlockState(final BlockState state);

	public void saveToItem(ItemStack itemStack) {
		DataComponentMap.Builder builder = DataComponentMap.builder();
		this.collectComponents(builder);
		itemStack.applyComponents(builder.build());
	}

	public void applyComponents(ItemStack itemStack) {

	}

	protected void collectComponents(DataComponentMap.Builder builder) {

	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public Tag save(final CompoundTag tag, final HolderLookup.Provider provider) {
		super.save(tag, provider);
		tag.put("BlockPos", NbtUtils.writeBlockPos(this.blockPos));

		if (this.state != this.block.defaultBlockState()) {
			tag.put("BlockState", NbtUtils.writeBlockState(this.state));
		}

		return tag;
	}

	protected BlockBoundObject(Block block, BlockPos blockPos, BlockState state, final ServerLevel serverLevel) {
		super(serverLevel);
		this.block = block;
		this.blockPos = blockPos;
		this.state = state;
	}

	protected BlockBoundObject(Block block, final CompoundTag compoundTag, final HolderLookup.Provider registries, final ServerLevel serverLevel) {
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
}
