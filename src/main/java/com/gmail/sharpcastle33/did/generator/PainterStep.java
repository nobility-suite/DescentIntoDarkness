package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.config.BlockTypeRange;
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
	private final boolean tagsInverted;

	public PainterStep(Type type, List<String> tags, boolean tagsInverted) {
		this.type = type;
		this.tags = tags;
		this.tagsInverted = tagsInverted;
	}

	public final Type getType() {
		return type;
	}

	public final List<String> getTags() {
		return tags;
	}

	public boolean areTagsInverted() {
		return tagsInverted;
	}

	public abstract Object serialize();

	protected final String getSerializationPrefix() {
		String prefix = "";
		if (!tags.isEmpty() || !tagsInverted) {
			prefix = "<";
			if (tagsInverted) {
				prefix += "!";
			}
			prefix += String.join(" ", tags) + "> ";
		}
		return prefix + type.getName();
	}

	/** @noinspection DuplicatedCode */
	public static PainterStep deserialize(Object value) {
		if (value instanceof String) {
			String line = (String) value;
			line = line.trim();
			List<String> tags = new ArrayList<>();
			boolean tagsInverted = true;
			if (line.startsWith("<")) {
				int closeIndex = line.indexOf('>');
				if (closeIndex >= 0) {
					tagsInverted = line.startsWith("<!");
					Collections.addAll(tags, line.substring(tagsInverted ? 2 : 1, closeIndex).split(" "));
					line = line.substring(closeIndex + 1).trim();
				}
			}

			String[] args = line.split("\\s+");
			Type type = Type.byName(args[0]);
			if (type == null) {
				throw new InvalidConfigException(line);
			}
			switch (type) {
				case REPLACE_ALL: {
					if (args.length < 3) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					double chance = args.length <= 3 ? 1 : ConfigUtil.parseDouble(args[3]);
					if (chance < 0) {
						chance = 0;
					} else if (chance > 1) {
						chance = 1;
					}
					return new ReplaceAll(tags, tagsInverted, old, _new, chance);

				}
				case REPLACE_CEILING: {
					if (args.length < 3) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
                    double chance = args.length <= 3 ? 1 : ConfigUtil.parseDouble(args[3]);
					if (chance < 0) {
						chance = 0;
					} else if (chance > 1) {
						chance = 1;
					}
					return new ReplaceCeiling(tags, tagsInverted, old, _new, chance);
				}
				case REPLACE_FLOOR: {
					if (args.length < 3) {
						throw new InvalidConfigException(line);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					double chance = args.length <= 3 ? 1 : ConfigUtil.parseDouble(args[3]);
					if (chance < 0) {
						chance = 0;
					} else if (chance > 1) {
						chance = 1;
					}
					return new ReplaceFloor(tags, tagsInverted, old, _new, chance);
				}
				case FLOOR_LAYER: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> block = ConfigUtil.parseBlock(args[1]);
					return new FloorLayer(tags, tagsInverted, block);
				}
				case CEILING_LAYER: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> block = ConfigUtil.parseBlock(args[1]);
					return new CeilingLayer(tags, tagsInverted, block);
				}
				case REPLACE_MESA: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
					BlockTypeRange<Integer> mesaLayers = BlockTypeRange.deserializePainter(2, args);
					mesaLayers.validateRange(0, 255, i -> i - 1, i -> i + 1);
					return new ReplaceMesa(tags, tagsInverted, old, mesaLayers);
				}
				default: {
					throw new InvalidConfigException(line);
				}
			}
		}

		throw new InvalidConfigException("Invalid painter step type: " + value.getClass());
	}

	public abstract void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException;

	public static class ReplaceAll extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;
		private final double chance;

		public ReplaceAll(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old, BlockStateHolder<?> _new, double chance) {
			super(Type.REPLACE_ALL, tags, tagsInverted);
			this.old = old;
			this._new = _new;
			this.chance = chance;
		}

		@Override
		public Object serialize() {
			String ret = getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString();
			if (chance != 1) {
				ret += " " + chance;
			}
			return ret;
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.chanceReplaceAll(ctx, loc, r, old, _new, chance);
		}
	}

	public static class ReplaceCeiling extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;
		private final double chance;

		public ReplaceCeiling(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old, BlockStateHolder<?> _new, double chance) {
			super(Type.REPLACE_CEILING, tags, tagsInverted);
			this.old = old;
			this._new = _new;
			this.chance = chance;
		}

		@Override
		public Object serialize() {
			String ret = getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString();
			if (chance != 1) {
				ret += " " + chance;
			}
			return ret;
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.chanceReplaceCeiling(ctx, loc, r, old, _new, chance);
		}
	}

	public static class ReplaceFloor extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;
		private final double chance;

		public ReplaceFloor(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old, BlockStateHolder<?> _new, double chance) {
			super(Type.REPLACE_FLOOR, tags, tagsInverted);
			this.old = old;
			this._new = _new;
			this.chance = chance;
		}

		@Override
		public Object serialize() {
			String ret = getSerializationPrefix() + " " + old.getAsString() + " " + _new.getAsString();
			if (chance != 1) {
				ret += " " + chance;
			}
			return ret;
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.chanceReplaceFloor(ctx, loc, r, old, _new, chance);
		}
	}

	public static class CeilingLayer extends PainterStep {
		private final BlockStateHolder<?> block;

		public CeilingLayer(List<String> tags, boolean tagsInverted, BlockStateHolder<?> block) {
			super(Type.CEILING_LAYER, tags, tagsInverted);
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

		public FloorLayer(List<String> tags, boolean tagsInverted, BlockStateHolder<?> block) {
			super(Type.FLOOR_LAYER, tags, tagsInverted);
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

	public static class ReplaceMesa extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockTypeRange<Integer> mesaLayers;

		public ReplaceMesa(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old, BlockTypeRange<Integer> mesaLayers) {
			super(Type.REPLACE_MESA, tags, tagsInverted);
			this.old = old;
			this.mesaLayers = mesaLayers;
		}

		@Override
		public Object serialize() {
			return getSerializationPrefix() + " " + old.getAsString() + " " + mesaLayers.serializePainter();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.replaceMesa(ctx, loc, r, old, mesaLayers);
		}
	}

	public enum Type {
		REPLACE_ALL("replace_all"),
		REPLACE_CEILING("replace_ceiling"),
		REPLACE_FLOOR("replace_floor"),
		CEILING_LAYER("ceiling_layer"),
		FLOOR_LAYER("floor_layer"),
		REPLACE_MESA("replace_mesa"),
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
