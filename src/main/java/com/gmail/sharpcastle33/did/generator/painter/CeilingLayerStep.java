package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;

public class CeilingLayerStep extends SimplePainterStep {
	private final BlockPredicate canPlaceOn;
	private final BlockProvider block;

	public CeilingLayerStep(ConfigurationSection map) {
		super(PainterStepType.CEILING_LAYER, map);
		canPlaceOn = map.contains("canPlaceOn") ? ConfigUtil.parseBlockPredicate(map.get("canPlaceOn")) : block -> true;
		block = ConfigUtil.parseBlockProvider(ConfigUtil.require(map, "block"));
	}

	@Override
	protected boolean canEverApplyToPos(CaveGenContext ctx, BlockVector3 pos) {
		return PostProcessor.isRoof(ctx, pos) && canPlaceOn.test(ctx.getBlock(pos));
	}

	@Override
	protected int getMinY(int radius) {
		return 3;
	}

	@Override
	protected void applyToBlock(CaveGenContext ctx, BlockVector3 pos, Centroid centroid) {
		ctx.setBlock(pos.add(0, -1, 0), block.get(ctx, centroid));
	}
}
