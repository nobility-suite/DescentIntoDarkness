package com.gmail.sharpcastle33.did.config;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
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
import java.lang.reflect.Modifier;
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
	private static final Object BUILTIN_BIOME_REGISTRY;
	private static final MethodAccessor REGISTRY_GET_RAW_ID;
	private static final MethodAccessor REGISTRY_GET;
	static {
		FuzzyReflection builtinRegistries = FuzzyReflection.fromClass(MinecraftReflection.getMinecraftClass("RegistryGeneration"));
		BUILTIN_BIOME_REGISTRY = Accessors.getFieldAccessor(builtinRegistries.getFieldByName("WORLDGEN_BIOME")).get(null);
		FuzzyReflection registry = FuzzyReflection.fromClass(MinecraftReflection.getMinecraftClass("IRegistry"));
		REGISTRY_GET_RAW_ID = Accessors.getMethodAccessor(registry.getMethod(FuzzyMethodContract.newBuilder()
				.banModifier(Modifier.STATIC)
				.returnTypeExact(int.class)
				.parameterExactArray(Object.class)
				.build()));
		REGISTRY_GET = Accessors.getMethodAccessor(registry.getMethod(FuzzyMethodContract.newBuilder()
				.banModifier(Modifier.STATIC)
				.returnTypeExact(Object.class)
				.parameterExactArray(MinecraftReflection.getMinecraftKeyClass())
				.build()));
	}

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
			String[] parts = biome.split(":", 2);
			Object identifier = MinecraftKey.getConverter().getGeneric(new MinecraftKey(parts[0], parts[1]));
			Object biomeObj = REGISTRY_GET.invoke(BUILTIN_BIOME_REGISTRY, identifier);
			assert biomeObj != null;
			return (Integer) REGISTRY_GET_RAW_ID.invoke(BUILTIN_BIOME_REGISTRY, biomeObj);
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
