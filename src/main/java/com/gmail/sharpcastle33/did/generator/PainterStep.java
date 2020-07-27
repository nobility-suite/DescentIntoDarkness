package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.Map;

public abstract class PainterStep {
	private final Type type;

	public PainterStep(Type type) {
		this.type = type;
	}

	public final Type getType() {
		return type;
	}

	public abstract Object serialize();

	public static PainterStep deserialize(Object value) {
		if (value instanceof String) {
			String[] args = ((String) value).split("\\s+");
			Type type = Type.byName(args[0]);
			if (type == null) {
				throw new InvalidConfigException((String) value);
			}
			switch (type) {
				case CHANCE_REPLACE: {
					if (args.length < 4) {
						throw new InvalidConfigException((String) value);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					double chance = ConfigUtil.parseDouble(args[3]);
					if (chance < 0) {
						chance = 0;
					} else if (chance > 1) {
						chance = 1;
					}
					return new ChanceReplace(old, _new, chance);

				}
				case RADIUS_REPLACE: {
					if (args.length < 3) {
						throw new InvalidConfigException((String) value);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					return new RadiusReplace(old, _new);
				}
				case REPLACE_CEILING: {
					if (args.length < 3) {
						throw new InvalidConfigException((String) value);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					return new ReplaceCeiling(old, _new);
				}
				case REPLACE_FLOOR: {
					if (args.length < 3) {
						throw new InvalidConfigException((String) value);
					}
                    BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
                    BlockStateHolder<?> _new = ConfigUtil.parseBlock(args[2]);
					return new ReplaceFloor(old, _new);
				}
				default: {
					throw new InvalidConfigException((String) value);
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

		public ChanceReplace(BlockStateHolder<?> old, BlockStateHolder<?> _new, double chance) {
			super(Type.CHANCE_REPLACE);
			this.old = old;
			this._new = _new;
			this.chance = chance;
		}

		@Override
		public Object serialize() {
			return getType().getName() + " " + old.getAsString() + " " + _new.getAsString() + " " + chance;
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.chanceReplace(ctx, loc, r, old, _new, chance);
		}
	}

	public static class RadiusReplace extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;

		public RadiusReplace(BlockStateHolder<?> old, BlockStateHolder<?> _new) {
			super(Type.RADIUS_REPLACE);
			this.old = old;
			this._new = _new;
		}

		@Override
		public Object serialize() {
			return getType().getName() + " " + old.getAsString() + " " + _new.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.radiusReplace(ctx, loc, r, old, _new);
		}
	}

	public static class ReplaceCeiling extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;

		public ReplaceCeiling(BlockStateHolder<?> old, BlockStateHolder<?> _new) {
			super(Type.REPLACE_CEILING);
			this.old = old;
			this._new = _new;
		}

		@Override
		public Object serialize() {
			return getType().getName() + " " + old.getAsString() + " " + _new.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.replaceCeiling(ctx, loc, r, old, _new);
		}
	}

	public static class ReplaceFloor extends PainterStep {
		private final BlockStateHolder<?> old;
		private final BlockStateHolder<?> _new;

		public ReplaceFloor(BlockStateHolder<?> old, BlockStateHolder<?> _new) {
			super(Type.REPLACE_FLOOR);
			this.old = old;
			this._new = _new;
		}

		@Override
		public Object serialize() {
			return getType().getName() + " " + old.getAsString() + " " + _new.getAsString();
		}

		@Override
		public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
			PostProcessor.replaceFloor(ctx, loc, r, old, _new);
		}
	}

	public enum Type {
		CHANCE_REPLACE("chance_replace"),
		RADIUS_REPLACE("radius_replace"),
		REPLACE_CEILING("replace_ceiling"),
		REPLACE_FLOOR("replace_floor"),
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
