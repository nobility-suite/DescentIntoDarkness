package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.google.common.collect.Lists;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Ore {
	private final String name;
	private final BlockPredicate block;
	private final int pollution;
	@Nullable
	private final List<Drop> dropTable;
	private final int breakAmount;

	public Ore(String name, BlockPredicate block, int pollution, @Nullable List<Drop> dropTable, int breakAmount) {
		this.name = name;
		this.block = block;
		this.pollution = pollution;
		this.dropTable = dropTable;
		this.breakAmount = breakAmount;
	}

	public String getName() {
		return name;
	}

	public BlockPredicate getBlock() {
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

	public static Ore deserialize(String name, ConfigurationSection map) {
		BlockPredicate block = ConfigUtil.parseBlockPredicate(ConfigUtil.require(map, "block"));
		int pollution = map.getInt("pollution", 0);
		Object dropTableVal = map.get("dropTable");
		List<Drop> dropTable;
		if (dropTableVal == null) {
			dropTable = null;
		} else if (ConfigUtil.isConfigurationSection(dropTableVal)) {
			ConfigurationSection dropTableMap = ConfigUtil.asConfigurationSection(dropTableVal);
			dropTable = dropTableMap.getValues(false).values().stream().map(Drop::deserialize).collect(Collectors.toCollection(ArrayList::new));
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

		public static Drop deserialize(Object val) {
			if (ConfigUtil.isConfigurationSection(val)) {
				ConfigurationSection map = ConfigUtil.asConfigurationSection(val);
				ItemStack item = ConfigUtil.parseItem(ConfigUtil.require(map, "item"));
				int weight = map.getInt("weight", 1);
				if (weight < 1) {
					throw new InvalidConfigException("Weight must be positive");
				}
				int minAmount = map.getInt("minAmount", 1);
				int maxAmount = map.getInt("maxAmount", 1);
				if (minAmount < 1 || maxAmount < minAmount || maxAmount > item.getMaxStackSize()) {
					throw new InvalidConfigException("Invalid drop amount range");
				}
				return new Drop(item, weight, minAmount, maxAmount);
			} else {
				return new Drop(ConfigUtil.parseItem(val), 1, 1, 1);
			}
		}
	}
}
