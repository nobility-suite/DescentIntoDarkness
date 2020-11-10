package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class TreeStructure extends Structure {
	private final BlockStateHolder<?> log;
	private final BlockStateHolder<?> leaf;
	private final BlockStateHolder<?> dirt;
	private final int minHeight;
	private final int maxHeight;
	private final int minLeafHeight;
	private final int maxLeafHeight;
	private final int topLeafRadius;
	private final int leafStepHeight;
	private final double cornerLeafChance;
	private final BlockStateHolder<?> vine;
	private final List<BlockStateHolder<?>> vinesCanReplace;
	private final BlockStateHolder<?> trunkVine;
	private final double trunkVineChance;
	private final BlockStateHolder<?> hangingVine;
	private final double hangingVineChance;
	private final BlockStateHolder<?> cocoa;
	private final boolean invertCocoaFacing;
	private final double cocoaChance;
	private final int minCocoaTreeHeight;

	protected TreeStructure(String name, List<StructurePlacementEdge> edges, double chance,
							int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace,
							List<String> tags, boolean tagsInverted, BlockStateHolder<?> log, BlockStateHolder<?> leaf,
							BlockStateHolder<?> dirt, int minHeight, int maxHeight, int minLeafHeight, int maxLeafHeight,
							int topLeafRadius, int leafStepHeight, double cornerLeafChance, BlockStateHolder<?> vine,
							List<BlockStateHolder<?>> vinesCanReplace, BlockStateHolder<?> trunkVine, double trunkVineChance,
							BlockStateHolder<?> hangingVine, double hangingVineChance, BlockStateHolder<?> cocoa,
							boolean invertCocoaFacing, double cocoaChance, int minCocoaTreeHeight) {
		super(name, StructureType.TREE, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
		this.log = log;
		this.leaf = leaf;
		this.dirt = dirt;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.minLeafHeight = minLeafHeight;
		this.maxLeafHeight = maxLeafHeight;
		this.topLeafRadius = topLeafRadius;
		this.leafStepHeight = leafStepHeight;
		this.cornerLeafChance = cornerLeafChance;
		this.vine = vine;
		this.vinesCanReplace = vinesCanReplace;
		this.trunkVine = trunkVine;
		this.trunkVineChance = trunkVineChance;
		this.hangingVine = hangingVine;
		this.hangingVineChance = hangingVineChance;
		this.cocoa = cocoa;
		this.invertCocoaFacing = invertCocoaFacing;
		this.cocoaChance = cocoaChance;
		this.minCocoaTreeHeight = minCocoaTreeHeight;
	}

	protected TreeStructure(String name, ConfigurationSection map) {
		super(name, StructureType.TREE, map);
		log = map.contains("log") ? ConfigUtil.parseBlock(map.getString("log")) : Util.requireDefaultState(BlockTypes.OAK_LOG);
		leaf = map.contains("leaf") ? ConfigUtil.parseBlock(map.getString("leaf")) : Util.requireDefaultState(BlockTypes.OAK_LEAVES).with(PropertyKey.PERSISTENT, true);
		dirt = map.contains("dirt") ? ConfigUtil.parseBlock(map.getString("dirt")) : null;
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
		vine = map.contains("vine") ? ConfigUtil.parseBlock(map.getString("vine")) : null;
		vinesCanReplace = ConfigUtil.deserializeSingleableList(map.get("vinesCanReplace"), ConfigUtil::parseBlock, () -> null);
		trunkVine = map.contains("trunkVine") ? ConfigUtil.parseBlock(map.getString("trunkVine")) : vine;
		trunkVineChance = map.getDouble("trunkVineChance", 2.0 / 3);
		hangingVine = map.contains("hangingVine") ? ConfigUtil.parseBlock(map.getString("hangingVine")) : vine;
		hangingVineChance = map.getDouble("hangingVineChance", 0.25);
		cocoa = map.contains("cocoa") ? ConfigUtil.parseBlock(map.getString("cocoa")) : null;
		invertCocoaFacing = map.getBoolean("invertCocoaFacing", false);
		cocoaChance = map.getDouble("cocoaChance", 0.2);
		minCocoaTreeHeight = map.getInt("minCocoaTreeHeight", 6);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("log", ConfigUtil.serializeBlock(log));
		map.set("leaf", ConfigUtil.serializeBlock(leaf));
		if (dirt != null) {
			map.set("dirt", ConfigUtil.serializeBlock(dirt));
		}
		map.set("minHeight", minHeight);
		map.set("maxHeight", maxHeight);
		map.set("minLeafHeight", minLeafHeight);
		map.set("maxLeafHeight", maxLeafHeight);
		map.set("topLeafRadius", topLeafRadius);
		map.set("leafStepHeight", leafStepHeight);
		map.set("cornerLeafChance", cornerLeafChance);
		if (vine != null) {
			map.set("vine", ConfigUtil.serializeBlock(vine));
		}
		if (vinesCanReplace != null) {
			map.set("vinesCanReplace", ConfigUtil.serializeSingleableList(vinesCanReplace, BlockStateHolder::getAsString));
		}
		if (trunkVine != null) {
			map.set("trunkVine", trunkVine);
		}
		map.set("trunkVineChance", trunkVineChance);
		if (hangingVine != null) {
			map.set("hangingVine", hangingVine);
		}
		map.set("hangingVineChance", hangingVineChance);
		if (cocoa != null) {
			map.set("cocoa", ConfigUtil.serializeBlock(cocoa));
		}
		map.set("invertCocoaFacing", invertCocoaFacing);
		map.set("cocoaChance", cocoaChance);
		map.set("minCocoaTreeHeight", minCocoaTreeHeight);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, Direction side, boolean force) throws WorldEditException {
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
			ctx.setBlock(pos.add(0, -1, 0), dirt);
		}

		int leafHeight = minLeafHeight + ctx.rand.nextInt(maxLeafHeight - minLeafHeight + 1);

		// place leaves
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
							ctx.setBlock(leafPos, leaf);
						}
					}
				}
			}
		}

		// place trunk
		for (int dy = 0; dy < trunkHeight; dy++) {
			if (canReplace(ctx, ctx.getBlock(pos.add(0, dy, 0)))) {
				ctx.setBlock(pos.add(0, dy, 0), this.log);

				if (trunkVine != null && dy > 0) {
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(-1, dy, 0)))) {
						placeVine(ctx, pos.add(-1, dy, 0), trunkVine, PropertyKey.EAST);
					}
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(1, dy, 0)))) {
						placeVine(ctx, pos.add(1, dy, 0), trunkVine, PropertyKey.WEST);
					}
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(0, dy, -1)))) {
						placeVine(ctx, pos.add(0, dy, -1), trunkVine, PropertyKey.SOUTH);
					}
					if (ctx.rand.nextDouble() < trunkVineChance && vinesCanReplace(ctx, ctx.getBlock(pos.add(0, dy, 1)))) {
						placeVine(ctx, pos.add(0, dy, 1), trunkVine, PropertyKey.NORTH);
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
						if (leaf.equalsFuzzy(ctx.getBlock(vinePos))) {
							BlockVector3 vinePostWest = vinePos.add(-1, 0, 0);
							BlockVector3 vinePosEast = vinePos.add(1, 0, 0);
							BlockVector3 vinePosNorth = vinePos.add(0, 0, -1);
							BlockVector3 vinePosSouth = vinePos.add(0, 0, 1);

							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePostWest))) {
								placeHangingVine(ctx, vinePostWest, PropertyKey.EAST);
							}
							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePosEast))) {
								placeHangingVine(ctx, vinePosEast, PropertyKey.WEST);
							}
							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePosNorth))) {
								placeHangingVine(ctx, vinePosNorth, PropertyKey.SOUTH);
							}
							if (ctx.rand.nextDouble() < hangingVineChance && vinesCanReplace(ctx, ctx.getBlock(vinePosSouth))) {
								placeHangingVine(ctx, vinePosSouth, PropertyKey.NORTH);
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
							Direction cocoaAttachDir = Util.getOpposite(dir);
							int age = Integer.MIN_VALUE;
							if (cocoa.getBlockType().hasProperty(PropertyKey.AGE)) {
								List<Integer> validAges = cocoa.getBlockType().<Integer>getProperty(PropertyKey.AGE).getValues();
								age = validAges.get(ctx.rand.nextInt(validAges.size()));
							}
							placeCocoa(ctx, age, pos.add(cocoaAttachDir.getBlockX(), trunkHeight - minCocoaTreeHeight + cocoaDy + 1, cocoaAttachDir.getBlockZ()), dir);
						}
					}
				}
			}
		}
	}

	private boolean vinesCanReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		return canReplace(ctx, block) && (vinesCanReplace == null || vinesCanReplace.stream().anyMatch(it -> it.equalsFuzzy(block)));
	}

	private void placeVine(CaveGenContext ctx, BlockVector3 pos, BlockStateHolder<?> vineBlock, PropertyKey attachProp) {
		if (vineBlock.getBlockType().hasProperty(attachProp)) {
			ctx.setBlock(pos, vineBlock.with(attachProp, true));
		} else {
			ctx.setBlock(pos, vineBlock);
		}
	}

	private void placeHangingVine(CaveGenContext worldIn, BlockVector3 pos, PropertyKey prop) {
		placeVine(worldIn, pos, hangingVine, prop);
		BlockVector3 vinePos = pos.add(0, -1, 0);
		for (int y = 4; vinesCanReplace(worldIn, worldIn.getBlock(vinePos)) && y > 0; y--) {
			placeVine(worldIn, vinePos, hangingVine, prop);
			vinePos = vinePos.add(0, -1, 0);
		}
	}

	private void placeCocoa(CaveGenContext ctx, int age, BlockVector3 pos, Direction dir) {
		BlockStateHolder<?> cocoa = this.cocoa;
		if (age != Integer.MIN_VALUE) {
			cocoa = cocoa.with(PropertyKey.AGE, age);
		}
		if (cocoa.getBlockType().hasProperty(PropertyKey.FACING)) {
			cocoa = cocoa.with(PropertyKey.FACING, invertCocoaFacing ? Util.getOpposite(dir) : dir);
		}
		ctx.setBlock(pos, cocoa);
	}
}
