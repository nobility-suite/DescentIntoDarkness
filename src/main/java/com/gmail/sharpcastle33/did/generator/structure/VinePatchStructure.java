package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

public class VinePatchStructure extends AbstractPatchStructure {
	private final BlockStateHolder<?> vine;
	private final BlockStateHolder<?> firstBlock;
	private final BlockStateHolder<?> lastBlock;
	private final int minHeight;
	private final int maxHeight;
	private final boolean vineRandomRotation;

	protected VinePatchStructure(String name, ConfigurationSection map) {
		super(name, StructureType.VINE_PATCH, map);
		this.vine = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "vine"));
		this.firstBlock = ConfigUtil.parseBlock(map.getString("firstBlock", ConfigUtil.serializeBlock(vine)));
		this.lastBlock = ConfigUtil.parseBlock(map.getString("lastBlock", ConfigUtil.serializeBlock(vine)));
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
	protected void serialize0(ConfigurationSection map) {
		super.serialize0(map);
		map.set("vine", ConfigUtil.serializeBlock(vine));
		map.set("firstBlock", ConfigUtil.serializeBlock(firstBlock));
		map.set("lastBlock", ConfigUtil.serializeBlock(lastBlock));
		map.set("minHeight", minHeight);
		map.set("maxHeight", maxHeight);
		map.set("vineRandomRotation", vineRandomRotation);
	}

	@Override
	protected void doPlace(CaveGenContext ctx, BlockVector3 pos) {
		int height = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);
		BlockStateHolder<?> block;
		BlockStateHolder<?> firstBlock;
		BlockStateHolder<?> lastBlock;
		if (vineRandomRotation) {
			int angle = ctx.rand.nextInt(4) * 90;
			Transform transform = new AffineTransform().rotateY(angle);
			block = Util.transformBlock(vine, transform);
			firstBlock = Util.transformBlock(this.firstBlock, transform);
			lastBlock = Util.transformBlock(this.lastBlock, transform);
		} else {
			block = vine;
			firstBlock = this.firstBlock;
			lastBlock = this.lastBlock;
		}

		BlockVector3 offsetPos = pos;
		boolean placed = false;
		for (int i = 0; i < height && canReplace(ctx, ctx.getBlock(offsetPos)); i++) {
			ctx.setBlock(offsetPos, i == 0 ? firstBlock : block);
			placed = true;
			offsetPos = offsetPos.add(0, -1, 0);
		}
		offsetPos = offsetPos.add(0, 1, 0);
		if (placed) {
			ctx.setBlock(offsetPos, lastBlock);
		}
	}
}
