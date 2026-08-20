package com.simibubi.create.infrastructure.gametest.blockBoundObjects;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.persistent.PersistentObjectType;

import com.tterrag.registrate.util.entry.RegistryEntry;

public class AllTestBlockBoundObjectTypes {

	private static final CreateRegistrate REGISTRATE = Create.registrate();

	public static final RegistryEntry<PersistentObjectType<?>, DummyBlockBoundObjectType> DUMMY = REGISTRATE.persistentObject("dummy", DummyBlockBoundObjectType::new).register();

	public static void register() {
	}
}
