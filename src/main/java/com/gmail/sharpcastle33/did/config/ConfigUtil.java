package com.gmail.sharpcastle33.did.config;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigUtil {
	public static BlockStateHolder<?> parseBlock(String val) {
		try {
			ParserContext context = new ParserContext();
			context.setRestricted(false);
			return WorldEdit.getInstance().getBlockFactory().parseFromInput(val, context);
		} catch (InputParseException e) {
			throw new InvalidConfigException("Invalid block state: " + val, e);
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
