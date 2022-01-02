package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import org.bukkit.configuration.ConfigurationSection;

public class VinePatchStructure extends AbstractPatchStructure {
	private final BlockProvider vine;
	private final BlockProvider firstBlock;
	private final BlockProvider lastBlock;
	private final int minHeight;
	private final int maxHeight;
	private final boolean vineRandomRotation;

	protected VinePatchStructure(String name, ConfigurationSection map) {
		super(name, StructureType.VINE_PATCH, map);
		this.vine = ConfigUtil.parseBlockProvider(ConfigUtil.require(map, "vine"));
		this.firstBlock = map.contains("firstBlock") ? ConfigUtil.parseBlockProvider(map.get("firstBlock")) : vine;
		this.lastBlock = map.contains("lastBlock") ? ConfigUtil.parseBlockProvider(map.get("lastBlock")) : vine;
		this.minHeight = map.getInt("minHeight", 5);
		this.maxHeight = map.getInt("maxHeight", 10);
		if (minHeight < 1 || maxHeight < minHeight) {
			throw new InvalidConfigException("Invalid height range");
		}
		this.vineRandomRotation = map.getBoolean("vineRandomRotation", true);
	}

	@Override
	protected Direction getOriginPositionSide() {
		return Direction.UP;
	}

	@Override
	protected boolean doPlace(CaveGenContext ctx, BlockVector3 pos, Centroid centroid) {
		int height = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);
		int angle = vineRandomRotation ? ctx.rand.nextInt(4) * 90 : 0;
		Transform transform = new AffineTransform().rotateY(angle);

		BlockVector3 offsetPos = pos;
		boolean placed = false;
		for (int i = 0; i < height && canReplace(ctx, ctx.getBlock(offsetPos)); i++) {
			ctx.setBlock(offsetPos, Util.transformBlock(i == 0 ? firstBlock.get(ctx, centroid) : vine.get(ctx, centroid), transform));
			placed = true;
			offsetPos = offsetPos.add(0, -1, 0);
		}
		offsetPos = offsetPos.add(0, 1, 0);
		if (placed) {
			ctx.setBlock(offsetPos, Util.transformBlock(lastBlock.get(ctx, centroid), transform));
		}

		return placed;
	}
}
