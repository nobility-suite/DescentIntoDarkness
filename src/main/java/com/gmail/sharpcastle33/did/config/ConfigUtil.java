package com.gmail.sharpcastle33.did.config;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.block.BlockStateHolder;

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
}
