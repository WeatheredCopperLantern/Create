package com.simibubi.create.foundation.blockEntity;

import java.util.function.Consumer;

import com.simibubi.create.foundation.persistent.BlockBoundObject;
import com.simibubi.create.foundation.persistent.IBBOBlockEntity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BBOBlockEntity<T extends BlockBoundObject> extends SmartBlockEntity implements IBBOBlockEntity<T> {

	private T bbo;

	public BBOBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	@Override
	public @Nullable T getBlockBoundObject(final BlockGetter level, final BlockPos pos) {
		if (this.bbo == null) {
			this.bbo = IBBOBlockEntity.super.getBlockBoundObject(level, pos);
		}
		return this.bbo;
	}

	public @Nullable T getBlockBoundObject() {
		if (this.bbo == null) {
			this.bbo = IBBOBlockEntity.super.getBlockBoundObject(this.level, this.getBlockPos());
		}
		return this.bbo;
	}

	public void withBlockBoundObjectDo(Consumer<T> action) {
		final T bbo = this.getBlockBoundObject();
		if (bbo != null) action.accept(bbo);
	}
}
