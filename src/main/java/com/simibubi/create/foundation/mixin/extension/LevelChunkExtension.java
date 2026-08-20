package com.simibubi.create.foundation.mixin.extension;

import java.util.List;

import com.simibubi.create.foundation.extension.interfaces.ILevelChunkInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;

import net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelChunk.class)
public abstract class LevelChunkExtension extends ChunkAccess implements ILevelChunkInterface {

	@Shadow
	@Final
	Level level;

	@SuppressWarnings("UnstableApiUsage")
	@Shadow
	@Final
	private LevelChunkAuxiliaryLightManager auxLightManager;

	public LevelChunkExtension(final ChunkPos chunkPos, final UpgradeData upgradeData, final LevelHeightAccessor levelHeightAccessor, final Registry<Biome> biomeRegistry, final long inhabitedTime, @Nullable final LevelChunkSection[] sections, @Nullable final BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
	}

	@Shadow
	protected abstract boolean isInLevel();

	@Shadow
	protected abstract <T extends BlockEntity> void addGameEventListener(final T blockEntity, final ServerLevel serverLevel);

	@Shadow
	protected abstract <T extends BlockEntity> void updateBlockEntityTicker(final T blockEntity);

	@Override
	public void create$forceAddAndRegisterBlockEntity(final BlockEntity blockEntity) {
		BlockPos blockpos = blockEntity.getBlockPos();
		blockEntity.setLevel(this.level);
		blockEntity.clearRemoved();
		BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), blockEntity);
		if (blockentity != null && blockentity != blockEntity) {
			blockentity.setRemoved();
			this.auxLightManager.removeLightAt(blockpos);
		}
		if (this.isInLevel()) {
			Level var3 = this.level;
			if (var3 instanceof final ServerLevel serverlevel) {
				this.addGameEventListener(blockEntity, serverlevel);
			}

			this.updateBlockEntityTicker(blockEntity);
			this.level.addFreshBlockEntities(List.of(blockEntity));
		}
	}
}
