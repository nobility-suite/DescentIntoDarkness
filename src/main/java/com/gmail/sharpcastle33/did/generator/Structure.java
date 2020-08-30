package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Structure {
	private final String name;
	private final Type type;
	protected final List<Edge> edges;
	private final double chance;
	private final int count;
	protected final List<BlockStateHolder<?>> canPlaceOn;
	protected final List<BlockStateHolder<?>> canReplace;
	private final List<Direction> validDirections = new ArrayList<>();
	private final List<String> tags;
	private final boolean tagsInverted;

	protected Structure(String name, Type type, ConfigurationSection map) {
		this.name = name;
		this.type = type;
		this.edges = ConfigUtil.deserializeSingleableList(map.get("edges"), val -> ConfigUtil.parseEnum(Edge.class, val), () -> Lists.newArrayList(Edge.values()));
		this.chance = map.getDouble("chance", 1);
		this.count = map.getInt("count", 1);
		this.canPlaceOn = deserializePlacementRule(map.get("canPlaceOn"));
		this.canReplace = deserializePlacementRule(map.get("canReplace"));
		this.tags = ConfigUtil.deserializeSingleableList(map.get("tags"), Function.identity(), ArrayList::new);
		this.tagsInverted = map.getBoolean("tagsInverted", !map.contains("tags"));
		computeValidDirections();
	}

	protected Structure(String name, Type type, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted) {
		this.name = name;
		this.type = type;
		this.edges = edges;
		this.chance = chance;
		this.count = count;
		this.canPlaceOn = canPlaceOn;
		this.canReplace = canReplace;
		this.tags = tags;
		this.tagsInverted = tagsInverted;
		computeValidDirections();
	}

	private void computeValidDirections() {
		if (edges.isEmpty()) {
			throw new InvalidConfigException("No edges to choose from");
		}
		for (Edge edge : edges) {
			Collections.addAll(validDirections, edge.directions);
		}
	}

	public final String getName() {
		return name;
	}

	public final Type getType() {
		return type;
	}

	public List<Direction> getValidDirections() {
		return validDirections;
	}

	public double getChance() {
		return chance;
	}

	public int getCount() {
		return count;
	}

	public boolean canPlaceOn(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canPlaceOn == null) {
			return !ctx.style.isTransparentBlock(block);
		} else {
			return canPlaceOn.stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

	public List<String> getTags() {
		return tags;
	}

	public boolean areTagsInverted() {
		return tagsInverted;
	}

	public void serialize(ConfigurationSection map) {
		map.set("type", ConfigUtil.enumToString(type));
		map.set("edges", ConfigUtil.serializeSingleableList(edges, ConfigUtil::enumToString));
		map.set("chance", chance);
		map.set("count", count);
		if (canPlaceOn != null) {
			map.set("canPlaceOn", ConfigUtil.serializeSingleableList(canPlaceOn, BlockStateHolder::getAsString));
		}
		if (canReplace != null) {
			map.set("canReplace", ConfigUtil.serializeSingleableList(canReplace, BlockStateHolder::getAsString));
		}
		if (!tags.isEmpty()) {
			map.set("tags", ConfigUtil.serializeSingleableList(tags, Function.identity()));
		}
		serialize0(map);
	}

	protected abstract void serialize0(ConfigurationSection map);

	public static Structure deserialize(String name, ConfigurationSection map) {
		Type type = ConfigUtil.parseEnum(Type.class, ConfigUtil.requireString(map, "type"));
		return type.deserialize(name, map);
	}

	protected static List<Edge> deserializeEdges(Object edges) {
		List<Edge> ret = ConfigUtil.deserializeSingleableList(edges, val -> ConfigUtil.parseEnum(Edge.class, val), () -> Lists.newArrayList(Edge.values()));
		if (ret.isEmpty()) {
			throw new InvalidConfigException("No edges to choose from");
		}
		return ret;
	}

	protected static List<BlockStateHolder<?>> deserializePlacementRule(Object rule) {
		return ConfigUtil.deserializeSingleableList(rule, ConfigUtil::parseBlock, () -> null);
	}

	public abstract void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException;

	protected boolean canReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canReplace == null) {
			return ctx.style.isTransparentBlock(block);
		} else {
			return canReplace.stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

	public static class SchematicStructure extends Structure {
		private final List<Schematic> schematics;
		private final Direction originSide;
		private final boolean ignoreAir;
		private final boolean randomRotation;

		public SchematicStructure(String name, ConfigurationSection map) {
			super(name, Type.SCHEMATIC, map);
			this.schematics = ConfigUtil.deserializeSingleableList(ConfigUtil.require(map, "schematics"), schematicName -> {
				Clipboard data = DescentIntoDarkness.plugin.getSchematic(schematicName);
				if (data == null) {
					throw new InvalidConfigException("Unknown schematic: " + schematicName);
				}
				return new SchematicStructure.Schematic(schematicName, data);
			}, () -> null);
			String originSideVal = map.getString("originSide");
			if (originSideVal == null) {
				this.originSide = Direction.DOWN;
			} else {
				this.originSide = ConfigUtil.parseEnum(Direction.class, originSideVal);
				if (!originSide.isCardinal() && !originSide.isUpright()) {
					throw new InvalidConfigException("Invalid Direction: " + originSideVal);
				}
			}
			this.ignoreAir = map.getBoolean("ignoreAir", true);
			this.randomRotation = map.getBoolean("randomRotation", true);
		}

		public SchematicStructure(String name, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, List<Schematic> schematics, Direction originSide, boolean ignoreAir, boolean randomRotation) {
			super(name, Type.SCHEMATIC, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
			this.schematics = schematics;
			this.originSide = originSide;
			this.ignoreAir = ignoreAir;
			this.randomRotation = randomRotation;
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			map.set("schematics", ConfigUtil.serializeSingleableList(schematics, schematic -> schematic.name));
			map.set("originSide", ConfigUtil.enumToString(originSide));
			map.set("ignoreAir", ignoreAir);
			map.set("randomRotation", randomRotation);
		}

		@Override
		public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
			Schematic chosenSchematic = schematics.get(ctx.rand.nextInt(schematics.size()));
			ClipboardHolder clipboardHolder = new ClipboardHolder(chosenSchematic.data);
			AffineTransform transform = new AffineTransform();

			if (side.isUpright() && randomRotation) {
				transform = transform.rotateY(ctx.rand.nextInt(4) * 90);
			}

			if (side != originSide) {
				if (originSide == Direction.DOWN) {
					switch (side) {
						case UP: transform = transform.rotateX(180); break;
						case NORTH: transform = transform.rotateX(-90); break;
						case SOUTH: transform = transform.rotateX(90); break;
						case WEST: transform = transform.rotateZ(90); break;
						case EAST: transform = transform.rotateZ(-90); break;
						default: throw new AssertionError("There are too many directions!");
					}
				} else if (originSide == Direction.UP) {
					switch (side) {
						case DOWN: transform = transform.rotateX(180); break;
						case NORTH: transform = transform.rotateX(90); break;
						case SOUTH: transform = transform.rotateX(-90); break;
						case WEST: transform = transform.rotateZ(-90); break;
						case EAST: transform = transform.rotateZ(90); break;
						default: throw new AssertionError("There are too many directions!");
					}
				} else {
					if (side.isCardinal()) {
						transform = transform.rotateY(originSide.toBlockVector().toYaw() - side.toBlockVector().toYaw());
					} else if (side == Direction.DOWN) {
						switch (originSide) {
							case NORTH: transform = transform.rotateX(90); break;
							case SOUTH: transform = transform.rotateX(-90); break;
							case WEST: transform = transform.rotateZ(-90); break;
							case EAST: transform = transform.rotateZ(90); break;
							default: throw new AssertionError("There are too many directions!");
						}
					} else {
						switch (originSide) {
							case NORTH: transform = transform.rotateX(-90); break;
							case SOUTH: transform = transform.rotateX(90); break;
							case WEST: transform = transform.rotateZ(90); break;
							case EAST: transform = transform.rotateZ(-90); break;
							default: throw new AssertionError("There are too many directions!");
						}
					}
				}
			}

			clipboardHolder.setTransform(transform);
			BlockVector3 to = pos.subtract(side.toBlockVector());
			if (canPlace(ctx, to, chosenSchematic.data, clipboardHolder.getTransform())) {
				Operation paste = clipboardHolder.createPaste(ctx.asExtent()).to(to).ignoreAirBlocks(ignoreAir).build();
				Operations.complete(paste);
				if (ctx.isDebug()) {
					ctx.setBlock(to, Util.requireDefaultState(BlockTypes.DIAMOND_BLOCK));
				}
			}
		}

		private boolean canPlace(CaveGenContext ctx, BlockVector3 to, Clipboard schematic, Transform transform) {
			for (BlockVector3 pos : schematic.getRegion()) {
				if (schematic.getBlock(pos).getBlockType() == BlockTypes.AIR) {
					continue;
				}
				BlockVector3 destPos = transform.apply(pos.subtract(schematic.getOrigin()).toVector3()).toBlockPoint().add(to);
				BlockStateHolder<?> block = ctx.getBlock(destPos);
				if (!canReplace(ctx, block)) {
					return false;
				}
			}
			return true;
		}

		public static class Schematic {
			private final String name;
			private final Clipboard data;

			private Schematic(String name, Clipboard data) {
				this.name = name;
				this.data = data;
			}
		}
	}

	public static class VeinStructure extends Structure {
		private final BlockStateHolder<?> ore;
		private final int radius;

		public VeinStructure(String name, ConfigurationSection map) {
			super(name, Type.VEIN, map);
			this.ore = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "ore"));
			this.radius = map.getInt("radius", 4);
		}

		public VeinStructure(String name, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, BlockStateHolder<?> ore, int radius) {
			super(name, Type.VEIN, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
			this.ore = ore;
			this.radius = radius;
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			map.set("ore", ore.getAsString());
			map.set("radius", radius);
		}

		@Override
		public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
			ModuleGenerator.generateOreCluster(ctx, pos, radius, canReplace, ore);
		}
	}

	public static abstract class AbstractPatchStructure extends Structure {
		private final int spreadX;
		private final int spreadY;
		private final int spreadZ;
		private final int tries;

		protected AbstractPatchStructure(String name, Type type, ConfigurationSection map) {
			super(name, type, map);
			this.spreadX = map.getInt("spreadX", 8);
			this.spreadY = map.getInt("spreadY", 4);
			this.spreadZ = map.getInt("spreadZ", 8);
			if (spreadX < 0 || spreadY < 0 || spreadZ < 0) {
				throw new InvalidConfigException("Spread cannot be negative");
			}
			this.tries = map.getInt("tries", 64);
		}

		protected AbstractPatchStructure(String name, Type type, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, int spreadX, int spreadY, int spreadZ, int tries) {
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
		public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
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

	public static class PatchStructure extends AbstractPatchStructure {
		private final BlockStateHolder<?> block;

		protected PatchStructure(String name, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, int spreadX, int spreadY, int spreadZ, int tries, BlockStateHolder<?> block) {
			super(name, Type.PATCH, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted, spreadX, spreadY, spreadZ, tries);
			this.block = block;
		}

		protected PatchStructure(String name, ConfigurationSection map) {
			super(name, Type.PATCH, map);
			this.block = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "block"));
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			super.serialize0(map);
			map.set("block", block.getAsString());
		}

		@Override
		protected void doPlace(CaveGenContext ctx, BlockVector3 pos, Direction side) {
			ctx.setBlock(pos, block);
		}
	}

	public static class VinePatchStructure extends AbstractPatchStructure {
		private final BlockStateHolder<?> vine;
		private final BlockStateHolder<?> firstBlock;
		private final BlockStateHolder<?> lastBlock;
		private final int minHeight;
		private final int maxHeight;
		private final boolean randomRotation;

		protected VinePatchStructure(String name, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, int spreadX, int spreadY, int spreadZ, int tries, BlockStateHolder<?> vine, BlockStateHolder<?> firstBlock, BlockStateHolder<?> lastBlock, int minHeight, int maxHeight, boolean randomRotation) {
			super(name, Type.VINE_PATCH, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted, spreadX, spreadY, spreadZ, tries);
			this.vine = vine;
			this.firstBlock = firstBlock;
			this.lastBlock = lastBlock;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
			this.randomRotation = randomRotation;
		}

		protected VinePatchStructure(String name, ConfigurationSection map) {
			super(name, Type.VINE_PATCH, map);
			this.vine = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "vine"));
			this.firstBlock = ConfigUtil.parseBlock(map.getString("firstBlock", vine.getAsString()));
			this.lastBlock = ConfigUtil.parseBlock(map.getString("lastBlock", vine.getAsString()));
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
			map.set("vine", vine.getAsString());
			map.set("firstBlock", firstBlock.getAsString());
			map.set("lastBlock", lastBlock.getAsString());
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

	public static class GlowstoneStructure extends Structure {
		private final int density;
		private final int spreadX;
		private final int height;
		private final int spreadZ;
		private final BlockStateHolder<?> block;

		public GlowstoneStructure(String name, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, int density, int spreadX, int height, int spreadZ, BlockStateHolder<?> block) {
			super(name, Type.GLOWSTONE, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
			this.density = density;
			this.spreadX = spreadX;
			this.height = height;
			this.spreadZ = spreadZ;
			this.block = block;
		}

		public GlowstoneStructure(String name, ConfigurationSection map) {
			super(name, Type.GLOWSTONE, map);
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
		protected void serialize0(ConfigurationSection map) {
			map.set("density", density);
			map.set("spreadX", spreadX);
			map.set("height", height);
			map.set("spreadZ", spreadZ);
			map.set("block", block.getAsString());
		}

		@Override
		public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
			Direction xAxis = side.isUpright() ? Direction.EAST : side.getRight();
			Direction zAxis = Direction.findClosest(side.toVector().cross(xAxis.toVector()), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert zAxis != null;

			ctx.setBlock(pos, block);
			for (int i = 0; i < density; i++) {
				BlockVector3 offsetPos = pos.add(
						xAxis.toBlockVector().multiply(ctx.rand.nextInt(spreadX) - ctx.rand.nextInt(spreadX)),
						side.toBlockVector().multiply(-ctx.rand.nextInt(height)),
						zAxis.toBlockVector().multiply(ctx.rand.nextInt(spreadZ) - ctx.rand.nextInt(spreadZ))
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

	public static class WaterfallStructure extends Structure {
		private final FluidType fluid;

		protected WaterfallStructure(String name, List<Edge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted, FluidType fluid) {
			super(name, Type.WATERFALL, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
			this.fluid = fluid;
		}

		protected WaterfallStructure(String name, ConfigurationSection map) {
			super(name, Type.WATERFALL, map);
			this.fluid = ConfigUtil.parseEnum(FluidType.class, map.getString("fluid", FluidType.WATER.name()));
		}

		@Override
		public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
			int wallCount = 0;
			for (Direction dir : Direction.valuesOf(Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT)) {
				if (canPlaceOn(ctx, ctx.getBlock(pos.add(dir.toBlockVector())))) {
					wallCount++;
				}
			}
			if (wallCount != 5) {
				return;
			}

			if (ctx.setBlock(pos, fluid.block)) {
				simulateFluidTick(ctx, pos);
			}
		}

		private void simulateFluidTick(CaveGenContext ctx, BlockVector3 pos) {
			BlockStateHolder<?> state = ctx.getBlock(pos);
			int level = state.<Integer>getState(PropertyKey.LEVEL);
			int levelDecrease = fluid == FluidType.LAVA ? 2 : 1;

			if (level > 0) {
				int minLevel = -100;
				int adjacentSourceBlocks = 0;
				for (Direction dir : Direction.valuesOf(Direction.Flag.CARDINAL)) {
					int depth = getDepth(ctx.getBlock(pos.add(dir.toBlockVector())));
					if (depth >= 0) {
						if (depth == 0) {
							adjacentSourceBlocks++;
						} else if (depth >= 8) {
							depth = 0;
						}
						minLevel = minLevel >= 0 && depth >= minLevel ? minLevel : depth;
					}
				}

				int newLevel = minLevel + levelDecrease;
				if (newLevel >= 8 || minLevel < 0) {
					newLevel = -1;
				}

				int depthAbove = getDepth(ctx.getBlock(pos.add(0, 1, 0)));
				if (depthAbove >= 0) {
					if (depthAbove >= 8) {
						newLevel = depthAbove;
					} else {
						newLevel = depthAbove + 8;
					}
				}

				if (adjacentSourceBlocks >= 2 && fluid == FluidType.WATER) {
					BlockStateHolder<?> stateBelow = ctx.getBlock(pos.add(0, -1, 0));
					if (!canReplace(ctx, stateBelow)) {
						newLevel = 0;
					} else if (stateBelow.getBlockType() == fluid.block.getBlockType() && stateBelow.<Integer>getState(PropertyKey.LEVEL) == 0) {
						newLevel = 0;
					}
				}

				if (newLevel != level) {
					level = newLevel;

					if (newLevel < 0) {
						ctx.setBlock(pos, Util.requireDefaultState(BlockTypes.AIR));
					} else {
						if (ctx.setBlock(pos, fluid.block.with(PropertyKey.LEVEL, newLevel))) {
							simulateFluidTick(ctx, pos);
						}
					}
				}
			}

			BlockVector3 posBelow = pos.add(0, -1, 0);
			BlockStateHolder<?> blockBelow = ctx.getBlock(posBelow);
			if (canFlowInto(ctx, posBelow, blockBelow)) {
				// skipped: trigger mix effects

				if (level >= 8) {
					tryFlowInto(ctx, posBelow, blockBelow, level);
				} else {
					tryFlowInto(ctx, posBelow, blockBelow, level + 8);
				}
			} else if (level >= 0 && (level == 0 || isBlocked(ctx, posBelow, blockBelow))) {
				Set<Direction> flowDirs = getPossibleFlowDirections(ctx, pos, level);
				int newLevel = level + levelDecrease;
				if (level >= 8) {
					newLevel = 1;
				}
				if (newLevel >= 8) {
					return;
				}
				for (Direction flowDir : flowDirs) {
					BlockVector3 offsetPos = pos.add(flowDir.toBlockVector());
					tryFlowInto(ctx, offsetPos, ctx.getBlock(offsetPos), newLevel);
				}
			}
		}

		private int getDepth(BlockStateHolder<?> state) {
			return state.getBlockType() == fluid.block.getBlockType() ? state.<Integer>getState(PropertyKey.LEVEL) : -1;
		}

		private boolean canFlowInto(CaveGenContext ctx, BlockVector3 pos, BlockStateHolder<?> block) {
			BlockType blockType = block.getBlockType();
			return blockType != fluid.block.getBlockType() && blockType != FluidType.LAVA.block.getBlockType() && !isBlocked(ctx, pos, block);
		}

		private void tryFlowInto(CaveGenContext ctx, BlockVector3 pos, BlockStateHolder<?> block, int level) {
			if (!canFlowInto(ctx, pos, block)) {
				return;
			}

			// skipped: trigger mix effects and block dropping
			if (ctx.setBlock(pos, fluid.block.with(PropertyKey.LEVEL, level))) {
				simulateFluidTick(ctx, pos);
			}
		}

		private boolean isBlocked(CaveGenContext ctx, BlockVector3 pos, BlockStateHolder<?> block) {
			return !canReplace(ctx, block) && block.getBlockType() != fluid.block.getBlockType();
		}

		private Set<Direction> getPossibleFlowDirections(CaveGenContext ctx, BlockVector3 pos, int level) {
			int minDistanceToLower = 1000;
			Set<Direction> flowDirs = EnumSet.noneOf(Direction.class);

			for (Direction dir : Direction.valuesOf(Direction.Flag.CARDINAL)) {
				BlockVector3 offsetPos = pos.add(dir.toBlockVector());
				BlockStateHolder<?> offsetState = ctx.getBlock(offsetPos);

				if (!isBlocked(ctx, offsetPos, offsetState) && (offsetState.getBlockType() != fluid.block.getBlockType() || offsetState.<Integer>getState(PropertyKey.LEVEL) > 0)) {
					int distanceToLower;
					BlockVector3 posBelow = offsetPos.add(0, -1, 0);
					if (isBlocked(ctx, posBelow, ctx.getBlock(posBelow))) {
						distanceToLower = getDistanceToLower(ctx, offsetPos, 1, Util.getOpposite(dir));
					} else {
						distanceToLower = 0;
					}

					if (distanceToLower < minDistanceToLower) {
						flowDirs.clear();
					}

					if (distanceToLower <= minDistanceToLower) {
						flowDirs.add(dir);
						minDistanceToLower = distanceToLower;
					}
				}
			}

			return flowDirs;
		}

		private int getDistanceToLower(CaveGenContext ctx, BlockVector3 pos, int distance, Direction excludingDir) {
			int minDistanceToLower = 1000;

			for (Direction dir : Direction.valuesOf(Direction.Flag.CARDINAL)) {
				if (dir == excludingDir) {
					continue;
				}

				BlockVector3 offsetPos = pos.add(dir.toBlockVector());
				BlockStateHolder<?> offsetState = ctx.getBlock(offsetPos);

				if (!isBlocked(ctx, offsetPos, offsetState) && (offsetState.getBlockType() != fluid.block.getBlockType() || offsetState.<Integer>getState(PropertyKey.LEVEL) > 0)) {
					if (!isBlocked(ctx, offsetPos.add(0, -1, 0), offsetState)) {
						return distance;
					}

					if (distance < getSlopeFindDistance()) {
						int distanceToLower = getDistanceToLower(ctx, offsetPos, distance + 1, Util.getOpposite(dir));
						if (distanceToLower < minDistanceToLower) {
							minDistanceToLower = distanceToLower;
						}
					}
				}
			}

			return minDistanceToLower;
		}

		private int getSlopeFindDistance() {
			return fluid == FluidType.LAVA ? 2 : 4;
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			map.set("fluid", ConfigUtil.enumToString(fluid));
		}

		public enum FluidType {
			WATER(Util.requireDefaultState(BlockTypes.WATER)),
			LAVA(Util.requireDefaultState(BlockTypes.LAVA));
			public final BlockStateHolder<?> block;

			FluidType(BlockStateHolder<?> block) {
				this.block = block;
			}
		}
	}

	public enum Type {
		SCHEMATIC(SchematicStructure::new),
		VEIN(VeinStructure::new),
		PATCH(PatchStructure::new),
		VINE_PATCH(VinePatchStructure::new),
		GLOWSTONE(GlowstoneStructure::new),
		WATERFALL(WaterfallStructure::new),
		;

		private final BiFunction<String, ConfigurationSection, Structure> deserializer;
		Type(BiFunction<String, ConfigurationSection, Structure> deserializer) {
			this.deserializer = deserializer;
		}

		public Structure deserialize(String name, ConfigurationSection map) {
			return deserializer.apply(name, map);
		}
	}

	public enum Edge {
		FLOOR(Direction.DOWN), CEILING(Direction.UP), WALL(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
		private final Direction[] directions;
		Edge(Direction... directions) {
			this.directions = directions;
		}
	}
}
