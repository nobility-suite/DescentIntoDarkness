package com.gmail.sharpcastle33.did.generator.structure;

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

import java.util.Collections;
import java.util.List;

public class WallPortalStructure extends Structure {
	private final int minWidth;
	private final int maxWidth;
	private final int minHeight;
	private final int maxHeight;
	private final SnappingSide snappingSide;
	@Nullable
	private final BlockPredicate portalClearBlocks;
	private final BlockProvider portalBlock;
	@Nullable
	private final BlockProvider frameBlock;

	protected WallPortalStructure(String name, ConfigurationSection map) {
		super(name, StructureType.WALL_PORTAL, map);
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
		snappingSide = map.contains("snappingSide") ? ConfigUtil.parseEnum(SnappingSide.class, map.getString("snappingSide")) : SnappingSide.NONE;
		portalClearBlocks = map.contains("portalClearBlocks") ? ConfigUtil.parseBlockPredicate(map.get("portalClearBlocks")) : null;
		portalBlock = map.contains("portalBlock") ? ConfigUtil.parseBlockProvider(map.get("portalBlock")) : BlockProvider.NETHER_PORTAL;
		frameBlock = map.contains("frameBlock") ? ConfigUtil.parseBlockProvider(map.get("frameBlock")) : null;
	}

	@Override
	protected List<StructurePlacementEdge> getDefaultEdges() {
		return Collections.singletonList(StructurePlacementEdge.WALL);
	}

	@Override
	protected boolean shouldTransformBlocksByDefault() {
		return true;
	}

	@Override
	protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
		return Direction.SOUTH;
	}

	@Override
	protected Direction getOriginPositionSide() {
		return Direction.SOUTH;
	}

	@Override
	protected boolean shouldSnapToAxisByDefault() {
		return true;
	}

	@Override
	protected boolean defaultCanReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		return !ctx.style.isTransparentBlock(block);
	}

	@Override
	public boolean place(CaveGenContext ctx, BlockVector3 pos, Centroid centroid, boolean force) throws WorldEditException {
		int width = minWidth + ctx.rand.nextInt(maxWidth - minWidth + 1);
		int height = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);
		int x = pos.getBlockX() - width / 2;
		if (width % 2 == 0 && ctx.rand.nextBoolean()) {
			x++;
		}
		int y = pos.getBlockY() - height / 2;
		if (height % 2 == 0 && ctx.rand.nextBoolean()) {
			y++;
		}
		int z = pos.getBlockZ();
		boolean canPlace = true;
		outer:
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (!canReplace(ctx, ctx.getBlock(BlockVector3.at(x + i, y + j, z)))) {
					canPlace = false;
					break outer;
				}
				if (!isPortalClearBlock(ctx, BlockVector3.at(x + i, y + j, z + 1))) {
					canPlace = false;
					break outer;
				}
			}
		}
		if (!force && !canPlace) {
			return false;
		}
		if (canPlace && snappingSide != SnappingSide.NONE) {
			if (snappingSide == SnappingSide.FLOOR) {
				outer:
				while (y >= 0) {
					y--;
					for (int i = 0; i < width; i++) {
						if (!canReplace(ctx, ctx.getBlock(BlockVector3.at(x + i, y, z)))) {
							break outer;
						}
						if (!isPortalClearBlock(ctx, BlockVector3.at(x + i, y, z + 1))) {
							break outer;
						}
					}
				}
				y++;
			} else {
				outer:
				while (y <= 255) {
					y++;
					for (int i = 0; i < width; i++) {
						if (!canReplace(ctx, ctx.getBlock(BlockVector3.at(x + i, y + height - 1, z)))) {
							break outer;
						}
						if (!isPortalClearBlock(ctx, BlockVector3.at(x + i, y + height - 1, z + 1))) {
							break outer;
						}
					}
				}
				y--;
			}
		}

		for (int i = -1; i <= width; i++) {
			for (int j = -1; j <= height; j++) {
				if (i == -1 || i == width || j == -1 || j == height) {
					if (frameBlock != null) {
						ctx.setBlock(BlockVector3.at(x + i, y + j, z), frameBlock.get(ctx, centroid));
					}
				} else {
					ctx.setBlock(BlockVector3.at(x + i, y + j, z), portalBlock.get(ctx, centroid));
				}
			}
		}

		return true;
	}

	private boolean isPortalClearBlock(CaveGenContext ctx, BlockVector3 pos) {
		if (portalClearBlocks == null) {
			return ctx.style.isTransparentBlock(ctx.getBlock(pos));
		}
		return portalClearBlocks.test(ctx.getBlock(pos));
	}

	public enum SnappingSide {
		NONE, FLOOR, CEILING
	}
}
