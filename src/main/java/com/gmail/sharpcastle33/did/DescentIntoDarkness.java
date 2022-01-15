package com.gmail.sharpcastle33.did;

import com.comphenix.protocol.reflect.ExactReflection;
import com.gmail.sharpcastle33.did.config.Biomes;
import com.gmail.sharpcastle33.did.config.CaveStyles;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.DataPacks;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.instancing.CaveTrackerManager;
import com.gmail.sharpcastle33.did.listeners.CaveEntranceListener;
import com.gmail.sharpcastle33.did.listeners.CommandListener;
import com.gmail.sharpcastle33.did.listeners.HiddenOre;
import com.gmail.sharpcastle33.did.listeners.MobSpawnManager;
import com.gmail.sharpcastle33.did.listeners.OreListener;
import com.gmail.sharpcastle33.did.listeners.PacketListener;
import com.gmail.sharpcastle33.did.listeners.PlayerListener;
import com.google.common.base.Charsets;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Level;

public class DescentIntoDarkness extends JavaPlugin {

	private CaveTrackerManager caveTrackerManager;
	private MobSpawnManager mobSpawnManager;
	private Scoreboard scoreboard;

	private FileConfiguration config = getConfig();
	private final Map<String, Clipboard> schematics = new HashMap<>();
	private final CaveStyles caveStyles = new CaveStyles();

	public static DescentIntoDarkness instance;
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
		instance = this;
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
		Bukkit.getPluginManager().registerEvents(new CaveEntranceListener(), instance);
		Bukkit.getPluginManager().registerEvents(new OreListener(), instance);
		Bukkit.getPluginManager().registerEvents(new PlayerListener(), instance);
		Bukkit.getPluginManager().registerEvents(new HiddenOre(), instance);
		mobSpawnManager = new MobSpawnManager();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, mobSpawnManager, 0, 1);
		Bukkit.getPluginManager().registerEvents(mobSpawnManager, instance);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, caveTrackerManager::update, 0, 20);

		PacketListener.register();

		HiddenOre.loadHiddenOreData();
	}

	@Override
	public void onDisable() {
		HiddenOre.saveHiddenOreData();
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

		DataPacks.reload();
		Biomes.reload();

		caveStyles.reload(config);

		Bukkit.getLogger().info("Reloaded DescentIntoDarkness config");
	}

	private static final Field CONFIG_FILE_FIELD = ExactReflection.fromClass(JavaPlugin.class, true).getField("configFile");

	@Override
	public void reloadConfig() {
		File configFile;
		try {
			configFile = (File) CONFIG_FILE_FIELD.get(this);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		config = ConfigUtil.loadConfiguration(configFile);

		final InputStream defConfigStream = getResource("config.yml");
		if (defConfigStream == null) {
			return;
		}

		config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
	}

	@NotNull
	@Override
	public FileConfiguration getConfig() {
		if (config == null) {
			reloadConfig();
		}
		return config;
	}

	public int getCaveTimeLimit() {
		return config.getInt("caveTimeLimit", 20 * 60 * 60 * 2); // 2 hours
	}

	public int getCustomBiomeIdStart() {
		return config.getInt("customBiomeIdStart", Biomes.DEFAULT_CUSTOM_BIOME_ID_START);
	}

	public CaveStyles getCaveStyles() {
		return caveStyles;
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
}
