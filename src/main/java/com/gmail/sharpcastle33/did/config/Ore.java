package com.gmail.sharpcastle33.did.config;

import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Ore {
	private final String name;
	private final BlockStateHolder<?> block;
	private final int pollution;
	@Nullable
	private final ItemStack dropItem;
	private final int minDropAmount;
	private final int maxDropAmount;
	private final int breakAmount;

	public Ore(String name, BlockStateHolder<?> block, int pollution, @Nullable ItemStack dropItem, int minDropAmount, int maxDropAmount, int breakAmount) {
		this.name = name;
		this.block = block;
		this.pollution = pollution;
		this.dropItem = dropItem;
		this.minDropAmount = minDropAmount;
		this.maxDropAmount = maxDropAmount;
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
	public ItemStack getDropItem() {
		return dropItem;
	}

	public int getMinDropAmount() {
		return minDropAmount;
	}

	public int getMaxDropAmount() {
		return maxDropAmount;
	}

	public int getBreakAmount() {
		return breakAmount;
	}

	public void serialize(ConfigurationSection map) {
		map.set("block", block.getAsString());
		map.set("pollution", pollution);
		if (dropItem != null) {
			map.set("dropItem", ConfigUtil.serializeItemStack(dropItem));
		}
		map.set("minDropAmount", minDropAmount);
		map.set("maxDropAmount", maxDropAmount);
		map.set("breakAmount", breakAmount);
	}

	public static Ore deserialize(String name, ConfigurationSection map) {
		BlockStateHolder<?> block = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "block"));
		int pollution = map.getInt("pollution", 0);
		Object dropItemVal = map.get("dropItem");
		ItemStack dropItem;
		if (dropItemVal == null) {
			dropItem = null;
		} else {
			dropItem = ConfigUtil.parseItem(dropItemVal);
		}
		int minDropAmount = map.getInt("minDropAmount", dropItem == null ? 1 : dropItem.getAmount());
		int maxDropAmount = map.getInt("maxDropAmount", dropItem == null ? 1 : dropItem.getAmount());
		if (minDropAmount > maxDropAmount) {
			throw new InvalidConfigException("minDropAmount > maxDropAmount");
		}
		int breakAmount = map.getInt("breakAmount", 10);
		if (breakAmount <= 0) {
			throw new InvalidConfigException("Break amount must be positive");
		}
		return new Ore(name, block, pollution, dropItem, minDropAmount, maxDropAmount, breakAmount);
	}
}
