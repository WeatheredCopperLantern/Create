package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ButtonBlock.class)
public interface ButtonBlockAccessor {
	@Accessor("type")
	BlockSetType create$getBlockSetType();

	@Accessor("ticksToStayPressed")
	int create$getTicksToStayPressed();
}
