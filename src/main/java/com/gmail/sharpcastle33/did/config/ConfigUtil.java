package com.gmail.sharpcastle33.did.config;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

public class ConfigUtil {
	public static BlockData parseBlockData(String val) {
		try {
			return Bukkit.getServer().createBlockData(val);
		} catch (IllegalArgumentException e) {
			throw new InvalidConfigException("Invalid block state: " + val);
		}
	}

	public static double parseDouble(String val) {
		try {
			return Double.parseDouble(val);
		} catch (NumberFormatException e) {
			throw new InvalidConfigException("Invalid double: " + val);
		}
	}
}
