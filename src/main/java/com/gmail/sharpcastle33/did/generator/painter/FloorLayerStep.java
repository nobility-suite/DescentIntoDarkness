package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class FloorLayerStep extends SimplePainterStep {
	private final BlockStateHolder<?> block;

	public FloorLayerStep(List<String> tags, boolean tagsInverted, BlockStateHolder<?> block) {
		super(PainterStepType.FLOOR_LAYER, tags, tagsInverted);
		this.block = block;
	}

	@Override
	public Object serialize() {
		return getSerializationPrefix() + " " + ConfigUtil.serializeBlock(block);
	}

	@Override
	protected boolean canEverApplyToPos(CaveGenContext ctx, BlockVector3 pos) {
		return PostProcessor.isFloor(ctx, pos) && !block.equalsFuzzy(ctx.getBlock(pos));
	}

	@Override
	protected int getMaxY(int radius) {
		return -3;
	}

	@Override
	protected void applyToBlock(CaveGenContext ctx, BlockVector3 pos) {
		ctx.setBlock(pos.add(0, 1, 0), block);
	}
}
