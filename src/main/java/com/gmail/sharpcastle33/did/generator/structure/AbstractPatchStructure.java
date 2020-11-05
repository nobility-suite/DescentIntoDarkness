package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public abstract class AbstractPatchStructure extends Structure {
	private final int spreadX;
	private final int spreadY;
	private final int spreadZ;
	private final int tries;

	protected AbstractPatchStructure(String name, StructureType type, ConfigurationSection map) {
		super(name, type, map);
		this.spreadX = map.getInt("spreadX", 8);
		this.spreadY = map.getInt("spreadY", 4);
		this.spreadZ = map.getInt("spreadZ", 8);
		if (spreadX < 0 || spreadY < 0 || spreadZ < 0) {
			throw new InvalidConfigException("Spread cannot be negative");
		}
		this.tries = map.getInt("tries", 64);
	}

	protected AbstractPatchStructure(String name, StructureType type, List<StructurePlacementEdge> edges, double chance, int count,
									 List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace,
									 List<String> tags, boolean tagsInverted, int spreadX, int spreadY, int spreadZ,
									 int tries) {
		super(name, type, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
		this.spreadX = spreadX;
		this.spreadY = spreadY;
		this.spreadZ = spreadZ;
		this.tries = tries;
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("spreadX", spreadX);
		map.set("spreadY", spreadY);
		map.set("spreadZ", spreadZ);
		map.set("tries", tries);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, Direction side, boolean force) throws WorldEditException {
		BlockVector3 origin = pos.subtract(side.toBlockVector());
		for (int i = 0; i < tries; i++) {
			BlockVector3 offsetPos = origin.add(
					ctx.rand.nextInt(spreadX + 1) - ctx.rand.nextInt(spreadX + 1),
					ctx.rand.nextInt(spreadY + 1) - ctx.rand.nextInt(spreadY + 1),
					ctx.rand.nextInt(spreadZ + 1) - ctx.rand.nextInt(spreadZ + 1)
			);
			if (canReplace(ctx, ctx.getBlock(offsetPos))) {
				BlockStateHolder<?> blockBelow = ctx.getBlock(offsetPos.add(side.toBlockVector()));
				boolean allowPlacement;
				if (canPlaceOn == null) {
					allowPlacement = !ctx.style.isTransparentBlock(blockBelow);
				} else {
					allowPlacement = canPlaceOn.stream().anyMatch(it -> it.equalsFuzzy(blockBelow));
				}
				if (allowPlacement) {
					doPlace(ctx, offsetPos, side);
				}
			}
		}
	}

	protected abstract void doPlace(CaveGenContext ctx, BlockVector3 pos, Direction side);
}
