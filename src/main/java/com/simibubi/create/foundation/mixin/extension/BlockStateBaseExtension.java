package com.simibubi.create.foundation.mixin.extension;

import com.simibubi.create.foundation.block.IBBO;
import com.simibubi.create.foundation.extension.interfaces.IdontKnowHowToCallThis;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseExtension extends StateHolder<Block, BlockState> implements IdontKnowHowToCallThis {

	protected BlockStateBaseExtension(final Block owner, final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values, final MapCodec<BlockState> propertiesCodec) {
		super(owner, values, propertiesCodec);
	}

	@Shadow
	public abstract Block getBlock();

	@Override
	public boolean create$hasBlockBoundObject() {
		return this.getBlock() instanceof IBBO<?>;
	}
}
