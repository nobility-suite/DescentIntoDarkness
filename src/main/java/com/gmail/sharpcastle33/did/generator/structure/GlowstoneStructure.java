package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.configuration.ConfigurationSection;

public class GlowstoneStructure extends Structure {
	private final int density;
	private final int spreadX;
	private final int height;
	private final int spreadZ;
	private final BlockStateHolder<?> block;

	public GlowstoneStructure(String name, ConfigurationSection map) {
		super(name, StructureType.GLOWSTONE, map);
		this.density = map.getInt("density", 1500);
		this.spreadX = map.getInt("spreadX", 8);
		this.height = map.getInt("height", 12);
		this.spreadZ = map.getInt("spreadZ", 8);
		if (spreadX <= 0 || height <= 0 || spreadZ <= 0) {
			throw new InvalidConfigException("spreadX, height and spreadZ must be positive");
		}
		String blockVal = map.getString("block");
		if (blockVal == null) {
			this.block = Util.requireDefaultState(BlockTypes.GLOWSTONE);
		} else {
			this.block = ConfigUtil.parseBlock(blockVal);
		}
	}

	@Override
	protected Direction getOriginPositionSide() {
		return Direction.UP;
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("density", density);
		map.set("spreadX", spreadX);
		map.set("height", height);
		map.set("spreadZ", spreadZ);
		map.set("block", ConfigUtil.serializeBlock(block));
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, boolean force) throws WorldEditException {
		ctx.setBlock(pos, block);
		for (int i = 0; i < density; i++) {
			BlockVector3 offsetPos = pos.add(
					ctx.rand.nextInt(spreadX) - ctx.rand.nextInt(spreadX),
					-ctx.rand.nextInt(height),
					ctx.rand.nextInt(spreadZ) - ctx.rand.nextInt(spreadZ)
			);

			int neighboringGlowstone = 0;
			for (Direction dir : Direction.valuesOf(Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT)) {
				if (block.equalsFuzzy(ctx.getBlock(offsetPos.add(dir.toBlockVector())))) {
					neighboringGlowstone++;
					if (neighboringGlowstone > 1) {
						break;
					}
				}
			}

			if (neighboringGlowstone == 1 && canReplace(ctx, ctx.getBlock(offsetPos))) {
				ctx.setBlock(offsetPos, block);
			}
		}
	}
}
