package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class ReplaceAllStep extends SimplePainterStep {
	private final BlockStateHolder<?> old;
	private final BlockStateHolder<?> _new;
	private final double chance;

	public ReplaceAllStep(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old, BlockStateHolder<?> _new,
						  double chance) {
		super(PainterStepType.REPLACE_ALL, tags, tagsInverted);
		this.old = old;
		this._new = _new;
		this.chance = chance;
	}

	@Override
	public Object serialize() {
		String ret = getSerializationPrefix() + " " + ConfigUtil.serializeBlock(old) + " " + ConfigUtil.serializeBlock(_new);
		if (chance != 1) {
			ret += " " + chance;
		}
		return ret;
	}

	@Override
	protected void applyToBlock(CaveGenContext ctx, BlockVector3 pos) {
		if (ctx.getBlock(pos).equalsFuzzy(old) && ctx.rand.nextDouble() < chance) {
			ctx.setBlock(pos, _new);
		}
	}
}
