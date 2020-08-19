package com.gmail.sharpcastle33.did.config;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Ore {
	private final String name;
	private final BlockStateHolder<?> block;
	private final int pollution;
	@Nullable
	private final List<Drop> dropTable;
	private final int breakAmount;

	public Ore(String name, BlockStateHolder<?> block, int pollution, @Nullable List<Drop> dropTable, int breakAmount) {
		this.name = name;
		this.block = block;
		this.pollution = pollution;
		this.dropTable = dropTable;
		this.breakAmount = breakAmount;
	}

	public String getName() {
		return name;
	}

	public BlockStateHolder<?> getBlock() {
		return block;
	}

	public int getPollution() {
		return pollution;
	}

	@Nullable
	public List<Drop> getDropTable() {
		return dropTable;
	}

	public int getBreakAmount() {
		return breakAmount;
	}

	public void serialize(ConfigurationSection map) {
		map.set("block", block.getAsString());
		map.set("pollution", pollution);
		if (dropTable != null) {
			if (dropTable.size() == 1) {
				dropTable.get(0).serialize(map, "dropTable");
			} else {
				ConfigurationSection dropTableSection = map.createSection("dropTable");
				int dropId = 0;
				for (Drop drop : dropTable) {
					drop.serialize(dropTableSection, "drop" + (++dropId));
				}
			}
		}
		map.set("breakAmount", breakAmount);
	}

	@SuppressWarnings("unchecked")
	public static Ore deserialize(String name, ConfigurationSection map) {
		BlockStateHolder<?> block = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "block"));
		int pollution = map.getInt("pollution", 0);
		Object dropTableVal = map.get("dropTable");
		List<Drop> dropTable;
		if (dropTableVal == null) {
			dropTable = null;
		} else if (dropTableVal instanceof Map || dropTableVal instanceof ConfigurationSection) {
			Map<String, Object> dropTableMap = dropTableVal instanceof Map ? (Map<String, Object>) dropTableVal : ((ConfigurationSection) dropTableVal).getValues(false);
			dropTable = dropTableMap.values().stream().map(Drop::deserialize).collect(Collectors.toCollection(ArrayList::new));
		} else {
			dropTable = Lists.newArrayList(Drop.deserialize(dropTableVal));
		}
		int breakAmount = map.getInt("breakAmount", 10);
		if (breakAmount <= 0) {
			throw new InvalidConfigException("Break amount must be positive");
		}
		return new Ore(name, block, pollution, dropTable, breakAmount);
	}

	public static class Drop {
		private final ItemStack item;
		private final int weight;
		private final int minAmount;
		private final int maxAmount;

		public Drop(ItemStack item, int weight, int minAmount, int maxAmount) {
			this.item = item;
			this.weight = weight;
			this.minAmount = minAmount;
			this.maxAmount = maxAmount;
		}

		public ItemStack getItem() {
			return item;
		}

		public int getWeight() {
			return weight;
		}

		public int getMinAmount() {
			return minAmount;
		}

		public int getMaxAmount() {
			return maxAmount;
		}

		public void serialize(ConfigurationSection parentSection, String key) {
			Object serializedItem = ConfigUtil.serializeItemStack(item);
			if (weight == 1 && minAmount == 1 && maxAmount == 1 && !(serializedItem instanceof Map)) {
				parentSection.set(key, serializedItem);
			} else {
				ConfigurationSection map = parentSection.createSection(key);
				map.set("item", serializedItem);
				if (weight != 1) {
					map.set("weight", weight);
				}
				if (minAmount != 1) {
					map.set("minAmount", minAmount);
				}
				if (maxAmount != 1) {
					map.set("maxAmount", maxAmount);
				}
			}
		}

		@SuppressWarnings("unchecked")
		public static Drop deserialize(Object val) {
			if (val instanceof ConfigurationSection) {
				return deserialize(((ConfigurationSection) val).getValues(false));
			} else if (val instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) val;
				Object itemVal = map.get("item");
				if (itemVal == null) {
					throw new InvalidConfigException("Missing \"item\"");
				}
				ItemStack item = ConfigUtil.parseItem(itemVal);
				int weight = asInt(map.getOrDefault("weight", 1));
				if (weight < 1) {
					throw new InvalidConfigException("Weight must be positive");
				}
				int minAmount = asInt(map.getOrDefault("minAmount", 1));
				int maxAmount = asInt(map.getOrDefault("maxAmount", 1));
				if (minAmount < 1 || maxAmount < minAmount || maxAmount > item.getMaxStackSize()) {
					throw new InvalidConfigException("Invalid drop amount range");
				}
				return new Drop(item, weight, minAmount, maxAmount);
			} else {
				return new Drop(ConfigUtil.parseItem(val), 1, 1, 1);
			}
		}

		private static int asInt(Object val) {
			if (val instanceof Number) {
				return ((Number) val).intValue();
			} else {
				throw new InvalidConfigException("Not an integer");
			}
		}
	}
}
