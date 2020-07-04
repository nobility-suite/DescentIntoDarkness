package com.gmail.sharpcastle33.did.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Locale;

public class ConfigUtil {
    public static Material getMaterialByKey(String key) {
        if (key.startsWith(NamespacedKey.MINECRAFT + ":")) {
            key = key.substring(NamespacedKey.MINECRAFT.length() + 1);
        }
        Material mat = Material.getMaterial(key.toUpperCase(Locale.ROOT));
        if (mat == null) {
            throw new InvalidConfigException(NamespacedKey.MINECRAFT + ":" + key);
        }
        return mat;
    }

    public static double parseDouble(String val) {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("Invalid double: " + val);
        }
    }
}
