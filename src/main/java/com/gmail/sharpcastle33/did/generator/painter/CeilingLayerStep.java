package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class CeilingLayerStep extends SimplePainterStep {
	private final BlockStateHolder<?> block;

	public CeilingLayerStep(List<String> tags, boolean tagsInverted, BlockStateHolder<?> block) {
		super(PainterStepType.CEILING_LAYER, tags, tagsInverted);
		this.block = block;
	}

	@Override
	public Object serialize() {
		return getSerializationPrefix() + " " + ConfigUtil.serializeBlock(block);
	}

	@Override
	protected boolean canEverApplyToPos(CaveGenContext ctx, BlockVector3 pos) {
		return PostProcessor.isRoof(ctx, pos) && !block.equalsFuzzy(ctx.getBlock(pos));
	}

	@Override
	protected int getMinY(int radius) {
		return 3;
	}

	@Override
	protected void applyToBlock(CaveGenContext ctx, BlockVector3 pos) {
		ctx.setBlock(pos.add(0, -1, 0), block);
	}
}
