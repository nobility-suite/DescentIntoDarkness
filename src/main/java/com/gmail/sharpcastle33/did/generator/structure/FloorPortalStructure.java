package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.provider.BlockProvider;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class FloorPortalStructure extends Structure {
	private final int minWidth;
	private final int maxWidth;
	private final int minHeight;
	private final int maxHeight;
	private final BlockProvider portalBlock;
	@Nullable
	private final BlockProvider frameBlock;

	protected FloorPortalStructure(String name, ConfigurationSection map) {
		super(name, StructureType.FLOOR_PORTAL, map);
		minWidth = map.getInt("minWidth", 2);
		maxWidth = map.getInt("maxWidth", 2);
		if (minWidth <= 0 || maxWidth < minWidth) {
			throw new InvalidConfigException("Invalid portal width range");
		}
		minHeight = map.getInt("minHeight", 3);
		maxHeight = map.getInt("maxHeight", 3);
		if (minHeight <= 0 || maxHeight < minHeight) {
			throw new InvalidConfigException("Invalid portal height range");
		}
		portalBlock = map.contains("portalBlock") ? ConfigUtil.parseBlockProvider(map.get("portalBlock")) : BlockProvider.NETHER_PORTAL;
		frameBlock = map.contains("frameBlock") ? ConfigUtil.parseBlockProvider(map.get("frameBlock")) : null;
	}

	@Override
	protected List<StructurePlacementEdge> getDefaultEdges() {
		return Collections.singletonList(StructurePlacementEdge.FLOOR);
	}

	@Override
	protected boolean shouldTransformBlocksByDefault() {
		return true;
	}

	@Override
	protected boolean shouldSnapToAxisByDefault() {
		return true;
	}

	@Override
	public boolean place(CaveGenContext ctx, BlockVector3 pos, Centroid centroid, boolean force) throws WorldEditException {
		int width = minWidth + ctx.rand.nextInt(maxWidth - minWidth + 1);
		int height = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);
		int x = pos.getBlockX() - width / 2;
		if (width % 2 == 0 && ctx.rand.nextBoolean()) {
			x++;
		}
		if (!force) {
			for (int i = -1; i <= width; i++) {
				if (!canPlaceOn(ctx, ctx.getBlock(BlockVector3.at(x + i, pos.getBlockY(), pos.getBlockZ())))) {
					return false;
				}
				for (int j = 1; j <= height + 1; j++) {
					if (!canReplace(ctx, ctx.getBlock(BlockVector3.at(x + i, pos.getBlockY() + j, pos.getBlockZ())))) {
						return false;
					}
				}
			}
		}

		for (int i = -1; i <= width; i++) {
			for (int j = 0; j <= height + 1; j++) {
				if (i == -1 || i == width || j == 0 || j == height + 1) {
					BlockStateHolder<?> frameBlock = this.frameBlock != null ? this.frameBlock.get(ctx, centroid) : ctx.style.getBaseBlock();
					ctx.setBlock(BlockVector3.at(x + i, pos.getBlockY() + j, pos.getBlockZ()), frameBlock);
				} else {
					ctx.setBlock(BlockVector3.at(x + i, pos.getBlockY() + j, pos.getBlockZ()), portalBlock.get(ctx, centroid));
				}
			}
		}

		return true;
	}
}
