package com.gmail.sharpcastle33.did.generator;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class TerrainGenerator {

	public static void paintOcean(Random rand, Location loc, int r) {
		r = r+1;
		//Material.PRISMARINE;
		//Material.PRISMARINE_BRICKS;
		//Material.DARK_PRISMARINE;

		radiusReplace(loc,r,Material.STONE.createBlockData(),Material.PRISMARINE.createBlockData());
		chanceReplace(rand,loc,r,Material.PRISMARINE.createBlockData(),Material.DARK_PRISMARINE.createBlockData(),0.1);
		chanceReplace(rand,loc,r,Material.PRISMARINE.createBlockData(),Material.PRISMARINE_BRICKS.createBlockData(), 0.1);
	}

	public static void paintCoral(Random rand, Location loc, int r) {
		radiusReplace(loc,r,Material.STONE.createBlockData(), Material.BRAIN_CORAL_BLOCK.createBlockData());
		chanceReplace(rand,loc,r,Material.BRAIN_CORAL_BLOCK.createBlockData(),Material.BUBBLE_CORAL_BLOCK.createBlockData(),0.1);
		chanceReplace(rand,loc,r,Material.BRAIN_CORAL_BLOCK.createBlockData(),Material.TUBE_CORAL_BLOCK.createBlockData(),0.1);
		chanceReplace(rand,loc,r,Material.BRAIN_CORAL_BLOCK.createBlockData(),Material.HORN_CORAL_BLOCK.createBlockData(),0.1);
		chanceReplace(rand,loc,r,Material.BRAIN_CORAL_BLOCK.createBlockData(),Material.FIRE_CORAL_BLOCK.createBlockData(),0.1);
		chanceReplace(rand,loc,r,Material.BRAIN_CORAL_BLOCK.createBlockData(),Material.DEAD_HORN_CORAL_BLOCK.createBlockData(),0.1);
		chanceReplace(rand,loc,r,Material.BRAIN_CORAL_BLOCK.createBlockData(),Material.WET_SPONGE.createBlockData(),0.05);

	}

	public static void paintGeneric(Random rand, Location loc, int r) {
		replaceFloor(loc,r,Material.STONE.createBlockData(),Material.GRAVEL.createBlockData());
		chanceReplace(rand,loc,r,Material.STONE.createBlockData(),Material.ANDESITE.createBlockData(),0.2);
		chanceReplace(rand,loc,r,Material.STONE.createBlockData(),Material.COBBLESTONE.createBlockData(),0.2);
		chanceReplace(rand,loc,r,Material.STONE.createBlockData(),Material.MOSSY_COBBLESTONE.createBlockData(),0.05);
	}

	public static void paintMarble(Random rand, Location loc, int r) {
		radiusReplace(loc,r,Material.STONE.createBlockData(), Material.DIORITE.createBlockData());
		chanceReplace(rand,loc,r,Material.DIORITE.createBlockData(),Material.POLISHED_DIORITE.createBlockData(),0.2);
		chanceReplace(rand,loc,r,Material.DIORITE.createBlockData(),Material.QUARTZ_BLOCK.createBlockData(),0.1);
	}

	public static void paintGlacial(Random rand, Location loc, int r) {
		radiusReplace(loc,r,Material.STONE.createBlockData(), Material.BLUE_ICE.createBlockData());

		replaceFloor(loc,r,Material.BLUE_ICE.createBlockData(),Material.SNOW_BLOCK.createBlockData());

		chanceReplace(rand,loc,r,Material.BLUE_ICE.createBlockData(),Material.PACKED_ICE.createBlockData(),0.2);
	}

	public static void paintTest(Random rand, Location loc, int r) {
		replaceFloor(loc,r,Material.STONE.createBlockData(),Material.SNOW_BLOCK.createBlockData());
		replaceCeiling(loc,r,Material.STONE.createBlockData(),Material.OBSIDIAN.createBlockData());
		radiusReplace(loc,r,Material.STONE.createBlockData(),Material.RED_WOOL.createBlockData());
	}

	public static void paintMesa(Random rand, Location loc, int r) {
		radiusReplace(loc,r,Material.STONE.createBlockData(),Material.GRANITE.createBlockData());
		replaceFloor(loc,r,Material.GRANITE.createBlockData(),Material.RED_SAND.createBlockData());
		replaceCeiling(loc,r,Material.GRANITE.createBlockData(),Material.RED_TERRACOTTA.createBlockData());
		chanceReplace(rand,loc,r,Material.GRANITE.createBlockData(),Material.POLISHED_GRANITE.createBlockData(),0.2);
	}

	public static void paintDesert(Random rand, Location loc, int r) {
		radiusReplace(loc,r,Material.STONE.createBlockData(),Material.SANDSTONE.createBlockData());
		replaceFloor(loc,r,Material.SANDSTONE.createBlockData(), Material.SAND.createBlockData());
		chanceReplace(rand,loc,r,Material.SANDSTONE.createBlockData(),Material.CHISELED_SANDSTONE.createBlockData(),0.2);
		chanceReplace(rand,loc,r,Material.SANDSTONE.createBlockData(),Material.GRANITE.createBlockData(),0.2);
		chanceReplace(rand,loc,r,Material.SANDSTONE.createBlockData(),Material.POLISHED_GRANITE.createBlockData(),0.1);
	}


	public static void paintMagma(Random rand, Location loc, int r) {
		//Material.OBSIDIAN
		//Material.BLACK_CONCRETE_POWDER;
		//Material.BLACK_CONCRETE;
		//Material.MAGMA_BLOCK;
		//Material.
		replaceFloor(loc,r,Material.STONE.createBlockData(),Material.BLACK_CONCRETE_POWDER.createBlockData());
		replaceCeiling(loc,r,Material.STONE.createBlockData(),Material.DEAD_TUBE_CORAL_BLOCK.createBlockData());
		radiusReplace(loc,r,Material.STONE.createBlockData(),Material.GRAY_CONCRETE.createBlockData());
		chanceReplace(rand,loc,r,Material.DEAD_TUBE_CORAL_BLOCK.createBlockData(),Material.DEAD_FIRE_CORAL_BLOCK.createBlockData(),0.5);

	}

	public static void generateBlob(Random rand, Location loc, int r, int rx, BlockData old, BlockData m) {

		int tx = rand.nextInt(r*2)-r;
		int tz = rand.nextInt(r*2)-r;
		int ty = rand.nextInt(r*2)-r;


		Location next = loc.clone().add(new Vector(tx,ty,tz));
		radiusReplace(next,rx,old,m);
	}

	public static void generateBlobs(Random rand, Location loc, int r, int rx, int amt, BlockData old, BlockData m) {
		for(int i = 0; i < amt; i++) {
			generateBlob(rand,loc,r,rx,old,m);
		}
	}

	public static boolean isFloor(Location loc) {
		Block b = loc.getBlock();

		return isSolid(b) && isSolid(b.getRelative(BlockFace.DOWN)) && !isSolid(b.getRelative(BlockFace.UP));
	}

	public static boolean isRoof(Location loc) {
		Block b = loc.getBlock();

		return isSolid(b) && !isSolid(b.getRelative(BlockFace.DOWN)) && isSolid(b.getRelative(BlockFace.UP));
	}

	public static boolean isSolid(Block b) {
		Material m = b.getType();
		return m != Material.AIR && m != Material.GLOWSTONE && m != Material.WATER && m != Material.LAVA;
	}

	public static void replaceFloor(Location loc, int r, BlockData old, BlockData m) {

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		World w = loc.getWorld();
		assert w != null;

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< -2; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
							if(isFloor(b.getLocation()))
								if(b.getBlockData().matches(old)) {
									b.setBlockData(m);
								}

						}
					}
				}
			}
		}

	}

	public static void replaceCeiling(Location loc, int r, BlockData old, BlockData m) {

		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		World w = loc.getWorld();
		assert w != null;

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=r; ty >2; ty--){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
							if(isRoof(b.getLocation()))
								if(b.getBlockData().matches(old)) {
									b.setBlockData(m);
								}

						}
					}
				}
			}
		}

	}

	public static void chanceReplace(Random rand, Location loc, int r, BlockData old, BlockData m, double chance) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		World w = loc.getWorld();
		assert w != null;

		if(chance >= 1) {
			radiusReplace(loc,r,old,m);
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
							Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
							if(b.getBlockData().matches(old)) {
								if(rand.nextDouble() < chance)
									b.setBlockData(m);
							}

						}
					}
				}
			}
		}
	}

	public static void radiusReplace(Location loc, int r, BlockData old, BlockData m) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		World w = loc.getWorld();
		assert w != null;

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
							if(b.getBlockData().matches(old)) {
								b.setBlockData(m);
							}

						}
					}
				}
			}
		}
	}

	public static Location getWall(Location loc, int r, Vector direction) {
		r= (int) (r *1.8);
		Location ret = loc.clone();
		for(int i = 0; i < r; i++) {
			ret.add(direction);
			if(ret.getBlock().getType() != Material.AIR) {
				Block up = ret.getBlock();
				if(up.getType() != Material.WATER && up.getType() != Material.LAVA) {
					if(up.getType() != Material.GLOWSTONE)
						return up.getLocation();
				}
			}
		}
		return ret;
	}

	public static Location getCeiling(Location loc, int r) {
		Location ret = loc.clone();
		for(int i = 0; i < r+2; i++) {
			ret.add(0,1,0);
			if(ret.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
				Block up = ret.getBlock().getRelative(BlockFace.UP);
				if(up.getType() != Material.WATER && up.getType() != Material.LAVA) {
					if(up.getType() != Material.GLOWSTONE)
						return up.getLocation();
				}
			}
		}
		return ret;
	}

	public static Location getFloor(Location loc, int r) {
		Location ret = loc.clone();
		for(int i = 0; i < r+2; i++) {
			ret.add(0,-1,0);
			if(ret.getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
				Block up = ret.getBlock().getRelative(BlockFace.DOWN);
				if(up.getType() != Material.WATER && up.getType() != Material.LAVA) {
					if(up.getType() != Material.GLOWSTONE)
						return up.getLocation();
				}
			}
		}
		return ret;
	}

	public static void genStalagmites(Random rand, Location loc, int r, int amount) {
		for(int i = 0; i < amount; i++) {
			int hozMod = Math.min(3, r);
			int tx = rand.nextInt(hozMod)+1;
			int tz = rand.nextInt(hozMod)+1;

			Location start = loc.clone().add(new Vector(tx,0,tz));
			Location end = getCeiling(start,r);
			end.add(new Vector(0,-1,0));
			end.getBlock().setType(Material.COBBLESTONE_WALL);
		}

	}

	public boolean isBottomSlabPos(Location loc) {
		Location temp = loc.clone();
		Location tx = temp.clone().add(1,0,0);
		Location tz = temp.clone().add(0,0,1);
		Location tx1 = temp.clone().add(-1,0,0);
		Location tz1 = temp.clone().add(0,0,-1);

		return isSlabConditionBottom(tx)
				|| isSlabConditionBottom(tz)
				|| isSlabConditionBottom(tx1)
				|| isSlabConditionBottom(tz1);
	}

	public boolean isTopSlabPos(Location loc) {
		Location temp = loc.clone();
		Location tx = temp.clone().add(1,0,0);
		Location tz = temp.clone().add(0,0,1);
		Location tx1 = temp.clone().add(-1,0,0);
		Location tz1 = temp.clone().add(0,0,-1);

		return isSlabConditionTop(tx)
				|| isSlabConditionTop(tz)
				|| isSlabConditionTop(tx1)
				|| isSlabConditionTop(tz1);
	}


	public boolean isSlabConditionBottom(Location l) {
		Material m = l.getBlock().getType();
		if(m != Material.GLOWSTONE && m != Material.AIR) {
			Material mx = l.getBlock().getRelative(BlockFace.UP).getType();
			return mx == Material.AIR || mx == Material.GLOWSTONE;
		}
		return false;
	}

	public boolean isSlabConditionTop(Location l) {
		Material m = l.getBlock().getType();
		if(m != Material.GLOWSTONE && m != Material.AIR) {
			Material mx = l.getBlock().getRelative(BlockFace.DOWN).getType();
			return mx == Material.AIR || mx == Material.GLOWSTONE;
		}
		return false;
	}






}
