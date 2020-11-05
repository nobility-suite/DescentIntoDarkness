package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class VeinStructure extends Structure {
	private final BlockStateHolder<?> ore;
	private final int radius;

	public VeinStructure(String name, ConfigurationSection map) {
		super(name, StructureType.VEIN, map);
		this.ore = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "ore"));
		this.radius = map.getInt("radius", 4);
	}

	public VeinStructure(String name, List<StructurePlacementEdge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn
			, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, BlockStateHolder<?> ore,
						 int radius) {
		super(name, StructureType.VEIN, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
		this.ore = ore;
		this.radius = radius;
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("ore", ore.getAsString());
		map.set("radius", radius);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, Direction side, boolean force) throws WorldEditException {
		ModuleGenerator.generateOreCluster(ctx, pos, radius, canReplace, ore);
	}
}
