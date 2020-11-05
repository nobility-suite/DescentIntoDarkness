package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class CeilingLayerStep extends PainterStep {
	private final BlockStateHolder<?> block;

	public CeilingLayerStep(List<String> tags, boolean tagsInverted, BlockStateHolder<?> block) {
		super(PainterStepType.CEILING_LAYER, tags, tagsInverted);
		this.block = block;
	}

	@Override
	public Object serialize() {
		return getSerializationPrefix() + " " + block.getAsString();
	}

	@Override
	public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		PostProcessor.ceilingLayer(ctx, loc, r, block);
	}
}
