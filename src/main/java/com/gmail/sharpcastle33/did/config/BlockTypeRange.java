package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.Pair;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.google.common.collect.Lists;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class BlockTypeRange<T extends Comparable<T>> {
	private final List<Entry<T>> entries;

	private BlockTypeRange(List<Entry<T>> entries) {
		this.entries = entries;
	}

	@Nullable
	public BlockProvider get(T yLevel) {
		for (Entry<T> entry : entries) {
			if (entry.min.compareTo(yLevel) <= 0 && yLevel.compareTo(entry.max) <= 0) {
				return entry.block;
			}
		}
		return null;
	}

	public void validateRange(T min, T max, UnaryOperator<T> nextDown, UnaryOperator<T> nextUp) {
		List<Pair<T, T>> unaccountedList = Lists.newArrayList(Pair.of(min, max));
		for (Entry<T> entry : entries) {
			for (int i = unaccountedList.size() - 1; i >= 0; i--) {
				Pair<T, T> unaccounted = unaccountedList.get(i);
				if (unaccounted.getRight().compareTo(entry.min) < 0 || unaccounted.getLeft().compareTo(entry.max) > 0) {
					// ranges do not overlap
					continue;
				}
				// ranges overlap, so this whole range surely can't stay
				unaccountedList.remove(i);
				// the top side of the range
				if (entry.max.compareTo(unaccounted.getRight()) < 0) {
					unaccountedList.add(i, Pair.of(nextUp.apply(entry.max), unaccounted.getRight()));
				}
				// the bottom side of the range
				if (unaccounted.getLeft().compareTo(entry.min) < 0) {
					unaccountedList.add(i, Pair.of(unaccounted.getLeft(), nextDown.apply(entry.min)));
				}
			}
		}
		if (!unaccountedList.isEmpty()) {
			throw new InvalidConfigException("Range list does not take into account ranges " + unaccountedList.stream()
					.map(range -> range.getLeft().equals(range.getRight()) ? String.valueOf(range.getLeft()) : (range.getLeft() + "-" + range.getRight()))
					.collect(Collectors.joining(", ")));
		}
	}

	public static BlockTypeRange<Integer> deserializeInt(Object val) {
		return deserialize(val, ConfigUtil::parseInt, 0, 255);
	}

	public static BlockTypeRange<Double> deserializeDouble(Object val) {
		return deserialize(val, ConfigUtil::parseDouble, 0.0, 1.0);
	}

	private static <T extends Comparable<T>> BlockTypeRange<T> deserialize(Object val, Function<String, T> typeParser, T min, T max) {
		List<Entry<T>> entries = new ArrayList<>();

		if (val instanceof List) {
			List<?> list = (List<?>) val;
			for (Object entry : list) {
				entries.add(parseEntry(entry, typeParser, min, max));
			}
		} else {
			entries.add(parseEntry(val, typeParser, min, max));
		}

		return new BlockTypeRange<>(entries);
	}

	private static <T extends Comparable<T>> Entry<T> parseEntry(Object val, Function<String, T> typeParser, T min, T max) {
		if (ConfigUtil.isConfigurationSection(val)) {
			ConfigurationSection section = ConfigUtil.asConfigurationSection(val);
			T minVal = section.contains("min") ? typeParser.apply(section.getString("min")) : min;
			T maxVal = section.contains("max") ? typeParser.apply(section.getString("max")) : max;
			return new Entry<>(minVal, maxVal, ConfigUtil.parseBlockProvider(ConfigUtil.require(section, "block")));
		} else if (val instanceof String) {
			return new Entry<>(min, max, ConfigUtil.parseBlockProvider(val));
		} else {
			throw new IllegalArgumentException("Invalid entry type");
		}
	}

	public static final class Entry<T extends Comparable<T>> {
		private final T min;
		private final T max;
		private final BlockProvider block;

		public Entry(T min, T max, BlockProvider block) {
			this.min = min;
			this.max = max;
			this.block = block;
		}
	}
}
