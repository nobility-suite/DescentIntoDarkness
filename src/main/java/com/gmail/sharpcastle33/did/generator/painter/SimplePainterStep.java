package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Predicate;

public abstract class SimplePainterStep extends PainterStep {
	public SimplePainterStep(PainterStepType type, ConfigurationSection map) {
		super(type, map);
	}

	@Override
	public void apply(CaveGenContext ctx, Centroid centroid, Predicate<BlockVector3> canTryToPaint) throws MaxChangedBlocksException {
		BlockVector3 center = centroid.pos.toBlockPoint();
		int x = center.getBlockX();
		int y = center.getBlockY();
		int z = center.getBlockZ();
		int radius = centroid.size + 4;

		for (int ty = getMinY(radius); ty <= getMaxY(radius); ty++) {
			for (int tx = -radius; tx <= radius; tx++) {
				for (int tz = -radius; tz <= radius; tz++) {
					if (tx * tx  +  ty * ty  +  tz * tz <= (radius - 2) * (radius - 2)) {
						if(tx == 0 && tz == 0 && Math.abs(tx + ty + tz) == radius - 2) {
							continue;
						}

						BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
						if (!ctx.style.isTransparentBlock(ctx.getBlock(pos)) && canEverApplyToPos(ctx, pos) && canTryToPaint.test(pos)) {
							applyToBlock(ctx, pos, centroid);
						}
					}
				}
			}
		}
	}

	protected int getMinY(int radius) {
		return -radius;
	}

	protected int getMaxY(int radius) {
		return radius;
	}

	protected boolean canEverApplyToPos(CaveGenContext ctx, BlockVector3 pos) {
		return true;
	}

	protected abstract void applyToBlock(CaveGenContext ctx, BlockVector3 pos, Centroid centroid);
}
