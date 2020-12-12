package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.BlockTypeRange;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;
import java.util.function.Predicate;

public class ReplaceMesaStep extends PainterStep {
	private final BlockStateHolder<?> old;
	private final BlockTypeRange<Integer> mesaLayers;

	public ReplaceMesaStep(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old,
						   BlockTypeRange<Integer> mesaLayers) {
		super(PainterStepType.REPLACE_MESA, tags, tagsInverted);
		this.old = old;
		this.mesaLayers = mesaLayers;
	}

	@Override
	public Object serialize() {
		return getSerializationPrefix() + " " + ConfigUtil.serializeBlock(old) + " " + mesaLayers.serializePainter();
	}

	@Override
	public void apply(CaveGenContext ctx, BlockVector3 center, int radius, Predicate<BlockVector3> canTryToPaint) throws MaxChangedBlocksException {
		int x = center.getBlockX();
		int y = center.getBlockY();
		int z = center.getBlockZ();

		for (int ty = -radius; ty <= radius; ty++) {
			BlockStateHolder<?> replacement = mesaLayers.get(ty + y);
			if (replacement == null) {
				continue;
			}
			for (int tx = -radius; tx <= radius; tx++) {
				for (int tz = -radius; tz <= radius; tz++) {
					if (tx * tx  +  ty * ty  +  tz * tz <= (radius - 2) * (radius - 2)) {
						if(tx == 0 && tz == 0 && Math.abs(tx + ty + tz) == radius - 2) {
							continue;
						}

						BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
						if (!ctx.style.isTransparentBlock(ctx.getBlock(pos)) && ctx.getBlock(pos).equalsFuzzy(old) && !PostProcessor.isFloor(ctx, pos) && canTryToPaint.test(pos)) {
							ctx.setBlock(pos, replacement);
						}
					}
				}
			}
		}
	}
}
