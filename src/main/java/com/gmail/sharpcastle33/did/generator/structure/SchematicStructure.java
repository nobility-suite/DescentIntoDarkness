package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class SchematicStructure extends Structure {
	private final List<Schematic> schematics;
	private final Direction originSide;
	private final boolean ignoreAir;
	private final boolean randomRotation;

	public SchematicStructure(String name, ConfigurationSection map) {
		super(name, StructureType.SCHEMATIC, map);
		this.schematics = ConfigUtil.deserializeSingleableList(ConfigUtil.require(map, "schematics"),
				schematicName -> {
			Clipboard data = DescentIntoDarkness.plugin.getSchematic(schematicName);
			if (data == null) {
				throw new InvalidConfigException("Unknown schematic: " + schematicName);
			}
			return new Schematic(schematicName, data);
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

	public SchematicStructure(String name, List<StructurePlacementEdge> edges, double chance, int count,
							  List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace,
							  List<String> tags, boolean tagsInverted, List<Schematic> schematics,
							  Direction originSide, boolean ignoreAir, boolean randomRotation) {
		super(name, StructureType.SCHEMATIC, edges, chance, count, canPlaceOn, canReplace, tags, tagsInverted);
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
	public void place(CaveGenContext ctx, BlockVector3 pos, Direction side, boolean force) throws WorldEditException {
		Schematic chosenSchematic = schematics.get(ctx.rand.nextInt(schematics.size()));
		ClipboardHolder clipboardHolder = new ClipboardHolder(chosenSchematic.data);
		AffineTransform transform = new AffineTransform();

		if (side.isUpright() && randomRotation) {
			transform = transform.rotateY(ctx.rand.nextInt(4) * 90);
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

		clipboardHolder.setTransform(transform);
		BlockVector3 to = pos.subtract(side.toBlockVector());
		if (force || canPlace(ctx, to, chosenSchematic.data, clipboardHolder.getTransform())) {
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
			BlockVector3 destPos =
					transform.apply(pos.subtract(schematic.getOrigin()).toVector3()).toBlockPoint().add(to);
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
