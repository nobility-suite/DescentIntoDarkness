package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import org.bukkit.configuration.ConfigurationSection;

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
	private final BlockPredicate canSpawnOn;
	private final BlockPredicate canSpawnIn;
	private final boolean centeredSpawn;
	private final boolean randomRotation;

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
			BlockPredicate canSpawnOn,
			BlockPredicate canSpawnIn,
			boolean centeredSpawn,
			boolean randomRotation) {
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
		this.randomRotation = randomRotation;
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

	public BlockPredicate getCanSpawnOn() {
		return canSpawnOn;
	}

	public BlockPredicate getCanSpawnIn() {
		return canSpawnIn;
	}

	public boolean isCenteredSpawn() {
		return centeredSpawn;
	}

	public boolean isRandomRotation() {
		return randomRotation;
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
		BlockPredicate canSpawnOn = map.contains("canSpawnOn") ? ConfigUtil.parseBlockPredicate(map.get("canSpawnOn")) : block -> block.getMaterial().isSolid();
		BlockPredicate canSpawnIn = map.contains("canSpawnIn") ? ConfigUtil.parseBlockPredicate(map.get("canSpawnIn")) : block -> !block.getMaterial().isMovementBlocker() && !block.getMaterial().isLiquid();
		boolean centeredSpawn = map.getBoolean("centeredSpawn", false);
		boolean randomRotation = map.getBoolean("randomRotation", true);
		return new MobSpawnEntry(name, mob, singleMobCost, minPackCost, maxPackCost, weight, minDistance, maxDistance, cooldown, despawnRange, xSize, ySize, zSize, canSpawnOn, canSpawnIn, centeredSpawn, randomRotation);
	}
}
