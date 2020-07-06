package com.gmail.sharpcastle33.did;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.PainterStep;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.sharpcastle33.did.listeners.CommandListener;
import com.gmail.sharpcastle33.did.listeners.OreListener;
import com.gmail.sharpcastle33.dungeonmaster.DungeonMaster;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {

	private DungeonMaster dungeonMaster;

	private FileConfiguration config = getConfig();
	private FileConfiguration caveStylesConfig;
	private Map<String, CaveStyle> caveStyles = null;

	public static Main plugin;
	
	@Override
	public void onEnable() {
		plugin = this;

		setupConfig();

		dungeonMaster = new DungeonMaster();
		findCommand("did").setExecutor(new CommandListener());
		Bukkit.getPluginManager().registerEvents(new OreListener(), plugin);
	}

	private PluginCommand findCommand(String name) {
		PluginCommand command = getCommand(name);
		if (command == null) {
			throw new IllegalStateException("Cannot find command: \"" + name + "\"");
		}
		return command;
	}

	private void setupConfig() {
		//config.addDefault("caveStyles", Lists.newArrayList("default"));
		config.options().copyDefaults(true);
		saveConfig();

		caveStylesConfig = reloadConfig("caveStyles");
		CaveStyle defaultStyle = new CaveStyle();
		defaultStyle.setPainterSteps(Lists.newArrayList(
				new PainterStep.ReplaceFloor(Material.STONE, Material.GRAVEL),
				new PainterStep.ChanceReplace(Material.STONE, Material.ANDESITE, 0.2),
				new PainterStep.ChanceReplace(Material.STONE, Material.COBBLESTONE, 0.2),
				new PainterStep.ChanceReplace(Material.STONE, Material.MOSSY_COBBLESTONE, 0.05)
		));
        ConfigurationSection defaultConfig = new MemoryConfiguration();
		defaultStyle.serialize(defaultConfig);
		caveStylesConfig.addDefaults(defaultConfig.getValues(false).entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>("default." + entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (a, b) -> a, (Supplier<HashMap<String, Object>>)HashMap::new)));
		caveStylesConfig.options().copyDefaults(true);
		saveConfig("caveStyles", caveStylesConfig);
		getCaveStyles(); // for error messages TODO: load this lazily?
	}

	public void reload() {
		reloadConfig();
		caveStylesConfig = reloadConfig("caveStyles");
		caveStyles = null;
		getCaveStyles(); // for error messages TODO: reload this lazily?
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

	public Map<String, CaveStyle> getCaveStyles() {
		if (caveStyles == null) {
			caveStyles = new HashMap<>();
			for (String styleName : caveStylesConfig.getKeys(false)) {
				try {
                    ConfigurationSection value = caveStylesConfig.getConfigurationSection(styleName);
                    if (value == null) {
                        throw new InvalidConfigException("Cave style \"" + styleName + "\" has invalid type");
                    }
					caveStyles.put(styleName, CaveStyle.deserialize(value));
				} catch (InvalidConfigException e) {
					getLogger().log(Level.SEVERE, "Failed to load cave style " + styleName, e);
				}
			}
		}
		return caveStyles;
	}
	
	public DungeonMaster getDungeonMaster() {
		return this.dungeonMaster;
	}
}
