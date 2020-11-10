package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class FloorLayerStep extends PainterStep {
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
	public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		PostProcessor.floorLayer(ctx, loc, r, block);
	}
}
