package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.BlockTypeRange;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PainterStep {
	private final PainterStepType type;
	private final List<String> tags;
	private final boolean tagsInverted;

	public PainterStep(PainterStepType type, List<String> tags, boolean tagsInverted) {
		this.type = type;
		this.tags = tags;
		this.tagsInverted = tagsInverted;
	}

	public final PainterStepType getType() {
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
			PainterStepType type = PainterStepType.byName(args[0]);
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
					return new ReplaceAllStep(tags, tagsInverted, old, _new, chance);

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
					return new ReplaceCeilingStep(tags, tagsInverted, old, _new, chance);
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
					return new ReplaceFloorStep(tags, tagsInverted, old, _new, chance);
				}
				case FLOOR_LAYER: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> block = ConfigUtil.parseBlock(args[1]);
					return new FloorLayerStep(tags, tagsInverted, block);
				}
				case CEILING_LAYER: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> block = ConfigUtil.parseBlock(args[1]);
					return new CeilingLayerStep(tags, tagsInverted, block);
				}
				case REPLACE_MESA: {
					if (args.length < 2) {
						throw new InvalidConfigException(line);
					}
					BlockStateHolder<?> old = ConfigUtil.parseBlock(args[1]);
					BlockTypeRange<Integer> mesaLayers = BlockTypeRange.deserializePainter(2, args);
					mesaLayers.validateRange(0, 255, i -> i - 1, i -> i + 1);
					return new ReplaceMesaStep(tags, tagsInverted, old, mesaLayers);
				}
				default: {
					throw new InvalidConfigException(line);
				}
			}
		}

		throw new InvalidConfigException("Invalid painter step type: " + value.getClass());
	}

	public abstract void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException;

}
