package com.simibubi.create.infrastructure.gametest.blocks;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.util.entry.BlockEntry;

public class AllTestBlocks {

	private static final CreateRegistrate REGISTRATE = Create.registrate();

	public static final BlockEntry<ImmovableBlock> IMMOVABLE_BE =
		REGISTRATE.block("gametest_immovable_be", ImmovableBlock::new)
			.initialProperties(() -> Blocks.STONE)
			.blockstate((ctx, prov) ->
				prov.simpleBlock(ctx.getEntry(), prov.models().getExistingFile(
					ResourceLocation.withDefaultNamespace("block/obsidian")
				))
			)
			.register();

	public static final BlockEntry<MovableBlock> MOVABLE_BE =
		REGISTRATE.block("gametest_movable_be", MovableBlock::new)
			.initialProperties(() -> Blocks.STONE)
			.blockstate((ctx, prov) ->
				prov.simpleBlock(ctx.getEntry(), prov.models().getExistingFile(
					ResourceLocation.withDefaultNamespace("block/stone")
				))
			)
			.register();

	public static final BlockEntry<BBOBlock> BBO =
		REGISTRATE.block("gametest_bbo", BBOBlock::new)
			.initialProperties(() -> Blocks.STONE)
			.blockstate((ctx, prov) ->
				prov.simpleBlock(ctx.getEntry(), prov.models().getExistingFile(
					ResourceLocation.withDefaultNamespace("block/redstone_block")
				))
			)
			.register();

	public static void register() {
	}
}
