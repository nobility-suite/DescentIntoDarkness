package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

public class VeinStructure extends Structure {
	private final BlockProvider ore;
	private final int radius;

	public VeinStructure(String name, ConfigurationSection map) {
		super(name, StructureType.VEIN, map);
		this.ore = ConfigUtil.parseBlockProvider(ConfigUtil.require(map, "ore"));
		this.radius = map.getInt("radius", 4);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, Centroid centroid, boolean force) throws WorldEditException {
		ModuleGenerator.generateOreCluster(ctx, centroid, pos, radius, block -> canReplace(ctx, block), ore);
	}

	@Override
	protected boolean defaultCanReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		return !ctx.style.isTransparentBlock(block);
	}
}
