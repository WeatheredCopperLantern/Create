package com.simibubi.create.content.redstone.link;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.createmod.catnip.nbt.NBTHelper;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import org.apache.commons.codec.digest.MurmurHash3;
import org.jspecify.annotations.NonNull;

public class Frequency {

	public static final Frequency EMPTY = new Frequency(Items.AIR, true);
	// TODO: save sizes and apply during load to avoid/reduce resizing
	private static final Map<Item, Frequency> basicFrequencies = new IdentityHashMap<>(32);
	private static final Map<FrequencyHash, FrequencyRef> complexFrequencies = new HashMap<>(16);

	private static final ReferenceQueue<Frequency> queue = new ReferenceQueue<>();

	public final ItemStack stack;
	private final Item item;
	private final boolean isSimple;
	private final DataComponentPatch componentPatch;
	private final FrequencyHash hash;

	@SuppressWarnings("unused")
	private Frequency(final Item item, final DataComponentPatch componentPatch, final FrequencyHash hash, final boolean doNotCallThisUnlessYouUnderstandExactlyHowTheFrequencyMapsWork) {
		this.item = item;
		this.isSimple = false;
		this.componentPatch = componentPatch;
		this.hash = hash;
		this.stack = new ItemStack(Holder.direct(this.item), 1, this.componentPatch);
	}

	@SuppressWarnings("unused")
	private Frequency(final Item item, final boolean doNotCallThisUnlessYouUnderstandExactlyHowTheFrequencyMapsWork) {
		this.item = item;
		this.isSimple = true;
		this.componentPatch = DataComponentPatch.EMPTY;
		this.hash = new FrequencyHash(0, new long[2]);
		this.stack = new ItemStack(this.item);
	}

	public static Frequency of(final @NonNull ItemStack stack) {
		if (stack.isEmpty()) return Frequency.EMPTY;

		final Item item = stack.getItem();
		if (stack.getComponents().isEmpty())
			return Frequency.basicFrequencies.computeIfAbsent(item, u -> new Frequency(item, true));

		final DataComponentPatch componentPatch = Frequency.extractAllowedPatches(stack.getComponentsPatch(), item).build();

		if (componentPatch.isEmpty())
			return Frequency.basicFrequencies.computeIfAbsent(item, u -> new Frequency(item, true));

		final FrequencyHash frequencyHash = new FrequencyHash(item.hashCode(), MurmurHash3.hash128x64((item.toString() + componentPatch).getBytes(StandardCharsets.UTF_8)));
		return Frequency.complexFrequencies.computeIfAbsent(frequencyHash, u -> {
			Frequency.cleanUp();
			return new FrequencyRef(new Frequency(item, componentPatch, frequencyHash, true));
		}).get();
	}

	private static void cleanUp() {
		FrequencyRef ref;
		while ((ref = (FrequencyRef) Frequency.queue.poll()) != null) {
			Frequency.complexFrequencies.remove(ref.hash, ref);
		}
	}

	@SuppressWarnings("unused") // Item will be useful for mixins
	public static DataComponentPatch.@NonNull Builder extractAllowedPatches(final DataComponentPatch patch, final Item item) {
		final DataComponentPatch.Builder builder = DataComponentPatch.builder();

		if (Frequency.isPatchSet(patch, DataComponents.CUSTOM_NAME)) {
			Frequency.copyPatch(builder, patch, DataComponents.CUSTOM_NAME); // Name set in Anvil
		} else {
			Frequency.copyPatch(builder, patch, DataComponents.ITEM_NAME); // Acts like the default item Name
		}

		Frequency.copyPatch(builder, patch, DataComponents.ENCHANTMENTS); // "Active" Enchantments
		Frequency.copyPatch(builder, patch, DataComponents.STORED_ENCHANTMENTS); // Enchantments on Books
		Frequency.copyPatch(builder, patch, DataComponents.CUSTOM_MODEL_DATA);
		Frequency.copyPatch(builder, patch, DataComponents.DYED_COLOR);
		Frequency.copyPatch(builder, patch, DataComponents.POTION_CONTENTS);
		Frequency.copyPatch(builder, patch, DataComponents.TRIM);
		Frequency.copyPatch(builder, patch, DataComponents.FIREWORK_EXPLOSION); // Firework Star
		Frequency.copyPatch(builder, patch, DataComponents.FIREWORKS); // Firework Rocket
		Frequency.copyPatch(builder, patch, DataComponents.PROFILE); // Player Heads
		Frequency.copyPatch(builder, patch, DataComponents.BANNER_PATTERNS);
		Frequency.copyPatch(builder, patch, DataComponents.BASE_COLOR); // Color of Banner on Shield
		Frequency.copyPatch(builder, patch, DataComponents.POT_DECORATIONS);

		if (Frequency.isPatchSet(patch, DataComponents.BUCKET_ENTITY_DATA)) {
			final Optional<? extends CustomData> bucketEntityData = patch.get(DataComponents.BUCKET_ENTITY_DATA);
			assert Objects.requireNonNull(bucketEntityData).isPresent(); // Make IDE shut up
			final CompoundTag fullTag = bucketEntityData.get().copyTag();
			final CompoundTag cleanedTag = new CompoundTag();
			if (fullTag.contains("BucketVariantTag", Tag.TAG_INT)) { //Tropical Fish
				cleanedTag.putInt("BucketVariantTag", fullTag.getInt("BucketVariantTag"));
			} else if (fullTag.contains("Variant", Tag.TAG_INT)) { //Axolotl
				cleanedTag.putInt("Variant", fullTag.getInt("Variant"));
			}
			builder.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(cleanedTag));
		}

		return builder;
	}

	public static <T> boolean isPatchSet(final @NonNull DataComponentPatch patch, final DataComponentType<T> component) {
		final Optional<? extends T> optional = patch.get(component);
		return optional != null && optional.isPresent();
	}

	public static <T> void copyPatch(final DataComponentPatch.Builder builder, final @NonNull DataComponentPatch patch, final DataComponentType<T> component) {
		final Optional<? extends T> optional = patch.get(component);
		if (optional != null && optional.isPresent()) {
			builder.set(component, optional.get());
		}
	}

	public static Frequency read(final CompoundTag nbt, final HolderLookup.Provider registries) {
		if (nbt.contains("Item")) {
			return Frequency.readNew(nbt);
		} else if (nbt.contains("id")) {
			return Frequency.readLegacy(nbt, registries);
		}
		return Frequency.EMPTY;
	}

	private static Frequency readNew(final CompoundTag nbt) {
		final Item item = BuiltInRegistries.ITEM.get(NBTHelper.readResourceLocation(nbt, "Item"));
		if (nbt.getBoolean("Simple")) {
			return Frequency.of(new ItemStack(item));
		}
		final DataComponentPatch componentPatch = DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, nbt.get("Patch")).getOrThrow();
		return Frequency.of(new ItemStack(Holder.direct(item), 1, componentPatch));
	}

	private static Frequency readLegacy(final CompoundTag nbt, final HolderLookup.Provider registries) {
		return Frequency.of(ItemStack.parseOptional(registries, nbt));
	}

	public CompoundTag write() {
		final CompoundTag nbt = new CompoundTag();
		NBTHelper.writeResourceLocation(nbt, "Item", BuiltInRegistries.ITEM.getKey(this.item));
		nbt.putBoolean("Simple", this.isSimple);
		if (!this.isSimple) {
			nbt.put("Patch", DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, this.componentPatch).getOrThrow());
		}
		return nbt;
	}

	@Override
	public String toString() {
		return this.item.toString() + ((this.isSimple) ? "" : this.componentPatch.toString());
	}

	private static class FrequencyRef extends WeakReference<Frequency> {

		private final FrequencyHash hash;

		private FrequencyRef(final Frequency referent) {
			super(referent, Frequency.queue);
			this.hash = referent.hash;
		}
	}

	private record FrequencyHash(int itemHash, long[] patchHash) {

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof FrequencyHash(
					final int itemHash, final long[] patchHash
			) && this.itemHash == itemHash && this.patchHash[0] == patchHash[0] && this.patchHash[1] == patchHash[1];
		}

		@Override
		public int hashCode() {
			return (Arrays.hashCode(this.patchHash) * 31) ^ this.itemHash;
		}
	}
}
