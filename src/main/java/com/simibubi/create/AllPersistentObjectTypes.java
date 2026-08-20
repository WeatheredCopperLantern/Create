package com.simibubi.create;

import com.simibubi.create.content.redstone.link.redstoneLink.RedstoneLinkPersistentObjectType;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import com.tterrag.registrate.util.entry.RegistryEntry;

public class AllPersistentObjectTypes {

	private static final CreateRegistrate REGISTRATE = Create.registrate();

	public static final RegistryEntry<PersistentObjectType<?>, RedstoneLinkPersistentObjectType> REDSTONE_LINK = REGISTRATE.persistentObject("redstone_link", RedstoneLinkPersistentObjectType::new).register();

	public static void register() {
	}
}
