package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class Biomes {
	public static final int DEFAULT_CUSTOM_BIOME_ID_START = 256;

	private static final Gson GSON = new Gson();
	private static final Map<String, Integer> biomeRawIds = new HashMap<>();
	private static final Map<String, JsonObject> biomeJsons = new HashMap<>();
	private static byte[] lastSha1 = new byte[20];
	private static final Set<UUID> notifiedPlayers = new HashSet<>();

	public static void reload() {
		Bukkit.getLogger().info("Loading biomes...");
		biomeRawIds.clear();
		biomeJsons.clear();
		int customBiomeIdStart = DescentIntoDarkness.plugin.getCustomBiomeIdStart();
		int biomeCount = 0;

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
		for (String namespace : DataPacks.getNamespaces()) {
			String prefix = "data/" + namespace + "/worldgen/biome/";
			for (String asset : DataPacks.getAssetsUnder(prefix)) {
				String biomeName = asset.substring(prefix.length());
				if (biomeName.endsWith(".json")) {
					biomeName = biomeName.substring(0, biomeName.length() - ".json".length());
					String biomeId = namespace + ":" + biomeName;
					digest.update(biomeId.getBytes(StandardCharsets.UTF_8));
					try (Reader reader = new InputStreamReader(new DigestInputStream(DataPacks.getInputStream(asset), digest), StandardCharsets.UTF_8)) {
						int biomeRawId = customBiomeIdStart + biomeCount;
						biomeJsons.put(biomeId, GSON.fromJson(reader, JsonObject.class));
						biomeRawIds.put(biomeId, biomeRawId);
						biomeCount++;
					} catch (IOException e) {
						Bukkit.getLogger().log(Level.SEVERE, "Failed to load biome " + biomeId, e);
					}
				}
			}
		}

		byte[] sha1 = digest.digest();
		if (!Arrays.equals(sha1, lastSha1)) {
			lastSha1 = sha1;
			for (Player player : Bukkit.getOnlinePlayers()) {
				player.sendMessage(ChatColor.YELLOW + "Reloaded DID config changed biomes, you must relog to apply these effects!");
			}
			notifiedPlayers.clear();
		}

		Bukkit.getLogger().info("Loaded " + biomeCount + " biomes");
	}

	public static String normalize(String biome) {
		if (!biome.contains(":")) {
			return "minecraft:" + biome;
		} else {
			return biome;
		}
	}

	public static boolean biomeExists(String biome) {
		biome = normalize(biome);
		return BiomeTypes.get(biome) != null || biomeJsons.containsKey(biome);
	}

	public static boolean isCustomBiome(String biome) {
		biome = normalize(biome);
		return biomeJsons.containsKey(biome);
	}

	public static int getRawId(String biome) {
		biome = normalize(biome);
		BiomeType biomeType = BiomeTypes.get(biome);
		if (biomeType != null) {
			return biomeType.getInternalId();
		}
		Integer rawId = biomeRawIds.get(biome);
		return rawId == null ? -1 : rawId;
	}

	public static Iterable<String> getCustomBiomes() {
		return biomeJsons.keySet();
	}

	public static JsonObject getBiomeJson(String biome) {
		biome = normalize(biome);
		return biomeJsons.get(biome);
	}

	public static void addNotifiedPlayer(UUID player) {
		notifiedPlayers.add(player);
	}

	public static boolean isPlayerNotified(UUID player) {
		return notifiedPlayers.contains(player);
	}
}
