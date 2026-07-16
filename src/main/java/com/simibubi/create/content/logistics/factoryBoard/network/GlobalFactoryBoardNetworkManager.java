package com.simibubi.create.content.logistics.factoryBoard.network;

import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.logistics.factoryBoard.factoryComponent.FactoryComponent;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GlobalFactoryBoardNetworkManager {

	public Map<ResourceKey<Level>, FactoryBoardNetwork> networks;
	public Map<UUID, FactoryComponent> components;
}
