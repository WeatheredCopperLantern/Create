package com.simibubi.create.foundation.mixin.accessor;

import net.createmod.catnip.config.ConfigBase;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConfigBase.CValue.class)
public interface CValueAccessor {
	@Accessor("value")
	ModConfigSpec.ConfigValue<?> create$getRawValue();
}
