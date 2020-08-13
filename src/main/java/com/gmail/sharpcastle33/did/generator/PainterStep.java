package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PainterStep {
	private final Type type;
	private final List<String> tags;

	public PainterStep(Type type, List<String> tags) {
		this.type = type;
		this.tags = tags;
	}

	public final Type getType() {
		return type;
	}

	public final List<String> getTags() {
		return tags;
	}

	public abstract Object serialize();

	protected final String getSerializationPrefix() {
		String prefix = "";
		if (!tags.isEmpty()) {
			prefix = "<" + String.join(" ", tags) + "> ";
		}
		return prefix + type.getName();
	}

	public static PainterStep deserialize(Object value) {
		if (value instanceof String) {
			String line = (String) value;
			line = line.trim();
			List<String> tags = new ArrayList<>();
			if (line.startsWith("<")) {
				int closeIndex = line.indexOf('>');
				if (closeIndex >= 0) {
					Collections.addAll(tags, line.substring(1, closeIndex).split(" "));
					line = line.substring(closeIndex + 1).trim();
				}
			}

			String[] args = line.split("\\s+");
			Type type = Type.byName(args[0]);
			if (type == null) {
				throw new InvalidConfigException(line);
			}
			switch (type) {
				case CHANCE_REPLACE: {
					if (args.length < 4) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					double chance = ConfigUtil.parseDouble(args[3]);
					if (chance < 0) {
						chance = 0;
					} else if (chance > 1) {
						chance = 1;
					}
					return new ChanceReplace(tags, old, _new, chance);

				}
				case RADIUS_REPLACE: {
					if (args.length < 3) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					return new RadiusReplace(tags, old, _new);
				}
				case REPLACE_CEILING: {
					if (args.length < 3) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					return new ReplaceCeiling(tags, old, _new);
				}
				case REPLACE_FLOOR: {
					if (args.length < 3) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					return new ReplaceFloor(tags, old, _new);
				}
				case FLOOR_LAYER: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> block = ConfigUtil.parseBlock(args[1]);
					return new FloorLayer(tags, block);
				}
				case CEILING_LAYER: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> block = ConfigUtil.parseBlock(args[1]);
					return new CeilingLayer(tags, block);
				}
				default: {
					throw new InvalidConfigException(line);
				}
			}
		}

		throw new InvalidConfigException("Invalid painter step type: " + value.getClass());
	}

	public abstract void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException;

	public static class ChanceReplace extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;
		private final double chance;

		public ChanceReplace(List<String> tags, BlockStateHolder<?> old, BlockStateHolder<?> _new, double chance) {
			super(Type.CHANCE_REPLACE, tags);
			this.old = old;
			this._new = _new;
			this.chance = chance;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString() + " " + chance;
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.chanceReplace(ctx, loc, r, old, _new, chance);
		}
	}

	public static class RadiusReplace extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;

		public RadiusReplace(List<String> tags, BlockStateHolder<?> old, BlockStateHolder<?> _new) {
			super(Type.RADIUS_REPLACE, tags);
			this.old = old;
			this._new = _new;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.radiusReplace(ctx, loc, r, old, _new);
		}
	}

	public static class ReplaceCeiling extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;

		public ReplaceCeiling(List<String> tags, BlockStateHolder<?> old, BlockStateHolder<?> _new) {
			super(Type.REPLACE_CEILING, tags);
			this.old = old;
			this._new = _new;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.replaceCeiling(ctx, loc, r, old, _new);
		}
	}

	public static class ReplaceFloor extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;

		public ReplaceFloor(List<String> tags, BlockStateHolder<?> old, BlockStateHolder<?> _new) {
			super(Type.REPLACE_FLOOR, tags);
			this.old = old;
			this._new = _new;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.replaceFloor(ctx, loc, r, old, _new);
		}
	}

	public static class CeilingLayer extends PainterStep {
		private final BlockStateHolder<?> block;

		public CeilingLayer(List<String> tags, BlockStateHolder<?> block) {
			super(Type.CEILING_LAYER, tags);
			this.block = block;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + block.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.ceilingLayer(ctx, loc, r, block);
		}
	}

	public static class FloorLayer extends PainterStep {
		private final BlockStateHolder<?> block;

		public FloorLayer(List<String> tags, BlockStateHolder<?> block) {
			super(Type.FLOOR_LAYER, tags);
			this.block = block;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + block.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.floorLayer(ctx, loc, r, block);
		}
	}

	public enum Type {
		CHANCE_REPLACE("chance_replace"),
		RADIUS_REPLACE("radius_replace"),
		REPLACE_CEILING("replace_ceiling"),
		REPLACE_FLOOR("replace_floor"),
		CEILING_LAYER("ceiling_layer"),
		FLOOR_LAYER("floor_layer"),
		;
		private final String name;
		Type(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static Type byName(String name) {
			return BY_NAME.get(name);
		}

		private static final Map<String, Type> BY_NAME = new HashMap<>();
		static {
			for (Type type : values()) {
				BY_NAME.put(type.getName(), type);
			}
		}
	}
}
