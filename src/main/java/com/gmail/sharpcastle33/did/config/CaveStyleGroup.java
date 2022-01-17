package com.gmail.sharpcastle33.did.config;

import org.bukkit.configuration.ConfigurationSection;

public class CaveStyleGroup {
	private final int groupWeight;
	private final CaveStyles.Weights caveWeights = new CaveStyles.Weights();

	public CaveStyleGroup(ConfigurationSection section) {
		this.groupWeight = section.getInt("weight", 1);
		ConfigurationSection caves = section.getConfigurationSection("caves");
		if (caves != null) {
			for (String key : caves.getKeys(false)) {
				int value = caves.getInt(key);
				if (value < 1) {
					throw new InvalidConfigException("Invalid weight for " + key);
				}
				caveWeights.put(key, value);
			}
		}
	}

	public int getGroupWeight() {
		return groupWeight;
	}

	public CaveStyles.Weights getCaveWeights() {
		return caveWeights;
	}
}
