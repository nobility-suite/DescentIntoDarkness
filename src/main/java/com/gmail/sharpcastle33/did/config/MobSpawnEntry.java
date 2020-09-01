package com.gmail.sharpcastle33.did.config;

import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class MobSpawnEntry {
	private final String name;
	private final String mob;
	private final int singleMobCost;
	private final int minPackCost;
	private final int maxPackCost;
	private final int weight;
	private final int minDistance;
	private final int maxDistance;
	private final int cooldown;
	private final int despawnRange;
	private final double xSize;
	private final double ySize;
	private final double zSize;
	private final List<BlockStateHolder<?>> canSpawnOn;
	private final List<BlockStateHolder<?>> canSpawnIn;
	private final boolean centeredSpawn;

	public MobSpawnEntry(
			String name,
			String mob,
			int singleMobCost,
			int minPackCost,
			int maxPackCost,
			int weight,
			int minDistance,
			int maxDistance,
			int cooldown,
			int despawnRange,
			double xSize,
			double ySize,
			double zSize,
			List<BlockStateHolder<?>> canSpawnOn,
			List<BlockStateHolder<?>> canSpawnIn,
			boolean centeredSpawn
	) {
		this.name = name;
		this.mob = mob;
		this.singleMobCost = singleMobCost;
		this.minPackCost = minPackCost;
		this.maxPackCost = maxPackCost;
		this.weight = weight;
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
		this.cooldown = cooldown;
		this.despawnRange = despawnRange;
		this.xSize = xSize;
		this.ySize = ySize;
		this.zSize = zSize;
		this.canSpawnOn = canSpawnOn;
		this.canSpawnIn = canSpawnIn;
		this.centeredSpawn = centeredSpawn;
	}

	public String getName() {
		return name;
	}

	public String getMob() {
		return mob;
	}

	public int getSingleMobCost() {
		return singleMobCost;
	}

	public int getMinPackCost() {
		return minPackCost;
	}

	public int getMaxPackCost() {
		return maxPackCost;
	}

	public int getWeight() {
		return weight;
	}

	public int getMinDistance() {
		return minDistance;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public int getCooldown() {
		return cooldown;
	}

	public int getDespawnRange() {
		return despawnRange;
	}

	public double getXSize() {
		return xSize;
	}

	public double getYSize() {
		return ySize;
	}

	public double getZSize() {
		return zSize;
	}

	public List<BlockStateHolder<?>> getCanSpawnOn() {
		return canSpawnOn;
	}

	public List<BlockStateHolder<?>> getCanSpawnIn() {
		return canSpawnIn;
	}

	public boolean isCenteredSpawn() {
		return centeredSpawn;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (obj.getClass() != MobSpawnEntry.class) return false;
		return name.equals(((MobSpawnEntry) obj).name);
	}

	public void serialize(ConfigurationSection map) {
		map.set("mob", mob);
		map.set("singleMobCost", singleMobCost);
		map.set("minPackCost", minPackCost);
		map.set("maxPackCost", maxPackCost);
		map.set("weight", weight);
		map.set("minDistance", minDistance);
		map.set("maxDistance", maxDistance);
		map.set("cooldown", cooldown);
		map.set("despawnRange", despawnRange);
		if (xSize != 0) map.set("xSize", xSize);
		if (ySize != 0) map.set("ySize", ySize);
		if (zSize != 0) map.set("zSize", zSize);
		if (canSpawnOn != null) map.set("canSpawnOn", ConfigUtil.serializeSingleableList(canSpawnOn, BlockStateHolder::getAsString));
		if (canSpawnIn != null) map.set("canSpawnIn", ConfigUtil.serializeSingleableList(canSpawnIn, BlockStateHolder::getAsString));
		map.set("centeredSpawn", centeredSpawn);
	}

	public static MobSpawnEntry deserialize(String name, ConfigurationSection map) {
		String mob = ConfigUtil.requireString(map, "mob");
		int singleMobCost = map.getInt("singleMobCost", 50);
		int minPackCost = map.getInt("minPackCost", 100);
		int maxPackCost = map.getInt("maxPackCost", 300);
		int weight = map.getInt("weight", 10);
		int minDistance = map.getInt("minDistance", 15);
		int maxDistance = map.getInt("maxDistance", 25);
		int cooldown = map.getInt("cooldown", 20);
		int despawnRange = map.getInt("despawnRange", 48);
		double xSize = map.getDouble("xSize", 0);
		double ySize = map.getDouble("ySize", 0);
		double zSize = map.getDouble("zSize",0);
		List<BlockStateHolder<?>> canSpawnOn = ConfigUtil.deserializeSingleableList(map.get("canSpawnOn"), ConfigUtil::parseBlock, () -> null);
		List<BlockStateHolder<?>> canSpawnIn = ConfigUtil.deserializeSingleableList(map.get("canSpawnIn"), ConfigUtil::parseBlock, () -> null);
		boolean centeredSpawn = map.getBoolean("centeredSpawn", false);
		return new MobSpawnEntry(name, mob, singleMobCost, minPackCost, maxPackCost, weight, minDistance, maxDistance, cooldown, despawnRange, xSize, ySize, zSize, canSpawnOn, canSpawnIn, centeredSpawn);
	}
}
