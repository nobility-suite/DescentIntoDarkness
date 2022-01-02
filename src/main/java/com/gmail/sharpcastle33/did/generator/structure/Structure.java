package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.provider.BlockPredicate;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class Structure {
	private final String name;
	private final StructureType type;
	protected final List<StructurePlacementEdge> edges;
	private final double count;
	private final @Nullable BlockPredicate canPlaceOn;
	private final @Nullable BlockPredicate canReplace;
	private final List<Direction> validDirections = new ArrayList<>();
	private final boolean snapToAxis;
	private final Direction originSide;
	private final boolean shouldTransformBlocks;
	private final boolean shouldTransformPosition;
	private final boolean randomRotation;
	private final List<String> tags;
	private final boolean tagsInverted;

	protected Structure(String name, StructureType type, ConfigurationSection map) {
		this.name = name;
		this.type = type;
		this.edges = ConfigUtil.deserializeSingleableList(map.get("edges"), val -> ConfigUtil.parseEnum(StructurePlacementEdge.class, val), this::getDefaultEdges);
		if (map.contains("chance")) {
			if (map.contains("count")) {
				throw new InvalidConfigException("Structure contains both \"chance\" and \"count\"");
			}
			double chance = map.getDouble("chance", 1);
			if (chance < 0 || chance > 0.999) {
				throw new InvalidConfigException("Structure chance must be between 0 <= chance < 1");
			}
			// 1 - chance = e^(-count)
			// -count = ln(1 - chance)
			this.count = -Math.log(1 - chance);
		} else {
			this.count = map.getDouble("count", 1);
		}
		this.canPlaceOn = map.contains("canPlaceOn") ? ConfigUtil.parseBlockPredicate(map.get("canPlaceOn")) : null;
		this.canReplace = map.contains("canReplace") ? ConfigUtil.parseBlockPredicate(map.get("canReplace")) : null;
		String originSideVal = map.getString("originSide");
		if (originSideVal == null) {
			this.originSide = getDefaultOriginSide(edges);
		} else {
			this.originSide = ConfigUtil.parseEnum(Direction.class, originSideVal);
			if (!originSide.isCardinal() && !originSide.isUpright()) {
				throw new InvalidConfigException("Invalid Direction: " + originSideVal);
			}
		}
		this.snapToAxis = map.getBoolean("snapToAxis", shouldSnapToAxisByDefault());
		this.shouldTransformBlocks = map.getBoolean("shouldTransformBlocks", shouldTransformBlocksByDefault());
		this.shouldTransformPosition = map.getBoolean("shouldTransformPosition", shouldTransformPositionByDefault());
		this.randomRotation = map.getBoolean("randomRotation", true);
		this.tags = ConfigUtil.deserializeSingleableList(map.get("tags"), Function.identity(), ArrayList::new);
		this.tagsInverted = map.getBoolean("tagsInverted", !map.contains("tags"));
		computeValidDirections();
	}

	private void computeValidDirections() {
		if (edges.isEmpty()) {
			throw new InvalidConfigException("No edges to choose from");
		}
		for (StructurePlacementEdge edge : edges) {
			Collections.addAll(validDirections, edge.getDirections());
		}
	}

	public final String getName() {
		return name;
	}

	public final StructureType getType() {
		return type;
	}

	public List<Direction> getValidDirections() {
		return validDirections;
	}

	public double getCount() {
		return count;
	}

	public boolean canPlaceOn(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canPlaceOn == null) {
			return !ctx.style.isTransparentBlock(block);
		} else {
			return canPlaceOn.test(block);
		}
	}

	protected List<StructurePlacementEdge> getDefaultEdges() {
		return Lists.newArrayList(StructurePlacementEdge.values());
	}

	protected boolean shouldTransformBlocksByDefault() {
		return false;
	}

	protected boolean shouldTransformPositionByDefault() {
		return true;
	}

	protected boolean shouldSnapToAxisByDefault() {
		return false;
	}

	public boolean shouldSnapToAxis() {
		return snapToAxis;
	}

	protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
		if (edges.contains(StructurePlacementEdge.FLOOR)) {
			return Direction.DOWN;
		} else if (edges.contains(StructurePlacementEdge.CEILING)) {
			return Direction.UP;
		} else {
			return Direction.SOUTH;
		}
	}

	public Direction getOriginSide() {
		return originSide;
	}

	protected Direction getOriginPositionSide() {
		return Direction.DOWN;
	}

	public Transform getBlockTransform(int randomYRotation, BlockVector3 pos, Direction side) {
		if (!shouldTransformBlocks) {
			return new Identity();
		}
		return getTransform(randomYRotation, pos, originSide, side);
	}

	public Transform getPositionTransform(int randomYRotation, BlockVector3 pos, Direction side) {
		if (!shouldTransformPosition) {
			return new Identity();
		}
		return getTransform(randomYRotation, pos, getOriginPositionSide(), side);
	}

	private Transform getTransform(int randomYRotation, BlockVector3 pos, Direction originSide, Direction side) {
		AffineTransform transform = new AffineTransform();

		transform = transform.translate(pos);

		if (side.isUpright() && randomRotation) {
			transform = transform.rotateY(randomYRotation);
		}

		if (side != originSide) {
			if (originSide == Direction.DOWN) {
				switch (side) {
					case UP:
						transform = transform.rotateX(180);
						break;
					case NORTH:
						transform = transform.rotateX(-90);
						break;
					case SOUTH:
						transform = transform.rotateX(90);
						break;
					case WEST:
						transform = transform.rotateZ(90);
						break;
					case EAST:
						transform = transform.rotateZ(-90);
						break;
					default:
						throw new AssertionError("There are too many directions!");
				}
			} else if (originSide == Direction.UP) {
				switch (side) {
					case DOWN:
						transform = transform.rotateX(180);
						break;
					case NORTH:
						transform = transform.rotateX(90);
						break;
					case SOUTH:
						transform = transform.rotateX(-90);
						break;
					case WEST:
						transform = transform.rotateZ(-90);
						break;
					case EAST:
						transform = transform.rotateZ(90);
						break;
					default:
						throw new AssertionError("There are too many directions!");
				}
			} else {
				if (side.isCardinal()) {
					transform = transform.rotateY(originSide.toBlockVector().toYaw() - side.toBlockVector().toYaw());
				} else if (side == Direction.DOWN) {
					switch (originSide) {
						case NORTH:
							transform = transform.rotateX(90);
							break;
						case SOUTH:
							transform = transform.rotateX(-90);
							break;
						case WEST:
							transform = transform.rotateZ(-90);
							break;
						case EAST:
							transform = transform.rotateZ(90);
							break;
						default:
							throw new AssertionError("There are too many directions!");
					}
				} else {
					switch (originSide) {
						case NORTH:
							transform = transform.rotateX(-90);
							break;
						case SOUTH:
							transform = transform.rotateX(90);
							break;
						case WEST:
							transform = transform.rotateZ(90);
							break;
						case EAST:
							transform = transform.rotateZ(-90);
							break;
						default:
							throw new AssertionError("There are too many directions!");
					}
				}
			}
		}

		transform = transform.translate(pos.multiply(-1));

		return transform;
	}

	public List<String> getTags() {
		return tags;
	}

	public boolean areTagsInverted() {
		return tagsInverted;
	}

	public static Structure deserialize(String name, ConfigurationSection map) {
		StructureType type = ConfigUtil.parseEnum(StructureType.class, ConfigUtil.requireString(map, "type"));
		return type.deserialize(name, map);
	}

	public abstract boolean place(CaveGenContext ctx, BlockVector3 pos, Centroid centroid, boolean force) throws WorldEditException;

	protected boolean canReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canReplace == null) {
			return defaultCanReplace(ctx, block);
		} else {
			return canReplace.test(block);
		}
	}

	protected boolean defaultCanReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		return ctx.style.isTransparentBlock(block);
	}

}
