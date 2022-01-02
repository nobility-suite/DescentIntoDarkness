package com.gmail.sharpcastle33.did.generator.structure;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class ChorusPlantStructure extends Structure {
	private final BlockProvider stemBlock;
	private final BlockProvider flowerBlock;
	private final int minRadius;
	private final int maxRadius;
	private final int minVLength;
	private final int maxVLength;
	private final int minHLength;
	private final int maxHLength;
	private final int minInitialHeightBoost;
	private final int maxInitialHeightBoost;
	private final int minBranchFactor;
	private final int maxBranchFactor;
	private final int minInitialBranchFactorBoost;
	private final int maxInitialBranchFactorBoost;
	private final double flowerChance;
	private final double initialFlowerChance;
	private final int minNumLayers;
	private final int maxNumLayers;

	protected ChorusPlantStructure(String name, ConfigurationSection map) {
		super(name, StructureType.CHORUS_PLANT, map);
		stemBlock = map.contains("stemBlock") ? ConfigUtil.parseBlockProvider(map.get("stemBlock")) : BlockProvider.CHORUS_PLANT;
		flowerBlock = map.contains("flowerBlock") ? ConfigUtil.parseBlockProvider(map.get("flowerBlock")) : BlockProvider.CHORUS_FLOWER;
		minRadius = map.getInt("minRadius", 8);
		maxRadius = map.getInt("maxRadius", 8);
		if (minRadius < 0 || maxRadius < minRadius) {
			throw new InvalidConfigException("Invalid radius range");
		}
		minVLength = map.getInt("minVLength", 1);
		maxVLength = map.getInt("maxVLength", 4);
		if (minVLength < 1 || maxVLength < minVLength) {
			throw new InvalidConfigException("Invalid V length range");
		}
		minHLength = map.getInt("minHLength", 1);
		maxHLength = map.getInt("maxHLength", 1);
		if (minHLength < 1 || maxHLength < minHLength) {
			throw new InvalidConfigException("Invalid H length range");
		}
		minInitialHeightBoost = map.getInt("minInitialHeightBoost", 1);
		maxInitialHeightBoost = map.getInt("maxInitialHeightBoost", 1);
		if (minInitialHeightBoost <= -maxVLength || maxInitialHeightBoost < minInitialHeightBoost) {
			throw new InvalidConfigException("Invalid initial height boost range");
		}
		minBranchFactor = map.getInt("minBranchFactor", 1);
		maxBranchFactor = map.getInt("maxBranchFactor", 3);
		if (minBranchFactor < 1 || maxBranchFactor < minBranchFactor) {
			throw new InvalidConfigException("Invalid branch factor");
		}
		minInitialBranchFactorBoost = map.getInt("minInitialBranchFactorBoost", 1);
		maxInitialBranchFactorBoost = map.getInt("maxInitialBranchFactorBoost", 1);
		if (minInitialBranchFactorBoost <= -maxBranchFactor || maxInitialBranchFactorBoost < minInitialBranchFactorBoost) {
			throw new InvalidConfigException("Invalid initial branch factor boost range");
		}
		flowerChance = map.getDouble("flowerChance", 0.25);
		if (flowerChance < 0 || flowerChance > 1) {
			throw new InvalidConfigException("Flower chance must be between 0 and 1");
		}
		initialFlowerChance = map.getDouble("initialFlowerChance", 0.2);
		if (initialFlowerChance < 0 || initialFlowerChance > 1) {
			throw new InvalidConfigException("Initial flower chance must be between 0 and 1");
		}
		minNumLayers = map.getInt("minNumLayers", 4);
		maxNumLayers = map.getInt("maxNumLayers", 4);
		if (minNumLayers < 0 || maxNumLayers < minNumLayers) {
			throw new InvalidConfigException("Invalid num layers range");
		}
	}

	@Override
	protected boolean shouldTransformBlocksByDefault() {
		return true;
	}

	@Override
	protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
		return Direction.DOWN;
	}

	private boolean canConnectTo(CaveGenContext ctx, BlockStateHolder<?> block) {
		return stemBlock.canProduce(block) || flowerBlock.canProduce(block) || canPlaceOn(ctx, block);
	}

	private BlockStateHolder<?> withConnectionProperties(CaveGenContext ctx, BlockVector3 pos, BlockStateHolder<?> block) {
		BlockType blockType = block.getBlockType();
		if (blockType.hasProperty(PropertyKey.UP) && canConnectTo(ctx, ctx.getBlock(pos.add(0, 1, 0)))) {
			block = block.with(PropertyKey.UP, true);
		}
		if (blockType.hasProperty(PropertyKey.DOWN) && canConnectTo(ctx, ctx.getBlock(pos.add(0, -1, 0)))) {
			block = block.with(PropertyKey.DOWN, true);
		}
		if (blockType.hasProperty(PropertyKey.WEST) && canConnectTo(ctx, ctx.getBlock(pos.add(-1, 0, 0)))) {
			block = block.with(PropertyKey.WEST, true);
		}
		if (blockType.hasProperty(PropertyKey.EAST) && canConnectTo(ctx, ctx.getBlock(pos.add(1, 0, 0)))) {
			block = block.with(PropertyKey.EAST, true);
		}
		if (blockType.hasProperty(PropertyKey.NORTH) && canConnectTo(ctx, ctx.getBlock(pos.add(0, 0, -1)))) {
			block = block.with(PropertyKey.NORTH, true);
		}
		if (blockType.hasProperty(PropertyKey.SOUTH) && canConnectTo(ctx, ctx.getBlock(pos.add(0, 0, 1)))) {
			block = block.with(PropertyKey.SOUTH, true);
		}
		return block;
	}

	private boolean isSurroundedByAir(CaveGenContext ctx, BlockVector3 pos, Direction exceptDirection) {
		for (Direction direction : Direction.valuesOf(Direction.Flag.CARDINAL)) {
			if (direction != exceptDirection && !canReplace(ctx, ctx.getBlock(pos.add(direction.toBlockVector())))) {
				return false;
			}
		}

		BlockState blockAbove = ctx.getBlock(pos.add(0, 1, 0));
		if (stemBlock.canProduce(blockAbove) || flowerBlock.canProduce(blockAbove)) {
			return canReplace(ctx, blockAbove);
		}

		return true;
	}

	@Override
	public boolean place(CaveGenContext ctx, BlockVector3 pos, Centroid centroid, boolean force) throws WorldEditException {
		pos = pos.add(0, 1, 0);
		if (!canReplace(ctx, ctx.getBlock(pos))
				|| !canReplace(ctx, ctx.getBlock(pos.add(0, 1, 0)))
				|| !isSurroundedByAir(ctx, pos.add(0, 1, 0), null)
		) {
			return false;
		}

		ctx.setBlock(pos, withConnectionProperties(ctx, pos, stemBlock.get(ctx, centroid)));
		int radius = minRadius + ctx.rand.nextInt(maxRadius - minRadius + 1);
		int numLayers = minNumLayers + ctx.rand.nextInt(maxNumLayers - minNumLayers + 1);
		generate(ctx, centroid, pos, pos, radius, 0, numLayers, force);
		return true;
	}

	private void generate(CaveGenContext ctx, Centroid centroid, BlockVector3 pos, BlockVector3 rootPos, int radius, int layer, int numLayers, boolean force) {
		int length = minVLength + ctx.rand.nextInt(maxVLength - minVLength + 1);
		if (layer == 0) {
			length += minInitialHeightBoost + ctx.rand.nextInt(maxInitialHeightBoost - minInitialHeightBoost + 1);
		}

		for (int i = 0; i < length; i++) {
			BlockVector3 offsetPos = pos.add(0, i + 1, 0);
			if (!force && (!canReplace(ctx, ctx.getBlock(offsetPos)) || !isSurroundedByAir(ctx, offsetPos, null))) {
				ctx.setBlock(pos.add(0, i, 0), flowerBlock.get(ctx, centroid));
				return;
			}

			ctx.setBlock(offsetPos, withConnectionProperties(ctx, offsetPos, stemBlock.get(ctx, centroid)));
			BlockVector3 posBelow = offsetPos.add(0, -1, 0);
			ctx.setBlock(posBelow, withConnectionProperties(ctx, posBelow, ctx.getBlock(posBelow)));
		}

		boolean extended = false;
		if (layer < numLayers) {
			double flowerChanceHere = layer == 0 ? initialFlowerChance : flowerChance;
			if (ctx.rand.nextDouble() >= flowerChanceHere) {
				int branchFactor = minBranchFactor + ctx.rand.nextInt(maxBranchFactor - minBranchFactor + 1);
				if (layer == 0) {
					branchFactor += minInitialBranchFactorBoost + ctx.rand.nextInt(maxInitialBranchFactorBoost - minInitialBranchFactorBoost + 1);
				}

				for (int i = 0; i < branchFactor; i++) {
					Direction direction = Direction.valuesOf(Direction.Flag.CARDINAL).get(ctx.rand.nextInt(4));
					int hLength = minHLength + ctx.rand.nextInt(maxHLength - minHLength + 1);
					for (int j = 1; j < hLength; j++) {
						BlockVector3 offsetPos = pos.add(0, length, 0).add(direction.toBlockVector().multiply(j));
						if (Math.abs(offsetPos.getX() - rootPos.getX()) < radius
								&& Math.abs(offsetPos.getZ() - rootPos.getZ()) < radius && canReplace(ctx, ctx.getBlock(offsetPos))
								&& canReplace(ctx, ctx.getBlock(offsetPos.add(0, -1, 0)))
								&& isSurroundedByAir(ctx, offsetPos, Util.getOpposite(direction))) {
							extended = true;
							ctx.setBlock(offsetPos, withConnectionProperties(ctx, offsetPos, stemBlock.get(ctx, centroid)));
							BlockVector3 posBehind = offsetPos.subtract(direction.toBlockVector());
							ctx.setBlock(posBehind, withConnectionProperties(ctx, posBehind, ctx.getBlock(posBehind)));
							if (j == hLength - 1) {
								generate(ctx, centroid, offsetPos, rootPos, radius, layer + 1, numLayers, force);
							}
						} else {
							if (j != 1) {
								ctx.setBlock(offsetPos.subtract(direction.toBlockVector()), flowerBlock.get(ctx, centroid));
							}
							break;
						}
					}
				}
			}
		}

		if (!extended) {
			ctx.setBlock(pos.add(0, length, 0), flowerBlock.get(ctx, centroid));
		}

	}
}
