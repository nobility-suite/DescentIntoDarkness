package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class VinePatchStructure extends AbstractPatchStructure {
	private final BlockStateHolder<?> vine;
	private final BlockStateHolder<?> firstBlock;
	private final BlockStateHolder<?> lastBlock;
	private final int minHeight;
	private final int maxHeight;
	private final boolean randomRotation;

	protected VinePatchStructure(String name, List<StructurePlacementEdge> edges, double chance, int count,
								 List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace,
								 List<String> tags, boolean tagsInverted, int spreadX, int spreadY, int spreadZ,
								 int tries, BlockStateHolder<?> vine, BlockStateHolder<?> firstBlock,
								 BlockStateHolder<?> lastBlock, int minHeight, int maxHeight, boolean randomRotation) {
		super(name, StructureType.VINE_PATCH, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted, spreadX,
				spreadY, spreadZ, tries);
		this.vine = vine;
		this.firstBlock = firstBlock;
		this.lastBlock = lastBlock;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.randomRotation = randomRotation;
	}

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
		this.randomRotation = map.getBoolean("randomRotation", true);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		super.serialize0(map);
		map.set("vine", ConfigUtil.serializeBlock(vine));
		map.set("firstBlock", ConfigUtil.serializeBlock(firstBlock));
		map.set("lastBlock", ConfigUtil.serializeBlock(lastBlock));
		map.set("minHeight", minHeight);
		map.set("maxHeight", maxHeight);
		map.set("randomRotation", randomRotation);
	}

	@Override
	protected void doPlace(CaveGenContext ctx, BlockVector3 pos, Direction side) {
		int height = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);
		BlockStateHolder<?> block;
		BlockStateHolder<?> firstBlock;
		BlockStateHolder<?> lastBlock;
		if (randomRotation) {
			int angle = ctx.rand.nextInt(4) * 90;
			block = rotate(vine, angle);
			firstBlock = rotate(this.firstBlock, angle);
			lastBlock = rotate(this.lastBlock, angle);
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
			offsetPos = offsetPos.subtract(side.toBlockVector());
		}
		offsetPos = offsetPos.add(side.toBlockVector());
		if (placed) {
			ctx.setBlock(offsetPos, lastBlock);
		}
	}

	@SuppressWarnings("unchecked")
	private static <B extends BlockStateHolder<B>> B rotate(BlockStateHolder<?> block, double degrees) {
		return BlockTransformExtent.transform((B) block, new AffineTransform().rotateY(degrees));
	}
}
