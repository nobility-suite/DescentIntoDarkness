package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;

public class ReplaceCeilingStep extends SimplePainterStep {
	private final BlockPredicate old;
	private final BlockProvider _new;
	private final double chance;

	public ReplaceCeilingStep(ConfigurationSection map) {
		super(PainterStepType.REPLACE_CEILING, map);
		this.old = ConfigUtil.parseBlockPredicate(ConfigUtil.require(map, "old"));
		this._new = ConfigUtil.parseBlockProvider(ConfigUtil.require(map, "new"));
		this.chance = map.getDouble("chance", 1);
	}

	@Override
	protected int getMinY(int radius) {
		return 3;
	}

	@Override
	protected boolean canEverApplyToPos(CaveGenContext ctx, BlockVector3 pos) {
		return PostProcessor.isRoof(ctx, pos);
	}

	@Override
	protected void applyToBlock(CaveGenContext ctx, BlockVector3 pos, Centroid centroid) {
		if (old.test(ctx.getBlock(pos)) && ctx.rand.nextDouble() < chance) {
			ctx.setBlock(pos, _new.get(ctx, centroid));
		}
	}
}
