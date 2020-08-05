package com.gmail.sharpcastle33.did.config;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public final class MobSpawnEntry {
	private final String name;
	private final EntityType mob; // TODO: allow mythic mobs type
	private final int singleMobCost;
	private final int minPackCost;
	private final int maxPackCost;
	private final int weight;
	private final int minDistance;
	private final int maxDistance;
	private final int cooldown;

	public MobSpawnEntry(
			String name,
			EntityType mob,
			int singleMobCost,
			int minPackCost,
			int maxPackCost,
			int weight,
			int minDistance,
			int maxDistance,
			int cooldown
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
	}

	public String getName() {
		return name;
	}

	public EntityType getMob() {
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
		map.set("mob", mob.getKey().toString());
		map.set("singleMobCost", singleMobCost);
		map.set("minPackCost", minPackCost);
		map.set("maxPackCost", maxPackCost);
		map.set("weight", weight);
		map.set("minDistance", minDistance);
		map.set("maxDistance", maxDistance);
		map.set("cooldown", cooldown);
	}

	public static MobSpawnEntry deserialize(String name, ConfigurationSection map) {
		EntityType mob = BukkitAdapter.adapt(com.sk89q.worldedit.world.entity.EntityType.REGISTRY.get(ConfigUtil.requireString(map, "mob")));
		int singleMobCost = map.getInt("singleMobCost", 50);
		int minPackCost = map.getInt("minPackCost", 100);
		int maxPackCost = map.getInt("maxPackCost", 300);
		int weight = map.getInt("weight", 10);
		int minDistance = map.getInt("minDistance", 15);
		int maxDistance = map.getInt("maxDistance", 25);
		int cooldown = map.getInt("cooldown", 20);
		return new MobSpawnEntry(name, mob, singleMobCost, minPackCost, maxPackCost, weight, minDistance, maxDistance, cooldown);
	}
}
