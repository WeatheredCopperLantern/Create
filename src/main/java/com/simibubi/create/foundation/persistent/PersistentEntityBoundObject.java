package com.simibubi.create.foundation.persistent;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PersistentEntityBoundObject extends PersistentObject {

	protected PersistentEntityBoundObject(final CompoundTag compoundTag, final ServerLevel serverLevel) {
		super(compoundTag, serverLevel);
	}

	protected PersistentEntityBoundObject(final ServerLevel serverLevel) {
		super(serverLevel);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public Tag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		super.save(tag, registries);
		//tag.put("BlockPos", NbtUtils.writeBlockPos(this.blockPos));
//
		//if (this.state != this.block.defaultBlockState()) {
		//	tag.put("BlockState", NbtUtils.writeBlockState(this.state));
		//}

		return tag;
	}

	public abstract void blockUnloaded();

	@OverridingMethodsMustInvokeSuper
	public void blockDestroyed() { //ENTITY UNLOADED/KILLED
		withManagerDo(manager -> manager.removeObject(this));
	}
}
