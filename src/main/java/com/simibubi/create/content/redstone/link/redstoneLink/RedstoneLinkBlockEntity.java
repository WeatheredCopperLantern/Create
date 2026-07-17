package com.simibubi.create.content.redstone.link.redstoneLink;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.link.linkable.LinkableBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import org.jspecify.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneLinkBlockEntity extends LinkableBlockEntity<RedstoneLinkLinkable> {

	public FactoryPanelSupportBehaviour panelSupport;

	@Override
	public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
		//TODO: refactor Factory Panels to run serverside only and sync data for visuals to client
		behaviours.add(this.panelSupport = new FactoryPanelSupportBehaviour(this));
	}

	@Override
	protected RedstoneLinkLinkable createFallbackLinkable() {
		return new RedstoneLinkLinkable(this.channel, this);
	}

	@Override
	protected void setSelfOnLinkable() {
		this.linkable.setBlockEntity(this);
	}

	public RedstoneLinkBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state, RedstoneLinkLinkable.class);
	}

	@Override
	public Component getDisplayName() {
		return AllBlocks.REDSTONE_LINK.get().getName();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(final int i, final Inventory inventory, final Player player) {
		return RedstoneLinkMenu.create(i, inventory, this);
	}
}
