package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;

public class PatchStructure extends AbstractPatchStructure {
	private final BlockProvider block;

	protected PatchStructure(String name, ConfigurationSection map) {
		super(name, StructureType.PATCH, map);
		this.block = ConfigUtil.parseBlockProvider(ConfigUtil.require(map, "block"));
	}

	@Override
	protected boolean doPlace(CaveGenContext ctx, BlockVector3 pos, Centroid centroid) {
		ctx.setBlock(pos, block.get(ctx, centroid));
		return true;
	}
}
