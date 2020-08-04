package com.gmail.sharpcastle33.did.config;

import org.bukkit.entity.EntityType;

public class MobSpawnEntry {
	private final String name;
	private final EntityType mob; // TODO: allow mythic mobs type
	private final int pollutionCost;
	private final int weight;
	private final int minDistance;
	private final int maxDistance;
	private final int cooldown;

	public MobSpawnEntry(String name, EntityType mob, int pollutionCost, int weight, int minDistance, int maxDistance, int cooldown) {
		this.name = name;
		this.mob = mob;
		this.pollutionCost = pollutionCost;
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

	public int getPollutionCost() {
		return pollutionCost;
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
}
