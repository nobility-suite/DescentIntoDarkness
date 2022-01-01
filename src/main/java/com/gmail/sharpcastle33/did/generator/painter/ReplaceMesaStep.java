package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.BlockTypeRange;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Predicate;

public class ReplaceMesaStep extends PainterStep {
	private final BlockPredicate old;
	private final BlockTypeRange<Integer> mesaLayers;

	public ReplaceMesaStep(ConfigurationSection map) {
		super(PainterStepType.REPLACE_MESA, map);
		this.old = ConfigUtil.parseBlockPredicate(ConfigUtil.require(map, "old"));
		this.mesaLayers = BlockTypeRange.deserializeInt(ConfigUtil.require(map, "layers"));
	}

	@Override
	public void apply(CaveGenContext ctx, Centroid centroid, Predicate<BlockVector3> canTryToPaint) throws MaxChangedBlocksException {
		BlockVector3 center = centroid.pos.toBlockPoint();
		int x = center.getBlockX();
		int y = center.getBlockY();
		int z = center.getBlockZ();
		int radius = centroid.size + 4;

		for (int ty = -radius; ty <= radius; ty++) {
			BlockProvider replacement = mesaLayers.get(ty + y);
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
						if (!ctx.style.isTransparentBlock(ctx.getBlock(pos)) && old.test(ctx.getBlock(pos)) && !PostProcessor.isFloor(ctx, pos) && canTryToPaint.test(pos)) {
							ctx.setBlock(pos, replacement.get(ctx, centroid));
						}
					}
				}
			}
		}
	}
}
