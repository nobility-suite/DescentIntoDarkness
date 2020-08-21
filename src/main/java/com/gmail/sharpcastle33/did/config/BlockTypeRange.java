package com.gmail.sharpcastle33.did.config;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class BlockTypeRange<T extends Comparable<T>> {
	private final List<Entry<T>> entries;
	private final List<BlockStateHolder<?>> blocks;

	private BlockTypeRange(List<Entry<T>> entries, List<BlockStateHolder<?>> blocks) {
		this.entries = entries;
		this.blocks = blocks;
	}

	@Nullable
	public BlockStateHolder<?> get(T yLevel) {
		for (Entry<T> entry : entries) {
			if (entry.min.compareTo(yLevel) <= 0 && yLevel.compareTo(entry.max) <= 0) {
				return entry.block;
			}
		}
		return null;
	}

	public List<BlockStateHolder<?>> getBlocks() {
		return blocks;
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
		List<BlockStateHolder<?>> blocks = new ArrayList<>();

		if (val instanceof ConfigurationSection) {
			ConfigurationSection map = (ConfigurationSection) val;
			for (String blockStr : map.getKeys(false)) {
				BlockStateHolder<?> block = ConfigUtil.parseBlock(blockStr);
				String rangeVal = ConfigUtil.requireString(map, blockStr);
				rangeVal = rangeVal.replace(" ", "");
				for (String range : rangeVal.split(",")) {
					String[] minMax = range.split("(?<!^)-", 2);
					if (minMax.length == 2) {
						entries.add(new Entry<>(typeParser.apply(minMax[0]), typeParser.apply(minMax[1]), block));
					} else {
						T tVal = typeParser.apply(minMax[0]);
						entries.add(new Entry<>(tVal, tVal, block));
					}
				}
				blocks.add(block);
			}
		} else if (val instanceof String) {
			BlockStateHolder<?> block = ConfigUtil.parseBlock((String) val);
			entries.add(new Entry<>(min, max, block));
			blocks.add(block);
		} else {
			throw new InvalidConfigException(val + " is not a block type range");
		}

		return new BlockTypeRange<>(entries, blocks);
	}

	public static BlockTypeRange<Integer> deserializePainter(int startArg, String[] args) {
		List<Entry<Integer>> entries = new ArrayList<>();
		LinkedHashSet<BlockStateHolder<?>> blocks = new LinkedHashSet<>();

		for (int i = startArg; i < args.length - 1; i += 2) {
			String[] minMax = args[i].split("(?<!^)-", 2);
			BlockStateHolder<?> block = ConfigUtil.parseBlock(args[i + 1]);
			if (minMax.length == 2) {
				entries.add(new Entry<>(ConfigUtil.parseInt(minMax[0]), ConfigUtil.parseInt(minMax[1]), block));
			} else {
				int y = ConfigUtil.parseInt(minMax[0]);
				entries.add(new Entry<>(y, y, block));
			}
			blocks.add(block);
		}

		return new BlockTypeRange<>(entries, new ArrayList<>(blocks));
	}

	public void serialize(ConfigurationSection parentSection, String key) {
		if (entries.size() == 1) {
			parentSection.set(key, entries.get(0).block.getAsString());
			return;
		}

		ConfigurationSection section = parentSection.createSection(key);
		entries.stream()
				.collect(Collectors.groupingBy((Entry<T> entry) -> entry.block, LinkedHashMap::new, Collectors.toList()))
				.forEach((block, entries) -> section.set(block.getAsString(), entries.stream()
						.map(entry -> entry.min.equals(entry.max) ? String.valueOf(entry.min) : (entry.min + "-" + entry.max))
						.collect(Collectors.joining(", "))));
	}

	public String serializePainter() {
		return entries.stream().map(entry -> {
			if (entry.min.equals(entry.max)) {
				return entry.min + " " + entry.block.getAsString();
			} else {
				return entry.min + "-" + entry.max + " " + entry.block.getAsString();
			}
		}).collect(Collectors.joining(" "));
	}

	@Override
	public int hashCode() {
		return entries.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (other == null) return false;
		if (other.getClass() != BlockTypeRange.class) return false;
		BlockTypeRange<?> that = (BlockTypeRange<?>) other;
		return entries.equals(that.entries);
	}

	public static final class Entry<T extends Comparable<T>> {
		private final T min;
		private final T max;
		private final BlockStateHolder<?> block;

		public Entry(T min, T max, BlockStateHolder<?> block) {
			this.min = min;
			this.max = max;
			this.block = block;
		}

		@Override
		public int hashCode() {
			return 31 * (31 * min.hashCode() + max.hashCode()) + block.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) return true;
			if (other == null) return false;
			if (other.getClass() != Entry.class) return false;
			Entry<?> that = (Entry<?>) other;
			return min.equals(that.min) && max.equals(that.max) && block.equals(that.block);
		}
	}
}
