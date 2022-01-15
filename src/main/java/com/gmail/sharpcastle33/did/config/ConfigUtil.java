package com.gmail.sharpcastle33.did.config;

import com.comphenix.protocol.reflect.ExactReflection;
import com.gmail.sharpcastle33.did.compat.NobilityItems;
import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigUtil {
	public static Object require(ConfigurationSection map, String key) {
		Object value = map.get(key);
		if (value == null) {
			throw new InvalidConfigException("Missing \"" + key + "\"");
		}
		return value;
	}

	public static String requireString(ConfigurationSection map, String key) {
		String value = map.getString(key);
		if (value == null) {
			throw new InvalidConfigException("Missing \"" + key + "\"");
		}
		return value;
	}

	public static BlockPredicate parseBlockPredicate(Object val) {
		if (val instanceof String) {
			BlockStateHolder<?> wantedBlock = parseBlock((String) val);
			return wantedBlock::equalsFuzzy;
		} else if (val instanceof List<?>) {
			List<?> list = (List<?>) val;
			List<BlockStateHolder<?>> blocks = new ArrayList<>();
			for (Object o : list) {
				if (!(o instanceof String)) {
					throw new InvalidConfigException("Invalid block predicate: " + val);
				}
				blocks.add(parseBlock((String) o));
			}
			return block -> blocks.stream().anyMatch(it -> it.equalsFuzzy(block));
		} else if (isConfigurationSection(val)) {
			ConfigurationSection map = asConfigurationSection(val);
			boolean inverted = map.getBoolean("inverted", false);
			BlockPredicate predicate = parseBlockPredicate(require(map, "block"));
			return block -> predicate.test(block) != inverted;
		} else {
			throw new InvalidConfigException("Invalid block predicate: " + val);
		}
	}

	public static BlockProvider parseBlockProvider(Object val) {
		if (val instanceof String) {
			return new BlockProvider.Single(parseBlock((String) val));
		} else if (val instanceof List<?>) {
			return parseWeightedProvider((List<?>) val);
		} else if (isConfigurationSection(val)) {
			ConfigurationSection map = asConfigurationSection(val);
			String type = requireString(map, "type");
			if (type.equalsIgnoreCase("single")) {
				return new BlockProvider.Single(parseBlock(requireString(map, "block")));
			} else if (type.equalsIgnoreCase("weighted")) {
				Object blocks = require(map, "blocks");
				if (!(blocks instanceof List<?>)) {
					throw new InvalidConfigException("Block provider type \"weighted\" requires a list of blocks");
				}
				return parseWeightedProvider((List<?>) blocks);
			} else if (type.equalsIgnoreCase("roomWeighted")) {
				Object blocks = require(map, "blocks");
				if (!(blocks instanceof List<?>)) {
					throw new InvalidConfigException("Block provider type \"roomWeighted\" requires a list of blocks");
				}
				List<BlockProvider> providers = new ArrayList<>();
				List<Integer> weights = new ArrayList<>();
				for (Object block : (List<?>) blocks) {
					BlockProvider provider = parseBlockProvider(block);
					int weight = isConfigurationSection(block) ? asConfigurationSection(block).getInt("weight", 1) : 1;
					if (weight <= 0) {
						throw new InvalidConfigException("Invalid weight: " + weight);
					}
					providers.add(provider);
					weights.add(weight);
				}
				return new BlockProvider.RoomWeighted(providers.toArray(new BlockProvider[0]), ArrayUtils.toPrimitive(weights.toArray(new Integer[0])));
			} else {
				throw new InvalidConfigException("Unknown block provider type: " + type);
			}
		} else {
			throw new InvalidConfigException("Invalid block provider: " + val);
		}
	}

	private static BlockProvider parseWeightedProvider(List<?> list) {
		if (list.isEmpty()) {
			throw new InvalidConfigException("Block provider list is empty");
		}
		List<BlockStateHolder<?>> blocks = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof String) {
				blocks.add(parseBlock((String) o));
				weights.add(1);
			} else if (isConfigurationSection(o)) {
				ConfigurationSection map = asConfigurationSection(o);
				BlockStateHolder<?> block = parseBlock(requireString(map, "block"));
				int weight = map.getInt("weight", 1);
				if (weight <= 0) {
					throw new InvalidConfigException("Invalid weight: " + weight);
				}
				blocks.add(block);
				weights.add(weight);
			}
		}
		return new BlockProvider.Weighted(blocks.toArray(new BlockStateHolder[0]), ArrayUtils.toPrimitive(weights.toArray(new Integer[0])));
	}

	public static BlockStateHolder<?> parseBlock(String val) {
		if (val.startsWith("nobility:")) {
			val = val.substring("nobility:".length());
			BlockData blockData = NobilityItems.getNobilityBlock(val);
			if (blockData == null) {
				throw new InvalidConfigException("Invalid NobilityBlock: " + val);
			}
			return BukkitAdapter.adapt(blockData);
		}
		try {
			ParserContext context = new ParserContext();
			context.setRestricted(false);
			context.setPreferringWildcard(true);
			return WorldEdit.getInstance().getBlockFactory().parseFromInput(val, context);
		} catch (InputParseException e) {
			throw new InvalidConfigException("Invalid block state: " + val, e);
		}
	}

	public static String serializeBlock(BlockStateHolder<?> block) {
		String nobilityName = NobilityItems.getNobilityName(BukkitAdapter.adapt(block));
		if (nobilityName != null) {
			return "nobility:" + nobilityName;
		}
		return block.getAsString();
	}

	@SuppressWarnings("unchecked")
	public static ItemStack parseItem(Object val) {
		if (val instanceof String) {
			String typeAndCount = (String) val;
			int starIndex = typeAndCount.indexOf('*');
			int count = starIndex == -1 ? 1 : parseInt(typeAndCount.substring(0, starIndex));
			String originalItemName = starIndex == -1 ? typeAndCount : typeAndCount.substring(starIndex + 1);
			String itemName = originalItemName;
			ItemStack nobilityStack = null;
			if (itemName.startsWith("minecraft:")) {
				itemName = itemName.substring("minecraft:".length());
			} else if (itemName.startsWith("nobility:")) {
				itemName = itemName.substring("nobility:".length());
				nobilityStack = NobilityItems.createNobilityStack(itemName, count);
				if (nobilityStack == null) {
					throw new InvalidConfigException("Unknown NobilityItem: " + itemName);
				}
			}
			itemName = itemName.toUpperCase(Locale.ROOT);
			Material material = nobilityStack != null ? nobilityStack.getType() : Material.getMaterial(itemName);
			if (material == null || !material.isItem()) {
				throw new InvalidConfigException("Unknown item: " + originalItemName);
			}
			if (count < 1 || count > material.getMaxStackSize()) {
				throw new InvalidConfigException("Cannot have a stack size of " + count + " for item " + originalItemName);
			}
			return nobilityStack != null ? nobilityStack : new ItemStack(material, count);
		} else if (val instanceof Map) {
			return ItemStack.deserialize((Map<String, Object>) val);
		} else if (val instanceof ConfigurationSection) {
			return ItemStack.deserialize(((ConfigurationSection) val).getValues(false));
		} else {
			throw new InvalidConfigException("Don't know how to turn a " + val.getClass().getSimpleName() + " into an ItemStack");
		}
	}

	public static int parseInt(String val) {
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			throw new InvalidConfigException("Invalid integer: " + val);
		}
	}

	public static double parseDouble(String val) {
		try {
			return Double.parseDouble(val);
		} catch (NumberFormatException e) {
			throw new InvalidConfigException("Invalid double: " + val);
		}
	}

	public static boolean isConfigurationSection(Object val) {
		return val instanceof ConfigurationSection || val instanceof Map;
	}

	public static ConfigurationSection asConfigurationSection(Object val) {
		if (val instanceof ConfigurationSection) {
			return (ConfigurationSection) val;
		} else if (val instanceof Map) {
			MemoryConfiguration config = new MemoryConfiguration();
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) val).entrySet()) {
				config.set(entry.getKey().toString(), entry.getValue());
			}
			return config;
		}
		throw new InvalidConfigException("Not a ConfigurationSection: " + val);
	}

	public static <T extends Enum<T>> T parseEnum(Class<T> type, String val) {
		T result = tryParseEnum(type, val);
		if (result == null) {
			throw new InvalidConfigException("Invalid " + type.getSimpleName() + ": " + val);
		}
		return result;
	}

	@Nullable
	public static <T extends Enum<T>> T tryParseEnum(Class<T> type, String val) {
		for (T enumVal : type.getEnumConstants()) {
			if (enumVal.name().equals(val.toUpperCase(Locale.ROOT))) {
				return enumVal;
			}
		}
		return null;
	}

	public static <T> List<T> deserializeSingleableList(Object val, Function<String, T> parseFunction, Supplier<List<T>> defaultSupplier) {
		if (val == null) {
			return defaultSupplier.get();
		} else if (val instanceof List) {
			return ((List<?>) val).stream().map(v -> parseFunction.apply(v.toString())).collect(Collectors.toCollection(ArrayList::new));
		} else {
			return Lists.newArrayList(parseFunction.apply(val.toString()));
		}
	}

	private static final Field YAML_FIELD = ExactReflection.fromClass(YamlConfiguration.class, true).getField("yaml");
	private static final Field LOADING_CONFIG_FIELD = ExactReflection.fromClass(Yaml.class, true).getField("loadingConfig");

	public static YamlConfiguration loadConfiguration(File file) {
		YamlConfiguration config = new YamlConfiguration();

		LoaderOptions loadingConfig;
		try {
			Yaml yaml = (Yaml) YAML_FIELD.get(config);
			loadingConfig = (LoaderOptions) LOADING_CONFIG_FIELD.get(yaml);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		loadingConfig.setAllowDuplicateKeys(false);

		try {
			config.load(file);
		} catch (FileNotFoundException ignore) {
		} catch (IOException | InvalidConfigurationException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, e);
		}

		return config;
	}
}
