package com.gmail.sharpcastle33.did;

import com.gmail.sharpcastle33.did.config.Biomes;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.DataPacks;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.instancing.CaveTrackerManager;
import com.gmail.sharpcastle33.did.listeners.CaveEntranceListener;
import com.gmail.sharpcastle33.did.listeners.CommandListener;
import com.gmail.sharpcastle33.did.listeners.MobSpawnManager;
import com.gmail.sharpcastle33.did.listeners.OreListener;
import com.gmail.sharpcastle33.did.listeners.PacketListener;
import com.gmail.sharpcastle33.did.listeners.PlayerListener;
import com.onarandombox.MultiverseCore.api.Core;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DescentIntoDarkness extends JavaPlugin {

	private CaveTrackerManager caveTrackerManager;
	private MobSpawnManager mobSpawnManager;
	private Scoreboard scoreboard;

	private FileConfiguration config = getConfig();
	private LinkedHashMap<String, Integer> caveStyleWeights = null;
	private Configuration caveStylesConfig;
	private NavigableMap<String, CaveStyle> caveStyles = null;
	private final Map<String, Clipboard> schematics = new HashMap<>();

	public static DescentIntoDarkness plugin;
	public static Core multiverseCore;

	@Override
	public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
		if (id == null) {
			return null;
		}
		if (id.startsWith("full_")) {
			id = id.substring(5);
			BlockStateHolder<?> state;
			try {
				state = ConfigUtil.parseBlock(id);
			} catch (InvalidConfigException e) {
				return null;
			}
			BlockData data = BukkitAdapter.adapt(state);
			return new ChunkGenerator() {
				@Override
				public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
					ChunkData chunkData = createChunkData(world);
					chunkData.setRegion(0, 0, 0, 16, 1, 16, Material.BEDROCK);
					chunkData.setRegion(0, 1, 0, 16, 255, 16, data);
					chunkData.setRegion(0, 255, 0, 16, 256, 16, Material.BEDROCK);
					return chunkData;
				}

				@Override
				public boolean isParallelCapable() {
					return true;
				}
			};
		}

		return null;
	}

	@Override
	public void onEnable() {
		plugin = this;
		multiverseCore = (Core) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
		if (multiverseCore == null) {
			throw new RuntimeException("DescentIntoDarkness depends on Multiverse-Core, which was not found");
		}

		setupConfig();

		if (caveTrackerManager != null) {
			caveTrackerManager.destroy();
		}
		int instanceLimit = config.getInt("instanceLimit", 8);
		if (instanceLimit <= 0) {
			instanceLimit = 8;
		}
		caveTrackerManager = new CaveTrackerManager(instanceLimit);
		registerCommand("did", new CommandListener());
		Bukkit.getPluginManager().registerEvents(new CaveEntranceListener(), plugin);
		Bukkit.getPluginManager().registerEvents(new OreListener(), plugin);
		Bukkit.getPluginManager().registerEvents(new PlayerListener(), plugin);
		mobSpawnManager = new MobSpawnManager();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, mobSpawnManager, 0, 1);
		Bukkit.getPluginManager().registerEvents(mobSpawnManager, plugin);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, caveTrackerManager::update, 0, 20);

		PacketListener.register();
	}

	@Override
	public void onDisable() {
		caveTrackerManager.destroy();
	}

	private <T extends CommandExecutor & TabCompleter> void registerCommand(String name, T executor) {
		PluginCommand command = getCommand(name);
		if (command == null) {
			throw new IllegalStateException("Cannot find command: \"" + name + "\"");
		}
		command.setExecutor(executor);
		command.setTabCompleter(executor);
	}

	private void setupConfig() {
		config.addDefault("instanceLimit", 8);
		config.addDefault("caveTimeLimit", 20 * 60 * 60 * 2); // 2 hours
		if (!config.contains("caveStyles")) {
			config.addDefault("caveStyles.default", 10);
		}
		config.addDefault("customBiomeIdStart", Biomes.DEFAULT_CUSTOM_BIOME_ID_START);
		config.options().copyDefaults(true);
		saveConfig();
		reload();
	}

	public void reload() {
		schematics.clear();
		reloadConfig();
		config = getConfig();

		DataPacks.reload();
		Biomes.reload();

		caveStyleWeights = new LinkedHashMap<>();
		ConfigurationSection caveStylesSection = config.getConfigurationSection("caveStyles");
		if (caveStylesSection != null) {
			for (String style : caveStylesSection.getKeys(false)) {
				caveStyleWeights.put(style, caveStylesSection.getInt(style, 10));
			}
		}

		Bukkit.getLogger().info("Loading cave styles...");
		File caveStylesDir = new File(getDataFolder(), "caveStyles");
		//noinspection ResultOfMethodCallIgnored
		caveStylesDir.mkdirs();

		try {
			Files.copy(Objects.requireNonNull(getResource("defaultCaveStyles.yml")), new File(caveStylesDir, "default.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to write default cave style", e);
		}

		List<File> caveStyleFiles;
		try {
			caveStyleFiles = Files.walk(caveStylesDir.toPath()).map(Path::toFile).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		caveStylesConfig = new MemoryConfiguration();
		for (File caveStyleFile : caveStyleFiles) {
			YamlConfiguration localConfig = YamlConfiguration.loadConfiguration(caveStyleFile);
			if (localConfig.getKeys(false).stream().anyMatch(caveStylesConfig::contains)) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed to load config file " + caveStyleFile.getName() + " because it contains keys already present in a previously loaded file");
			} else {
				localConfig.getValues(true).forEach(caveStylesConfig::set);
			}
		}
		this.caveStyles = null;
		getCaveStyles();

		Bukkit.getLogger().info("Reloaded DescentIntoDarkness config");
	}

	private FileConfiguration reloadConfig(String configName) {
		File configFile = new File(getDataFolder(), configName + ".yml");
		return YamlConfiguration.loadConfiguration(configFile);
	}

	private void saveConfig(String configName, FileConfiguration config) {
		File configFile = new File(getDataFolder(), configName + ".yml");
		try {
			config.save(configFile);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Could not save " + configName + ".yml", e);
		}
	}

	public int getCaveTimeLimit() {
		return config.getInt("caveTimeLimit", 20 * 60 * 60 * 2); // 2 hours
	}

	public int getCustomBiomeIdStart() {
		return config.getInt("customBiomeIdStart", Biomes.DEFAULT_CUSTOM_BIOME_ID_START);
	}

	public LinkedHashMap<String, Integer> getCaveStyleWeights() {
		return caveStyleWeights;
	}

	public NavigableMap<String, CaveStyle> getCaveStyles() {
		if (caveStyles == null) {
			caveStyles = new TreeMap<>();

			try {
				Set<String> styleStack = new HashSet<>();
				Set<String> processedStyles = new HashSet<>();
				for (String styleName : caveStylesConfig.getKeys(false)) {
					inlineCaveStyleInheritance(styleName, styleStack, processedStyles);
				}
			} catch (InvalidConfigException e) {
				getLogger().log(Level.SEVERE, "Failed to load cave styles", e);
				return caveStyles;
			}

			for (String styleName : caveStylesConfig.getKeys(false)) {
				try {
					ConfigurationSection value = caveStylesConfig.getConfigurationSection(styleName);
					if (value == null) {
						throw new InvalidConfigException("Cave style \"" + styleName + "\" has invalid type");
					}
					caveStyles.put(styleName, CaveStyle.deserialize(styleName, value));
				} catch (InvalidConfigException e) {
					getLogger().log(Level.SEVERE, "Failed to load cave style " + styleName, e);
				}
			}
		}
		return caveStyles;
	}

	private void inlineCaveStyleInheritance(String styleName, Set<String> styleStack, Set<String> processedStyles) {
		if (styleStack.contains(styleName)) {
			throw new InvalidConfigException("Detected cyclic cave style inheritance");
		}
		if (processedStyles.contains(styleName)) {
			return;
		}
		styleStack.add(styleName);
		processedStyles.add(styleName);

		ConfigurationSection caveStyle = caveStylesConfig.getConfigurationSection(styleName);
		if (caveStyle == null) {
			throw new InvalidConfigException("Tried to inherit from cave style \"" + styleName + "\" which does not exist");
		}

		class InheritanceData {
			final String name;
			final Set<String> mergeTop = new HashSet<>();
			final Set<String> mergeBottom = new HashSet<>();

			InheritanceData(String name) {
				this.name = name;
			}
		}
		List<InheritanceData> parents = new ArrayList<>();
		List<?> inheritList = caveStyle.getList("inherit");
		if (inheritList != null) {
			for (Object parent : inheritList) {
				if (parent instanceof Map || parent instanceof ConfigurationSection) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = parent instanceof Map ? (Map<String, Object>) parent : ((ConfigurationSection) parent).getValues(false);
					Object name = map.get("name");
					if (name == null) {
						throw new InvalidConfigException("Complex inherit must have a \"name\"");
					}
					InheritanceData data = new InheritanceData(name.toString());
					Object merge = map.get("merge");
					if (merge instanceof Map || merge instanceof ConfigurationSection) {
						@SuppressWarnings("unchecked")
						Map<String, Object> mergeMap = merge instanceof Map ? (Map<String, Object>) merge : ((ConfigurationSection) merge).getValues(false);
						mergeMap.forEach((key, val) -> {
							String strVal = String.valueOf(val);
							if (strVal.equals("top")) {
								data.mergeTop.add(key);
							} else if (strVal.equals("bottom")) {
								data.mergeBottom.add(key);
							} else {
								throw new InvalidConfigException("Complex inherit merge must be either \"top\" or \"bottom\"");
							}
						});
					}
					parents.add(data);
				} else {
					parents.add(new InheritanceData(parent.toString()));
				}
			}
		}

		if (!caveStyle.contains("__builtin_no_default_inherit") && parents.stream().noneMatch(it -> it.name.equals("default"))) {
			parents.add(new InheritanceData("default"));
		}
		for (InheritanceData parent : parents) {
			inlineCaveStyleInheritance(parent.name, styleStack, processedStyles);
			ConfigurationSection parentStyle = caveStylesConfig.getConfigurationSection(parent.name);
			assert parentStyle != null;
			parentStyle.getValues(false).forEach((key, val) -> {
				if (!"inherit".equals(key) && !"abstract".equals(key) && !"__builtin_no_default_inherit".equals(key)) {
					if (caveStyle.contains(key)) {
						if (parent.mergeTop.contains(key)) {
							Object ourVal = caveStyle.get(key);
							if (val instanceof List) {
								if (!(ourVal instanceof List)) {
									throw new InvalidConfigException("Cannot merge mismatching types under section \"" + key + "\"");
								}
								//noinspection unchecked
								((List<Object>) ourVal).addAll((List<Object>) val);
							} else if (val instanceof Map || val instanceof ConfigurationSection) {
								if (!(ourVal instanceof Map) && !(ourVal instanceof ConfigurationSection)) {
									throw new InvalidConfigException("Cannot merge mismatching types under section \"" + key + "\"");
								}
								@SuppressWarnings("unchecked")
								Map<String, Object> parentVal = val instanceof Map ? (Map<String, Object>) val : ((ConfigurationSection) val).getValues(false);
								if (ourVal instanceof Map) {
									//noinspection unchecked
									((Map<String, Object>) ourVal).putAll(parentVal);
								} else {
									parentVal.forEach(((ConfigurationSection) ourVal)::set);
								}
							} else {
								throw new InvalidConfigException("Cannot merge type under section \"" + key + "\"");
							}
						} else if (parent.mergeBottom.contains(key)) {
							Object ourVal = caveStyle.get(key);
							if (val instanceof List) {
								if (!(ourVal instanceof List)) {
									throw new InvalidConfigException("Cannot merge mismatching types under section \"" + key + "\"");
								}
								@SuppressWarnings("unchecked")
								List<Object> newVal = new ArrayList<>((List<Object>) val);
								//noinspection unchecked
								newVal.addAll((List<Object>) ourVal);
								caveStyle.set(key, newVal);
							} else if (val instanceof Map || val instanceof ConfigurationSection) {
								if (!(ourVal instanceof Map) && !(ourVal instanceof ConfigurationSection)) {
									throw new InvalidConfigException("Cannot merge mismatching types under section \"" + key + "\"");
								}
								@SuppressWarnings("unchecked")
								Map<String, Object> parentVal = val instanceof Map ? (Map<String, Object>) val : ((ConfigurationSection) val).getValues(false);
								@SuppressWarnings("unchecked")
								Map<String, Object> ourMap = ourVal instanceof Map ? (Map<String, Object>) ourVal : ((ConfigurationSection) ourVal).getValues(false);
								Map<String, Object> newMap = new LinkedHashMap<>(parentVal);
								newMap.putAll(ourMap);
								caveStyle.set(key, newMap);
							} else {
								throw new InvalidConfigException("Cannot merge type under section \"" + key + "\"");
							}
						}
					} else {
						// copy if necessary
						Object newVal = val;
						if (val instanceof List) {
							newVal = new ArrayList<>((List<?>) val);
						} else if (val instanceof Map) {
							newVal = new LinkedHashMap<>((Map<?, ?>) val);
						} else if (val instanceof ConfigurationSection) {
							newVal = new LinkedHashMap<>(((ConfigurationSection) val).getValues(false));
						}
						caveStyle.set(key, newVal);
					}
				}
			});
		}

		styleStack.remove(styleName);
	}

	public Clipboard getSchematic(String name) {
		Clipboard schematic = schematics.get(name);
		if (schematic != null) {
			return schematic;
		}
		File schemDir = new File(getDataFolder(), "schematics");
		if (!schemDir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			schemDir.mkdirs();
		}

		File schemFile = new File(schemDir, name + ".schem");
		if (!schemFile.exists()) {
			return null;
		}

		ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
		if (format == null) {
			return null;
		}
		try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
			schematic = reader.read();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		schematics.put(name, schematic);
		return schematic;
	}

	public CaveTrackerManager getCaveTrackerManager() {
		return caveTrackerManager;
	}

	public MobSpawnManager getMobSpawnManager() {
		return mobSpawnManager;
	}

	public Scoreboard getScoreboard() {
		if (scoreboard == null) {
			ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
			if (scoreboardManager == null) {
				throw new IllegalStateException("Accessed DescentIntoDarkness.getScoreboard too early");
			}
			scoreboard = scoreboardManager.getMainScoreboard();
		}
		return scoreboard;
	}

	public void runSyncNow(Runnable task) {
		try {
			runSyncLater(task).get();
		} catch (InterruptedException | ExecutionException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Exception occurred when running synchronous task", e);
		}
	}

	public <T> T supplySyncNow(Supplier<T> task) {
		if (Bukkit.isPrimaryThread()) {
			Bukkit.getLogger().log(Level.SEVERE, "Calling supplySyncNow from main thread, causing a deadlock!");
			Thread.dumpStack();
		}
		try {
			return supplySyncLater(task).get();
		} catch (InterruptedException | ExecutionException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Exception occurred when running synchronous task", e);
			throw new RuntimeException(e);
		}
	}

	public CompletableFuture<Void> runSyncLater(Runnable task) {
		return supplySyncLater(() -> {
			task.run();
			return null;
		});
	}

	public <T> CompletableFuture<T> supplySyncLater(Supplier<T> task) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTask(this, () -> {
			T result;
			try {
				result = task.get();
			} catch (Throwable t) {
				future.completeExceptionally(t);
				return;
			}
			future.complete(result);
		});
		return future;
	}

	public CompletableFuture<Void> runAsync(Runnable task) {
		return supplyAsync(() -> {
			task.run();
			return null;
		});
	}

	public <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
		CompletableFuture<T> future = new CompletableFuture<>();
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			T result;
			try {
				result = task.get();
			} catch (Throwable t) {
				future.completeExceptionally(t);
				return;
			}
			future.complete(result);
		});
		return future;
	}

	private static List<Material> ALL_MATERIALS;

	public static List<Material> getAllMaterials() {
		// I can't find a better way of doing this
		if (ALL_MATERIALS == null) {
			ALL_MATERIALS = new ArrayList<>();
			for (Material material : Material.values()) {
				if (!material.name().startsWith("LEGACY_")) {
					ALL_MATERIALS.add(material);
				}
			}
		}
		return ALL_MATERIALS;
	}
}
