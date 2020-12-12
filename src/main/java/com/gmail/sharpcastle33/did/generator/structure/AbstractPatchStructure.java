package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

public abstract class AbstractPatchStructure extends Structure {
	private final int spreadX;
	private final int spreadY;
	private final int spreadZ;
	private final boolean spreadLocal;
	private final int tries;

	protected AbstractPatchStructure(String name, StructureType type, ConfigurationSection map) {
		super(name, type, map);
		this.spreadX = map.getInt("spreadX", 8);
		this.spreadY = map.getInt("spreadY", 4);
		this.spreadZ = map.getInt("spreadZ", 8);
		if (spreadX < 0 || spreadY < 0 || spreadZ < 0) {
			throw new InvalidConfigException("Spread cannot be negative");
		}
		this.spreadLocal = map.getBoolean("spreadLocal", false);
		this.tries = map.getInt("tries", 64);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("spreadX", spreadX);
		map.set("spreadY", spreadY);
		map.set("spreadZ", spreadZ);
		map.set("spreadLocal", spreadLocal);
		map.set("tries", tries);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, boolean force) throws WorldEditException {
		BlockVector3 origin = pos.subtract(getOriginPositionSide().toBlockVector());
		BlockVector3 spread = BlockVector3.at(spreadX, spreadY, spreadZ);
		if (!spreadLocal) {
			spread = Util.applyDirection(ctx.getLocationTransform(), spread).abs();
		}
		for (int i = 0; i < tries; i++) {
			BlockVector3 offsetPos = origin.add(
					ctx.rand.nextInt(spread.getX() + 1) - ctx.rand.nextInt(spread.getX() + 1),
					ctx.rand.nextInt(spread.getY() + 1) - ctx.rand.nextInt(spread.getY() + 1),
					ctx.rand.nextInt(spread.getZ() + 1) - ctx.rand.nextInt(spread.getZ() + 1)
			);
			if (canReplace(ctx, ctx.getBlock(offsetPos))) {
				BlockStateHolder<?> blockBelow = ctx.getBlock(offsetPos.add(getOriginPositionSide().toBlockVector()));
				if (canPlaceOn(ctx, blockBelow)) {
					doPlace(ctx, offsetPos);
				}
			}
		}
	}

	protected abstract void doPlace(CaveGenContext ctx, BlockVector3 pos);
}
