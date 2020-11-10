package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class PatchStructure extends AbstractPatchStructure {
	private final BlockStateHolder<?> block;

	protected PatchStructure(String name, List<StructurePlacementEdge> edges, double chance, int count,
							 List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace,
							 List<String> tags, boolean tagsInverted, int spreadX, int spreadY, int spreadZ, int tries
			, BlockStateHolder<?> block) {
		super(name, StructureType.PATCH, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted, spreadX, spreadY,
				spreadZ, tries);
		this.block = block;
	}

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
	protected void doPlace(CaveGenContext ctx, BlockVector3 pos, Direction side) {
		ctx.setBlock(pos, block);
	}
}
