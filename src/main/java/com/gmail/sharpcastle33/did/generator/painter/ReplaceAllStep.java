package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class ReplaceAllStep extends PainterStep {
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
