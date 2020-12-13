package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

public class VeinStructure extends Structure {
	private final BlockStateHolder<?> ore;
	private final int radius;

	public VeinStructure(String name, ConfigurationSection map) {
		super(name, StructureType.VEIN, map);
		this.ore = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "ore"));
		this.radius = map.getInt("radius", 4);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("ore", ConfigUtil.serializeBlock(ore));
		map.set("radius", radius);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, boolean force) throws WorldEditException {
		ModuleGenerator.generateOreCluster(ctx, pos, radius, block -> canReplace(ctx, block), ore);
	}
}
