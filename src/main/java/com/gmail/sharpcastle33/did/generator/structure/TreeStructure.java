package com.gmail.sharpcastle33.did.generator.structure;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeStructure extends Structure {
	private final BlockProvider log;
	private final BlockProvider leaf;
	private final @Nullable BlockProvider dirt;
	private final int minHeight;
	private final int maxHeight;
	private final int minLeafHeight;
	private final int maxLeafHeight;
	private final int topLeafRadius;
	private final int leafStepHeight;
	private final double cornerLeafChance;
	private final @Nullable BlockProvider vine;
	private final BlockPredicate vinesCanReplace;
	private final @Nullable BlockProvider trunkVine;
	private final double trunkVineChance;
	private final BlockProvider hangingVine;
	private final double hangingVineChance;
	private final BlockProvider cocoa;
	private final boolean invertCocoaFacing;
	private final double cocoaChance;
	private final int minCocoaTreeHeight;

	protected TreeStructure(String name, ConfigurationSection map) {
		super(name, StructureType.TREE, map);
		log = map.contains("log") ? ConfigUtil.parseBlockProvider(map.get("log")) : BlockProvider.OAK_LOG;
		leaf = map.contains("leaf") ? ConfigUtil.parseBlockProvider(map.get("leaf")) : BlockProvider.OAK_LEAVES;
		dirt = map.contains("dirt") ? ConfigUtil.parseBlockProvider(map.get("dirt")) : null;
		minHeight = map.getInt("minHeight", 4);
		maxHeight = map.getInt("maxHeight", 6);
		if (minHeight <= 0 || maxHeight < minHeight) {
			throw new InvalidConfigException("Invalid height range");
		}
		minLeafHeight = map.getInt("minLeafHeight", 4);
		maxLeafHeight = map.getInt("maxLeafHeight", 4);
		if (minLeafHeight > minHeight || maxLeafHeight < minLeafHeight) {
			throw new InvalidConfigException("Invalid leaf height range");
		}
		topLeafRadius = map.getInt("topLeafRadius", 1);
		leafStepHeight = map.getInt("leafStepHeight", 2);
		if (leafStepHeight < 1) {
			throw new InvalidConfigException("leafStepHeight must be positive");
		}
		cornerLeafChance = map.getDouble("cornerLeafChance", 0.5);
		vine = map.contains("vine") ? ConfigUtil.parseBlockProvider(map.get("vine")) : null;
		vinesCanReplace = map.contains("vinesCanReplace") ? ConfigUtil.parseBlockPredicate(map.get("vinesCanReplace")) : block -> true;
		trunkVine = map.contains("trunkVine") ? ConfigUtil.parseBlockProvider(map.get("trunkVine")) : vine;
		trunkVineChance = map.getDouble("trunkVineChance", 2.0 / 3);
		hangingVine = map.contains("hangingVine") ? ConfigUtil.parseBlockProvider(map.get("hangingVine")) : vine;
		hangingVineChance = map.getDouble("hangingVineChance", 0.25);
		cocoa = map.contains("cocoa") ? ConfigUtil.parseBlockProvider(map.get("cocoa")) : null;
		invertCocoaFacing = map.getBoolean("invertCocoaFacing", false);
		cocoaChance = map.getDouble("cocoaChance", 0.2);
		minCocoaTreeHeight = map.getInt("minCocoaTreeHeight", 6);
	}

	@Override
	protected boolean shouldTransformBlocksByDefault() {
		return true;
	}

	@Override
	protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
		return Direction.DOWN;
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, Centroid centroid, boolean force) throws WorldEditException {
		pos = pos.add(0, 1, 0);

		int trunkHeight = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);

		// check whether the tree can generate
		if (!force && (pos.getY() < 1 || pos.getY() + trunkHeight + 1 > 256)) {
			return;
		}

		boolean canGenerate = true;
		for (int y = pos.getY(); y <= pos.getY() + 1 + trunkHeight; y++) {
			int leafRadius = 1;
			if (y == pos.getY()) {
				leafRadius = 0;
			}
			if (y >= pos.getY() + 1 + trunkHeight - 2) {
				leafRadius = 2;
			}

			for (int x = pos.getX() - leafRadius; x <= pos.getX() + leafRadius && canGenerate; x++) {
				for (int z = pos.getZ() - leafRadius; z <= pos.getZ() + leafRadius && canGenerate; z++) {
					if (y >= 0 && y < 256) {
						if (!canReplace(ctx, ctx.getBlock(BlockVector3.at(x, y, z)))) {
							canGenerate = false;
						}
					} else {
						canGenerate = false;
					}
				}
			}
		}

		if (!force && !canGenerate) {
			return;
		}

		if (dirt != null) {
			ctx.setBlock(pos.add(0, -1, 0), dirt.get(ctx, centroid));
		}

		int leafHeight = minLeafHeight + ctx.rand.nextInt(maxLeafHeight - minLeafHeight + 1);

		// place leaves
		Set<BlockVector3> leafPositions = new HashSet<>();
		for (int y = pos.getY() - leafHeight + 1 + trunkHeight; y <= pos.getY() + trunkHeight; y++) {
			int yRelativeToTop = y - (pos.getY() + trunkHeight);
			int leafRadius = topLeafRadius - yRelativeToTop / leafStepHeight;

			for (int x = pos.getX() - leafRadius; x <= pos.getX() + leafRadius; x++) {
				int dx = x - pos.getX();
				for (int z = pos.getZ() - leafRadius; z <= pos.getZ() + leafRadius; z++) {
					int dz = z - pos.getZ();
					if (Math.abs(dx) != leafRadius || Math.abs(dz) != leafRadius || ctx.rand.nextDouble() < cornerLeafChance && yRelativeToTop != 0) {
						BlockVector3 leafPos = BlockVector3.at(x, y, z);
						if (canReplace(ctx, ctx.getBlock(leafPos))) {
							ctx.setBlock(leafPos, leaf.get(ctx, centroid));
							leafPositions.add(leafPos);
						}
					}
				}
			}
		}

		// place trunk
		for (int dy = 0; dy < trunkHeight; dy++) {
			if (canReplace(ctx, ctx.getBlock(pos.add(0, dy, 0)))) {
				ctx.setBlock(pos.add(0, dy, 0), this.log.get(ctx, centroid));

				if (trunkVine != null && dy > 0) {
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(-1, dy, 0)))) {
						placeVine(ctx, pos.add(-1, dy, 0), trunkVine.get(ctx, centroid), PropertyKey.EAST);
					}
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(1, dy, 0)))) {
						placeVine(ctx, pos.add(1, dy, 0), trunkVine.get(ctx, centroid), PropertyKey.WEST);
					}
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(0, dy, -1)))) {
						placeVine(ctx, pos.add(0, dy, -1), trunkVine.get(ctx, centroid), PropertyKey.SOUTH);
					}
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(0, dy, 1)))) {
						placeVine(ctx, pos.add(0, dy, 1), trunkVine.get(ctx, centroid), PropertyKey.NORTH);
					}
				}
			}
		}

		// place hanging vines
		if (hangingVine != null) {
			for (int y = pos.getY() - leafHeight + 1 + trunkHeight; y <= pos.getY() + trunkHeight; y++) {
				int yRelativeToTop = y - (pos.getY() + trunkHeight);
				int vineRadius = topLeafRadius + 1 - yRelativeToTop / leafStepHeight;

				for (int x = pos.getX() - vineRadius; x <= pos.getX() + vineRadius; x++) {
					for (int z = pos.getZ() - vineRadius; z <= pos.getZ() + vineRadius; z++) {
						BlockVector3 vinePos = BlockVector3.at(x, y, z);
						if (leafPositions.contains(vinePos)) {
							BlockVector3 vinePostWest = vinePos.add(-1, 0, 0);
							BlockVector3 vinePosEast = vinePos.add(1, 0, 0);
							BlockVector3 vinePosNorth = vinePos.add(0, 0, -1);
							BlockVector3 vinePosSouth = vinePos.add(0, 0, 1);

							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePostWest))) {
								placeHangingVine(ctx, centroid, vinePostWest, PropertyKey.EAST);
							}
							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePosEast))) {
								placeHangingVine(ctx, centroid, vinePosEast, PropertyKey.WEST);
							}
							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePosNorth))) {
								placeHangingVine(ctx, centroid, vinePosNorth, PropertyKey.SOUTH);
							}
							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePosSouth))) {
								placeHangingVine(ctx, centroid, vinePosSouth, PropertyKey.NORTH);
							}
						}
					}
				}
			}
		}

		// place cocoa
		if (cocoa != null) {
			if (ctx.rand.nextDouble() < cocoaChance && trunkHeight >= minCocoaTreeHeight) {
				for (int cocoaDy = 0; cocoaDy < minCocoaTreeHeight - leafHeight; cocoaDy++) {
					for (Direction dir : Direction.valuesOf(Direction.Flag.CARDINAL)) {
						if (ctx.rand.nextInt(4 - cocoaDy) == 0) {
							BlockStateHolder<?> cocoa = this.cocoa.get(ctx, centroid);
							Direction cocoaAttachDir = Util.getOpposite(dir);
							int age = Integer.MIN_VALUE;
							if (cocoa.getBlockType().hasProperty(PropertyKey.AGE)) {
								List<Integer> validAges = cocoa.getBlockType().<Integer>getProperty(PropertyKey.AGE).getValues();
								age = validAges.get(ctx.rand.nextInt(validAges.size()));
							}
							placeCocoa(ctx, cocoa, age, pos.add(cocoaAttachDir.getBlockX(), trunkHeight - minCocoaTreeHeight + cocoaDy + 1, cocoaAttachDir.getBlockZ()), dir);
						}
					}
				}
			}
		}
	}

	private boolean vinesCanReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		return canReplace(ctx, block) && vinesCanReplace.test(block);
	}

	private void placeVine(CaveGenContext ctx, BlockVector3 pos, BlockStateHolder<?> vineBlock, PropertyKey attachProp) {
		if (vineBlock.getBlockType().hasProperty(attachProp)) {
			ctx.setBlock(pos, vineBlock.with(attachProp, true));
		} else {
			ctx.setBlock(pos, vineBlock);
		}
	}

	private void placeHangingVine(CaveGenContext ctx, Centroid centroid, BlockVector3 pos, PropertyKey prop) {
		assert hangingVine != null;
		placeVine(ctx, pos, hangingVine.get(ctx, centroid), prop);
		BlockVector3 vinePos = pos.add(0, -1, 0);
		for (int y = 4; vinesCanReplace(ctx, ctx.getBlock(vinePos)) && y > 0; y--) {
			placeVine(ctx, vinePos, hangingVine.get(ctx, centroid), prop);
			vinePos = vinePos.add(0, -1, 0);
		}
	}

	private void placeCocoa(CaveGenContext ctx, BlockStateHolder<?> cocoa, int age, BlockVector3 pos, Direction dir) {
		if (age != Integer.MIN_VALUE) {
			cocoa = cocoa.with(PropertyKey.AGE, age);
		}
		if (cocoa.getBlockType().hasProperty(PropertyKey.FACING)) {
			cocoa = cocoa.with(PropertyKey.FACING, invertCocoaFacing ? Util.getOpposite(dir) : dir);
		}
		ctx.setBlock(pos, cocoa);
	}
}
