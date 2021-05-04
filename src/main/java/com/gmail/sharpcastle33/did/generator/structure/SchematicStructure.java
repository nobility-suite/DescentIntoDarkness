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
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class SchematicStructure extends Structure {
	private final List<Schematic> schematics;
	private final boolean ignoreAir;

	public SchematicStructure(String name, ConfigurationSection map) {
		super(name, StructureType.SCHEMATIC, map);
		this.schematics = ConfigUtil.deserializeSingleableList(ConfigUtil.require(map, "schematics"),
				schematicName -> {
			Clipboard data = DescentIntoDarkness.instance.getSchematic(schematicName);
			if (data == null) {
				throw new InvalidConfigException("Unknown schematic: " + schematicName);
			}
			return new Schematic(schematicName, data);
		}, () -> null);
		this.ignoreAir = map.getBoolean("ignoreAir", true);
	}

	@Override
	protected boolean shouldTransformBlocksByDefault() {
		return true;
	}

	@Override
	protected Direction getOriginPositionSide() {
		return getOriginSide();
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("schematics", ConfigUtil.serializeSingleableList(schematics, schematic -> schematic.name));
		map.set("ignoreAir", ignoreAir);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, boolean force) throws WorldEditException {
		Schematic chosenSchematic = schematics.get(ctx.rand.nextInt(schematics.size()));
		ClipboardHolder clipboardHolder = new ClipboardHolder(chosenSchematic.data);

		BlockVector3 to = pos.subtract(getOriginPositionSide().toBlockVector());
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
