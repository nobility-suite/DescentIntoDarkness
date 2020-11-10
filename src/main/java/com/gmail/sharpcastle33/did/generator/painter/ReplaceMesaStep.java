package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.BlockTypeRange;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.PostProcessor;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;

public class ReplaceMesaStep extends PainterStep {
	private final BlockStateHolder<?> old;
	private final BlockTypeRange<Integer> mesaLayers;

	public ReplaceMesaStep(List<String> tags, boolean tagsInverted, BlockStateHolder<?> old,
						   BlockTypeRange<Integer> mesaLayers) {
		super(PainterStepType.REPLACE_MESA, tags, tagsInverted);
		this.old = old;
		this.mesaLayers = mesaLayers;
	}

	@Override
	public Object serialize() {
		return getSerializationPrefix() + " " + ConfigUtil.serializeBlock(old) + " " + mesaLayers.serializePainter();
	}

	@Override
	public void apply(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		PostProcessor.replaceMesa(ctx, loc, r, old, mesaLayers);
	}
}
