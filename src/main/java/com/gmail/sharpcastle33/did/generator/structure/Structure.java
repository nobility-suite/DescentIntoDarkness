package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class Structure {
	private final String name;
	private final StructureType type;
	protected final List<StructurePlacementEdge> edges;
	private final double count;
	private final List<BlockStateHolder<?>> canPlaceOn;
	private final List<BlockStateHolder<?>> cannotPlaceOn;
	private final List<BlockStateHolder<?>> canReplace;
	private final List<BlockStateHolder<?>> cannotReplace;
	private final List<Direction> validDirections = new ArrayList<>();
	private final Direction originSide;
	private final boolean shouldTransformBlocks;
	private final boolean shouldTransformPosition;
	private final boolean randomRotation;
	private final List<String> tags;
	private final boolean tagsInverted;

	protected Structure(String name, StructureType type, ConfigurationSection map) {
		this.name = name;
		this.type = type;
		this.edges = ConfigUtil.deserializeSingleableList(map.get("edges"), val -> ConfigUtil.parseEnum(StructurePlacementEdge.class, val), () -> Lists.newArrayList(StructurePlacementEdge.values()));
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
		this.canPlaceOn = deserializePlacementRule(map.get("canPlaceOn"));
		this.cannotPlaceOn = deserializePlacementRule(map.get("cannotPlaceOn"));
		this.canReplace = deserializePlacementRule(map.get("canReplace"));
		this.cannotReplace = deserializePlacementRule(map.get("cannotReplace"));
		String originSideVal = map.getString("originSide");
		if (originSideVal == null) {
			if (edges.contains(StructurePlacementEdge.FLOOR)) {
				this.originSide = Direction.DOWN;
			} else if (edges.contains(StructurePlacementEdge.CEILING)) {
				this.originSide = Direction.UP;
			} else {
				this.originSide = Direction.SOUTH;
			}
		} else {
			this.originSide = ConfigUtil.parseEnum(Direction.class, originSideVal);
			if (!originSide.isCardinal() && !originSide.isUpright()) {
				throw new InvalidConfigException("Invalid Direction: " + originSideVal);
			}
		}
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
			if (cannotPlaceOn == null) {
				return !ctx.style.isTransparentBlock(block);
			}
			return cannotPlaceOn.stream().noneMatch(it -> it.equalsFuzzy(block));
		} else {
			return canPlaceOn.stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

	protected boolean shouldTransformBlocksByDefault() {
		return false;
	}

	protected boolean shouldTransformPositionByDefault() {
		return true;
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

	public void serialize(ConfigurationSection map) {
		map.set("type", ConfigUtil.enumToString(type));
		map.set("edges", ConfigUtil.serializeSingleableList(edges, ConfigUtil::enumToString));
		map.set("count", count);
		if (canPlaceOn != null) {
			map.set("canPlaceOn", ConfigUtil.serializeSingleableList(canPlaceOn, BlockStateHolder::getAsString));
		}
		if (cannotPlaceOn != null) {
			map.set("cannotPlaceOn", ConfigUtil.serializeSingleableList(cannotPlaceOn, BlockStateHolder::getAsString));
		}
		if (canReplace != null) {
			map.set("canReplace", ConfigUtil.serializeSingleableList(canReplace, BlockStateHolder::getAsString));
		}
		if (cannotReplace != null) {
			map.set("cannotReplace", ConfigUtil.serializeSingleableList(cannotReplace, BlockStateHolder::getAsString));
		}
		map.set("originSide", ConfigUtil.enumToString(originSide));
		map.set("shouldTransformBlocks", shouldTransformBlocks);
		map.set("shouldTransformPosition", shouldTransformPosition);
		map.set("randomRotation", randomRotation);
		if (!tags.isEmpty()) {
			map.set("tags", ConfigUtil.serializeSingleableList(tags, Function.identity()));
		}
		serialize0(map);
	}

	protected abstract void serialize0(ConfigurationSection map);

	public static Structure deserialize(String name, ConfigurationSection map) {
		StructureType type = ConfigUtil.parseEnum(StructureType.class, ConfigUtil.requireString(map, "type"));
		return type.deserialize(name, map);
	}

	protected static List<BlockStateHolder<?>> deserializePlacementRule(Object rule) {
		return ConfigUtil.deserializeSingleableList(rule, ConfigUtil::parseBlock, () -> null);
	}

	public abstract void place(CaveGenContext ctx, BlockVector3 pos, boolean force) throws WorldEditException;

	protected boolean canReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canReplace == null) {
			if (cannotReplace == null) {
				return ctx.style.isTransparentBlock(block);
			}
			return cannotReplace.stream().noneMatch(it -> it.equalsFuzzy(block));
		} else {
			return canReplace.stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

}
