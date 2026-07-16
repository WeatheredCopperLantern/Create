package com.simibubi.create.foundation.item;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import com.tterrag.registrate.util.entry.ItemEntry;
import org.jspecify.annotations.NonNull;

public class DyedItemList<T extends Item> implements Iterable<ItemEntry<T>> {

	private static final int COLOR_AMOUNT = DyeColor.values().length;

	@SuppressWarnings("unchecked")
	private final ItemEntry<T>[] values = new ItemEntry[DyedItemList.COLOR_AMOUNT];

	public DyedItemList(final Function<DyeColor, ItemEntry<T>> filler) {
		for (final DyeColor color : DyeColor.values()) {
			this.values[color.ordinal()] = filler.apply(color);
		}
	}

	public ItemEntry<T> get(final DyeColor color) {
		return this.values[color.ordinal()];
	}

	public boolean contains(final ItemStack itemStack) {
		return this.contains(itemStack.getItem());
	}

	public boolean contains(final Item item) {
		for (final ItemEntry<?> entry : this.values) {
			if (entry.is(item)) {
				return true;
			}
		}
		return false;
	}

	public ItemEntry<T>[] toArray() {
		return Arrays.copyOf(this.values, this.values.length);
	}

	@Override
	public @NonNull Iterator<ItemEntry<T>> iterator() {
		return new Iterator<>() {
			private int index;

			@Override
			public boolean hasNext() {
				return this.index < DyedItemList.this.values.length;
			}

			@Override
			public ItemEntry<T> next() {
				if (!this.hasNext()) throw new NoSuchElementException();
				final ItemEntry<T> tBlockEntry = DyedItemList.this.values[this.index];
				this.index++;
				return tBlockEntry;
			}
		};
	}
}
