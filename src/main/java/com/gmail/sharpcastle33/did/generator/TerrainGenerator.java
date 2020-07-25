package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Util;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class TerrainGenerator {

	public static void paintOcean(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		r = r+1;
		//Material.PRISMARINE;
		//Material.PRISMARINE_BRICKS;
		//Material.DARK_PRISMARINE;

		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.PRISMARINE));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.PRISMARINE), Util.requireDefaultState(BlockTypes.DARK_PRISMARINE),0.1);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.PRISMARINE), Util.requireDefaultState(BlockTypes.PRISMARINE_BRICKS), 0.1);
	}

	public static void paintCoral(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.BUBBLE_CORAL_BLOCK),0.1);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.TUBE_CORAL_BLOCK),0.1);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.HORN_CORAL_BLOCK),0.1);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.FIRE_CORAL_BLOCK),0.1);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.DEAD_HORN_CORAL_BLOCK),0.1);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.WET_SPONGE),0.05);

	}

	public static void paintGeneric(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAVEL));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.ANDESITE),0.2);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.COBBLESTONE),0.2);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.MOSSY_COBBLESTONE),0.05);
	}

	public static void paintMarble(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.DIORITE));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.DIORITE), Util.requireDefaultState(BlockTypes.POLISHED_DIORITE),0.2);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.DIORITE), Util.requireDefaultState(BlockTypes.QUARTZ_BLOCK),0.1);
	}

	public static void paintGlacial(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.BLUE_ICE));

		replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.BLUE_ICE), Util.requireDefaultState(BlockTypes.SNOW_BLOCK));

		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BLUE_ICE), Util.requireDefaultState(BlockTypes.PACKED_ICE),0.2);
	}

	public static void paintTest(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.SNOW_BLOCK));
		replaceCeiling(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.OBSIDIAN));
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.RED_WOOL));
	}

	public static void paintMesa(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRANITE));
		replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.GRANITE), Util.requireDefaultState(BlockTypes.RED_SAND));
		replaceCeiling(ctx,loc,r, Util.requireDefaultState(BlockTypes.GRANITE), Util.requireDefaultState(BlockTypes.RED_TERRACOTTA));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.GRANITE), Util.requireDefaultState(BlockTypes.POLISHED_GRANITE),0.2);
	}

	public static void paintDesert(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.SANDSTONE));
		replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.SAND));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.CHISELED_SANDSTONE),0.2);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.GRANITE),0.2);
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.POLISHED_GRANITE),0.1);
	}


	public static void paintMagma(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
		//Material.OBSIDIAN
		//Material.BLACK_CONCRETE_POWDER;
		//Material.BLACK_CONCRETE;
		//Material.MAGMA_BLOCK;
		//Material.
		replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.BLACK_CONCRETE_POWDER));
		replaceCeiling(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.DEAD_TUBE_CORAL_BLOCK));
		radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAY_CONCRETE));
		chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.DEAD_TUBE_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.DEAD_FIRE_CORAL_BLOCK),0.5);

	}

	public static void generateBlob(CaveGenContext ctx, BlockVector3 loc, int r, int rx, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {

		int tx = ctx.rand.nextInt(r*2)-r;
		int tz = ctx.rand.nextInt(r*2)-r;
		int ty = ctx.rand.nextInt(r*2)-r;


		BlockVector3 next = loc.add(tx,ty,tz);
		radiusReplace(ctx, next,rx,old,m);
	}

	public static void generateBlobs(CaveGenContext ctx, BlockVector3 loc, int r, int rx, int amt, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {
		for(int i = 0; i < amt; i++) {
			generateBlob(ctx,loc,r,rx,old,m);
		}
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
