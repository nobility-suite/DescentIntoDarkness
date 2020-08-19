package com.gmail.sharpcastle33.did.config;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
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

	public static BlockStateHolder<?> parseBlock(String val) {
		try {
			ParserContext context = new ParserContext();
			context.setRestricted(false);
			return WorldEdit.getInstance().getBlockFactory().parseFromInput(val, context);
		} catch (InputParseException e) {
			throw new InvalidConfigException("Invalid block state: " + val, e);
		}
	}

	@SuppressWarnings("unchecked")
	public static ItemStack parseItem(Object val) {
		if (val instanceof String) {
			String typeAndCount = (String) val;
			int starIndex = typeAndCount.indexOf('*');
			int count = starIndex == -1 ? 1 : parseInt(typeAndCount.substring(0, starIndex));
			String originalItemName = starIndex == -1 ? typeAndCount : typeAndCount.substring(starIndex + 1);
			String itemName = originalItemName;
			if (itemName.startsWith("minecraft:")) itemName = itemName.substring("minecraft:".length());
			itemName = itemName.toUpperCase(Locale.ROOT);
			Material material = Material.getMaterial(itemName);
			if (material == null || !material.isItem()) {
				throw new InvalidConfigException("Unknown item: " + originalItemName);
			}
			if (count < 1 || count > material.getMaxStackSize()) {
				throw new InvalidConfigException("Cannot have a stack size of " + count + " for item " + originalItemName);
			}
			return new ItemStack(material, count);
		} else if (val instanceof Map) {
			return ItemStack.deserialize((Map<String, Object>) val);
		} else if (val instanceof ConfigurationSection) {
			return ItemStack.deserialize(((ConfigurationSection) val).getValues(false));
		} else {
			throw new InvalidConfigException("Don't know how to turn a " + val.getClass().getSimpleName() + " into an ItemStack");
		}
	}

	public static Object serializeItemStack(ItemStack stack) {
		if (stack.hasItemMeta()) {
			return stack.serialize();
		} else if (stack.getAmount() != 1) {
			return stack.getAmount() + "*" + stack.getType().getKey();
		} else {
			return stack.getType().getKey().toString();
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

	public static <T extends Enum<T>> T parseEnum(Class<T> type, String val) {
		for (T enumVal : type.getEnumConstants()) {
			if (enumVal.name().equals(val.toUpperCase(Locale.ROOT))) {
				return enumVal;
			}
		}
		throw new InvalidConfigException("Invalid " + type.getSimpleName() + ": " + val);
	}

	public static String enumToString(Enum<?> val) {
		return val.name().toLowerCase(Locale.ROOT);
	}

	public static <T> Object serializeSingleableList(List<T> list, Function<T, String> toStringFunction) {
		if (list.size() == 1) {
			return toStringFunction.apply(list.get(0));
		} else {
			return list.stream().map(toStringFunction).collect(Collectors.toCollection(ArrayList::new));
		}
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
}
