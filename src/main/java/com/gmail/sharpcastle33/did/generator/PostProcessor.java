package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Util;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.logging.Level;

public class PostProcessor {

	public static void postProcess(CaveGenContext ctx, List<Centroid> centroids) throws WorldEditException {
		Bukkit.getLogger().log(Level.WARNING, "Beginning smoothing pass... " + centroids.size() + " centroids.");

		for(Centroid centroid : centroids) {
			smooth(ctx, centroid.pos.toBlockPoint(),centroid.size+2);
		}

		Bukkit.getLogger().log(Level.WARNING, "Beginning painter pass...");

		for(Centroid centroid : centroids) {
			for (PainterStep painterStep : ctx.style.getPainterSteps()) {
				if (painterStep.getTags().isEmpty() || painterStep.getTags().stream().anyMatch(centroid.tags::contains)) {
					painterStep.apply(ctx, centroid.pos.toBlockPoint(), centroid.size+2);
				}
			}
		}

		Bukkit.getLogger().log(Level.WARNING, "Beginning structure pass...");

		for (Structure structure : ctx.style.getStructures()) {
			generateStructure(ctx, centroids, 100, structure);
		}

		if (!centroids.isEmpty()) {
			generatePortal(ctx, centroids.get(0).pos.toBlockPoint(), 100);
		}

		if (ctx.isDebug()) {
			for (Centroid centroid : centroids) {
				ctx.setBlock(centroid.pos.toBlockPoint(), Util.requireDefaultState(BlockTypes.EMERALD_BLOCK));
			}
		}
	}

	public static void smooth(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						//delete(tx+x, ty+y, tz+z);
						BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);

						if(ctx.style.getBaseBlock().equalsFuzzy(ctx.getBlock(pos))) {
							int amt = countAir(ctx, pos);
							if(amt>=13) {
								//Bukkit.getServer().getLogger().log(Level.WARNING,"count: " + amt);
								if(ctx.rand.nextInt(100) < 95) {
									ctx.setBlock(pos, ctx.style.getAirBlock());
								}
							}
						}
					}
				}
			}
		}
	}

	public static int countAir(CaveGenContext ctx, BlockVector3 loc) {
		int r = 1;
		int ret = 0;
		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					BlockVector3 pos = loc.add(tx, ty, tz);
					if(ctx.style.getAirBlock().equalsFuzzy(ctx.getBlock(pos))){
						ret++;
					}
				}
			}
		}
		return ret;
	}



	public static void generateStructure(CaveGenContext ctx, List<Centroid> centroids, int caveRadius, Structure structure) throws WorldEditException {
		for (Centroid centroid : centroids) {
			if (ctx.rand.nextDouble() < structure.getChance()) {
				if (structure.getTags().isEmpty() || structure.getTags().stream().anyMatch(centroid.tags::contains)) {
					Direction dir = structure.getRandomDirection(ctx.rand);
					BlockVector3 pos;
					if (dir == Direction.DOWN) {
						pos = PostProcessor.getFloor(ctx, centroid.pos.toBlockPoint(), caveRadius);
					} else if (dir == Direction.UP) {
						pos = PostProcessor.getCeiling(ctx, centroid.pos.toBlockPoint(), caveRadius);
					} else {
						pos = PostProcessor.getWall(ctx, centroid.pos.toBlockPoint(), caveRadius, dir.toBlockVector());
					}
					if (structure.canPlaceOn(ctx, ctx.getBlock(pos))) {
						structure.place(ctx, pos, dir);
					}
				}
			}
		}
	}

	private static void generatePortal(CaveGenContext ctx, BlockVector3 firstCentroid, int caveRadius) {
		if (ctx.style.getPortals().isEmpty()) {
			return;
		}
		Structure portal = ctx.style.getPortals().get(ctx.rand.nextInt(ctx.style.getPortals().size()));
		if (ctx.rand.nextDouble() >= portal.getChance()) {
			return;
		}
		BlockVector3 pos = PostProcessor.getFloor(ctx, firstCentroid, caveRadius);
		if (portal.canPlaceOn(ctx, ctx.getBlock(pos))) {
			portal.place(ctx, pos, Direction.DOWN);
		}
	}


	public static void generateWaterfalls(CaveGenContext ctx, List<Centroid> centroids, Vector3 loc, int caveRadius, int amount, int rarity, int placeRadius) {
		for(Centroid centroid : centroids) {
			if(ctx.rand.nextInt(rarity) == 0) {
				placeWaterfalls(loc,caveRadius,amount,placeRadius);
			}
		}
	}

	public static int placeWaterfalls(Vector3 loc, int caveRadius, int amount, int placeRadius) {
		for(int i = 0; i < amount; i++) {
			//TODO
		}

		return 0;

	}

	public static boolean isFloor(CaveGenContext ctx, BlockVector3 pos) {
		return isSolid(ctx, pos) && isSolid(ctx, pos.add(0, -1, 0)) && !isSolid(ctx, pos.add(0, 1, 0));
	}

	public static boolean isRoof(CaveGenContext ctx, BlockVector3 pos) {
		return isSolid(ctx, pos) && !isSolid(ctx, pos.add(0, -1, 0)) && isSolid(ctx, pos.add(0, 1, 0));
	}

	public static boolean isSolid(CaveGenContext ctx, BlockVector3 pos) {
		return !ctx.style.isTransparentBlock(ctx.getBlock(pos));
	}

	public static void replaceFloor(CaveGenContext ctx, BlockVector3 loc, int r, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< -2; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							if(isFloor(ctx, pos))
								if(ctx.getBlock(pos).equalsFuzzy(old)) {
									ctx.setBlock(pos, m);
								}

						}
					}
				}
			}
		}

	}

	public static void replaceCeiling(CaveGenContext ctx, BlockVector3 loc, int r, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=r; ty >2; ty--){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							if(isRoof(ctx, pos))
								if(ctx.getBlock(pos).equalsFuzzy(old)) {
									ctx.setBlock(pos, m);
								}

						}
					}
				}
			}
		}

	}

	public static void floorLayer(CaveGenContext ctx, BlockVector3 loc, int r, BlockStateHolder<?> m) throws MaxChangedBlocksException {

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< -2; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							if(isFloor(ctx, pos) && !m.equalsFuzzy(ctx.getBlock(pos)))
								ctx.setBlock(pos.add(0, 1, 0), m);
						}
					}
				}
			}
		}

	}

	public static void ceilingLayer(CaveGenContext ctx, BlockVector3 loc, int r, BlockStateHolder<?> m) throws MaxChangedBlocksException {

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=r; ty >2; ty--){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							if(isRoof(ctx, pos) && !m.equalsFuzzy(ctx.getBlock(pos)))
								ctx.setBlock(pos.add(0, -1, 0), m);
						}
					}
				}
			}
		}

	}

	public static void chanceReplace(CaveGenContext ctx, BlockVector3 loc, int r, BlockStateHolder<?> old, BlockStateHolder<?> m, double chance) throws MaxChangedBlocksException {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		if(chance >= 1) {
			radiusReplace(ctx,loc,r,old,m);
			return;
		}

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							if(ctx.getBlock(pos).equalsFuzzy(old)) {
								if(ctx.rand.nextDouble() < chance)
									ctx.setBlock(pos, m);
							}

						}
					}
				}
			}
		}
	}

	public static void radiusReplace(CaveGenContext ctx, BlockVector3 loc, int r, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							if(ctx.getBlock(pos).equalsFuzzy(old)) {
								ctx.setBlock(pos, m);
							}
						}
					}
				}
			}
		}
	}

	public static BlockVector3 getWall(CaveGenContext ctx, BlockVector3 loc, int r, BlockVector3 direction) {
		r= (int) (r *1.8);
		BlockVector3 ret = loc;
		for(int i = 0; i < r; i++) {
			ret = ret.add(direction);
			if (!ctx.style.isTransparentBlock(ctx.getBlock(ret))) {
				return ret;
			}
		}
		return ret;
	}

	public static BlockVector3 getCeiling(CaveGenContext ctx, BlockVector3 loc, int r) {
		BlockVector3 ret = loc;
		for(int i = 0; i < r+2; i++) {
			ret = ret.add(0,1,0);
			if (!ctx.style.isTransparentBlock(ctx.getBlock(ret))) {
				return ret;
			}
		}
		return ret;
	}

	public static BlockVector3 getFloor(CaveGenContext ctx, BlockVector3 loc, int r) {
		BlockVector3 ret = loc;
		for(int i = 0; i < r+2; i++) {
			ret = ret.add(0, -1, 0);
			if (!ctx.style.isTransparentBlock(ctx.getBlock(ret))) {
				return ret;
			}
		}
		return ret;
	}

	public static void genStalagmites(CaveGenContext ctx, BlockVector3 loc, int r, int amount) throws MaxChangedBlocksException {
		for(int i = 0; i < amount; i++) {
			int hozMod = Math.min(3, r);
			int tx = ctx.rand.nextInt(hozMod)+1;
			int tz = ctx.rand.nextInt(hozMod)+1;

			BlockVector3 start = loc.add(tx,0,tz);
			BlockVector3 end = getCeiling(ctx,start,r);
			end = end.add(0,-1,0);
			ctx.setBlock(end, Util.requireDefaultState(BlockTypes.COBBLESTONE_WALL));
		}

	}

	public boolean isBottomSlabPos(CaveGenContext ctx, BlockVector3 loc) {
		BlockVector3 tx = loc.add(1,0,0);
		BlockVector3 tz = loc.add(0,0,1);
		BlockVector3 tx1 = loc.add(-1,0,0);
		BlockVector3 tz1 = loc.add(0,0,-1);

		return isSlabConditionBottom(ctx, tx)
				|| isSlabConditionBottom(ctx, tz)
				|| isSlabConditionBottom(ctx, tx1)
				|| isSlabConditionBottom(ctx, tz1);
	}

	public boolean isTopSlabPos(CaveGenContext ctx, BlockVector3 loc) {
		BlockVector3 tx = loc.add(1,0,0);
		BlockVector3 tz = loc.add(0,0,1);
		BlockVector3 tx1 = loc.add(-1,0,0);
		BlockVector3 tz1 = loc.add(0,0,-1);

		return isSlabConditionTop(ctx, tx)
				|| isSlabConditionTop(ctx, tz)
				|| isSlabConditionTop(ctx, tx1)
				|| isSlabConditionTop(ctx, tz1);
	}


	public boolean isSlabConditionBottom(CaveGenContext ctx, BlockVector3 loc) {
		if(!ctx.style.isTransparentBlock(ctx.getBlock(loc))) {
			return ctx.style.isTransparentBlock(ctx.getBlock(loc.add(0, 1, 0)));
		}
		return false;
	}

	public boolean isSlabConditionTop(CaveGenContext ctx, BlockVector3 loc) {
		if(!ctx.style.isTransparentBlock(ctx.getBlock(loc))) {
			return ctx.style.isTransparentBlock(ctx.getBlock(loc.add(0, -1, 0)));
		}
		return false;
	}






}
