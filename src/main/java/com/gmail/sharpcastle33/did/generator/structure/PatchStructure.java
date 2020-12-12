package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

public class PatchStructure extends AbstractPatchStructure {
	private final BlockStateHolder<?> block;

	protected PatchStructure(String name, ConfigurationSection map) {
		super(name, StructureType.PATCH, map);
		this.block = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "block"));
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		super.serialize0(map);
		map.set("block", ConfigUtil.serializeBlock(block));
	}

	@Override
	protected void doPlace(CaveGenContext ctx, BlockVector3 pos) {
		ctx.setBlock(pos, block);
	}
}
