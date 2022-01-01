package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CaveStyles {
	private Weights caveStyleWeights = null;
	private Configuration caveStylesConfig;
	private NavigableMap<String, CaveStyle> caveStyles = null;

	public void reload(ConfigurationSection config) {
		caveStyleWeights = new Weights();
		ConfigurationSection caveStylesSection = config.getConfigurationSection("caveStyles");
		if (caveStylesSection != null) {
			for (String style : caveStylesSection.getKeys(false)) {
				caveStyleWeights.weights.put(style, caveStylesSection.getInt(style, 10));
			}
		}

		Bukkit.getLogger().info("Loading cave styles...");
		File caveStylesDir = new File(DescentIntoDarkness.instance.getDataFolder(), "caveStyles");
		//noinspection ResultOfMethodCallIgnored
		caveStylesDir.mkdirs();

		try {
			Files.copy(Objects.requireNonNull(DescentIntoDarkness.instance.getResource("defaultCaveStyles.yml")), new File(caveStylesDir, "default.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed to write default cave style", e);
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
			YamlConfiguration localConfig = ConfigUtil.loadConfiguration(caveStyleFile);
			if (localConfig.getKeys(false).stream().anyMatch(caveStylesConfig::contains)) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed to load config file " + caveStyleFile.getName() + " because it contains keys already present in a previously loaded file");
			} else {
				localConfig.getValues(true).forEach(caveStylesConfig::set);
			}
		}
		this.caveStyles = null;
		getCaveStylesByName();
	}

	public Weights getWeights() {
		return caveStyleWeights;
	}

	public NavigableMap<String, CaveStyle> getCaveStylesByName() {
		if (caveStyles == null) {
			caveStyles = new TreeMap<>();

			try {
				Set<String> styleStack = new HashSet<>();
				Set<String> processedStyles = new HashSet<>();
				for (String styleName : caveStylesConfig.getKeys(false)) {
					inlineCaveStyleInheritance(styleName, styleStack, processedStyles);
				}
			} catch (InvalidConfigException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed to load cave styles", e);
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
					Bukkit.getLogger().log(Level.SEVERE, "Failed to load cave style " + styleName, e);
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
				if (ConfigUtil.isConfigurationSection(parent)) {
					ConfigurationSection map = ConfigUtil.asConfigurationSection(parent);
					String name = ConfigUtil.requireString(map, "name");
					InheritanceData data = new InheritanceData(name);
					ConfigurationSection mergeMap = map.getConfigurationSection("merge");
					if (mergeMap != null) {
						for (String key : mergeMap.getKeys(false)) {
							String val = mergeMap.getString(key);
							if ("top".equals(val)) {
								data.mergeTop.add(key);
							} else if ("bottom".equals(val)) {
								data.mergeBottom.add(key);
							} else {
								throw new InvalidConfigException("Complex inherit merge must be either \"top\" or \"bottom\"");
							}
						}
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
							} else if (ConfigUtil.isConfigurationSection(val)) {
								if (!ConfigUtil.isConfigurationSection(ourVal)) {
									throw new InvalidConfigException("Cannot merge mismatching types under section \"" + key + "\"");
								}
								ConfigurationSection parentVal = ConfigUtil.asConfigurationSection(val);
								if (ourVal instanceof Map) {
									for (String parentValKey : parentVal.getKeys(false)) {
										//noinspection unchecked
										((Map<String, Object>) ourVal).put(parentValKey, parentVal.get(parentValKey));
									}
								} else {
									for (String parentValKey : parentVal.getKeys(false)) {
										((ConfigurationSection) ourVal).set(parentValKey, parentVal.get(parentValKey));
									}
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
							} else if (ConfigUtil.isConfigurationSection(val)) {
								if (!ConfigUtil.isConfigurationSection(ourVal)) {
									throw new InvalidConfigException("Cannot merge mismatching types under section \"" + key + "\"");
								}
								@SuppressWarnings("unchecked")
								Map<String, Object> parentVal = val instanceof Map ? (Map<String, Object>) val : ((ConfigurationSection) val).getValues(false);
								@SuppressWarnings("unchecked")
								Map<String, Object> ourMap = ourVal instanceof Map ? (Map<String, Object>) ourVal : ((ConfigurationSection) ourVal).getValues(false);
								Map<String, Object> newMap = new LinkedHashMap<>(parentVal);
								newMap.putAll(ourMap);
								caveStyle.set(key, null);
								caveStyle.createSection(key, newMap);
							} else {
								throw new InvalidConfigException("Cannot merge type under section \"" + key + "\"");
							}
						}
					} else {
						// copy if necessary
						if (val instanceof List) {
							caveStyle.set(key, new ArrayList<>((List<?>) val));
						} else if (val instanceof Map) {
							caveStyle.createSection(key, (Map<?, ?>) val);
						} else if (val instanceof ConfigurationSection) {
							caveStyle.createSection(key, ((ConfigurationSection) val).getValues(false));
						} else {
							caveStyle.set(key, val);
						}
					}
				}
			});
		}

		styleStack.remove(styleName);
	}

	public static class Weights {
		private final LinkedHashMap<String, Integer> weights = new LinkedHashMap<>();

		@Nullable
		public String getRandom(Random rand) {
			int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
			int randVal = rand.nextInt(totalWeight);
			for (Map.Entry<String, Integer> entry : weights.entrySet()) {
				randVal -= entry.getValue();
				if (randVal < 0) {
					return entry.getKey();
				}
			}

			return null;
		}

		public void remove(String style) {
			weights.remove(style);
		}
	}
}
