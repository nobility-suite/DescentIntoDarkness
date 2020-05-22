package com.gmail.sharpcastle33.did.generator;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class TerrainGenerator {

  public static void paintOcean(Location loc, int r) {
    r = r+1;
    //Material.PRISMARINE;
    //Material.PRISMARINE_BRICKS;
    //Material.DARK_PRISMARINE;
    
    radiusReplace(loc,r,Material.STONE,Material.PRISMARINE);
    chanceReplace(loc,r,Material.PRISMARINE,Material.DARK_PRISMARINE,0.1);
    chanceReplace(loc,r,Material.PRISMARINE,Material.PRISMARINE_BRICKS, 0.1);
  }
  
  public static void paintCoral(Location loc, int r) {
	  radiusReplace(loc,r,Material.STONE, Material.BRAIN_CORAL_BLOCK);
	  chanceReplace(loc,r,Material.BRAIN_CORAL_BLOCK,Material.BUBBLE_CORAL_BLOCK,0.1);
	  chanceReplace(loc,r,Material.BRAIN_CORAL_BLOCK,Material.TUBE_CORAL_BLOCK,0.1);
	  chanceReplace(loc,r,Material.BRAIN_CORAL_BLOCK,Material.HORN_CORAL_BLOCK,0.1);
	  chanceReplace(loc,r,Material.BRAIN_CORAL_BLOCK,Material.FIRE_CORAL_BLOCK,0.1);
	  chanceReplace(loc,r,Material.BRAIN_CORAL_BLOCK,Material.DEAD_HORN_CORAL_BLOCK,0.1);
	  chanceReplace(loc,r,Material.BRAIN_CORAL_BLOCK,Material.WET_SPONGE,0.05);

  }
  
  public static void paintMarble(Location loc, int r) {
    radiusReplace(loc,r,Material.STONE, Material.DIORITE);
    chanceReplace(loc,r,Material.DIORITE,Material.POLISHED_DIORITE,0.2);
    chanceReplace(loc,r,Material.DIORITE,Material.QUARTZ_BLOCK,0.1);
  }
  
  public static void paintGlacial(Location loc, int r) {
	    radiusReplace(loc,r,Material.STONE, Material.BLUE_ICE);
	    
	    replaceFloor(loc,r,Material.BLUE_ICE,Material.SNOW_BLOCK);

	    chanceReplace(loc,r,Material.BLUE_ICE,Material.PACKED_ICE,0.2);
	  }
  
  public static void paintTest(Location loc, int r) {
	  replaceFloor(loc,r,Material.STONE,Material.SNOW_BLOCK);
	  replaceCeiling(loc,r,Material.STONE,Material.OBSIDIAN);
	  radiusReplace(loc,r,Material.STONE,Material.RED_WOOL);
  }
  
  
  public static void paintMagma(Location loc, int r) {
    //Material.OBSIDIAN
    //Material.BLACK_CONCRETE_POWDER;
    //Material.BLACK_CONCRETE;
    //Material.MAGMA_BLOCK;
    //Material.
    replaceFloor(loc,r,Material.STONE,Material.BLACK_CONCRETE_POWDER);
    replaceCeiling(loc,r,Material.STONE,Material.DEAD_TUBE_CORAL_BLOCK);
    radiusReplace(loc,r,Material.STONE,Material.GRAY_CONCRETE);
    chanceReplace(loc,r,Material.DEAD_TUBE_CORAL_BLOCK,Material.DEAD_FIRE_CORAL_BLOCK,0.5);
    
  }
  
  public static void generateBlob(Location loc, int r, int rx, Material old, Material m) {
    
    Random rand = new Random();
    int tx = rand.nextInt(r*2)-r;
    int tz = rand.nextInt(r*2)-r;
    int ty = rand.nextInt(r*2)-r;
    
    
    Location next = loc.clone().add(new Vector(tx,ty,tz));
    radiusReplace(next,rx,old,m);
  }
  
  public static void generateBlobs(Location loc, int r, int rx, int amt, Material old, Material m) {
    for(int i = 0; i < amt; i++) {
      generateBlob(loc,r,rx,old,m);
    }
  }
  
  public static boolean isFloor(Location loc) {
    Block b = loc.getBlock();
    
    if(isSolid(b) && isSolid(b.getRelative(BlockFace.DOWN)) && !isSolid(b.getRelative(BlockFace.UP))) {
      return true;
    }else return false;
  }
  
  public static boolean isRoof(Location loc) {
    Block b = loc.getBlock();
    
    if(isSolid(b) && !isSolid(b.getRelative(BlockFace.DOWN)) && isSolid(b.getRelative(BlockFace.UP))) {
      return true;
    }else return false;
  }
  
  public static boolean isSolid(Block b) {
    Material m = b.getType();
    if(m == Material.AIR || m == Material.GLOWSTONE || m == Material.WATER || m == Material.LAVA) {
      return false;
    }else return true;
  }

  public static void replaceFloor(Location loc, int r, Material old, Material m) {

    int x = loc.getBlockX();
    int y = loc.getBlockY();
    int z = loc.getBlockZ();
    
    World w = loc.getWorld();
    
    for(int tx=-r; tx< r+1; tx++){
      for(int ty=-r; ty< -2; ty++){
          for(int tz=-r; tz< r+1; tz++){
              if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
                  if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
                      continue;
                  }
                  if(ty+y > 0) {
                    Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
                    if(isFloor(b.getLocation()))
                    if(b.getType() == old) {
                      b.setType(m);
                    }
                    
                  }
              }
          }
      }
  }
    
  }
  
  public static void replaceCeiling(Location loc, int r, Material old, Material m) {

	    int x = loc.getBlockX();
	    int y = loc.getBlockY();
	    int z = loc.getBlockZ();
	    
	    World w = loc.getWorld();
	    
	    for(int tx=-r; tx< r+1; tx++){
	      for(int ty=r; ty >2; ty--){
	          for(int tz=-r; tz< r+1; tz++){
	              if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
	                  if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
	                      continue;
	                  }
	                  if(ty+y > 0) {
	                    Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
	                    if(isRoof(b.getLocation()))
	                    if(b.getType() == old) {
	                      b.setType(m);
	                    }
	                    
	                  }
	              }
	          }
	      }
	  }
	    
	  }
  
  public static void chanceReplace(Location loc, int r, Material old, Material m, double chance) {
    int x = loc.getBlockX();
    int y = loc.getBlockY();
    int z = loc.getBlockZ();
    
    World w = loc.getWorld();
    
    Random rand = new Random();
    int bound = 1000;
    
    if(chance >= 1) {
      radiusReplace(loc,r,old,m);
      return;
    }
    
    if(chance < 0.001) {
      return;
    }
    
    int rng = (int) (chance*1000);
    
    for(int tx=-r; tx< r+1; tx++){
        for(int ty=-r; ty< r+1; ty++){
            for(int tz=-r; tz< r+1; tz++){
                if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
                    if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
                        continue;
                    }
                    if(ty+y > 0) {
                      Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
                      if(b.getType() == old) {
                        if(rand.nextInt(bound) < rng)
                        b.setType(m);
                      }
                      
                    }
                }
            }
        }
    }
  }
  
  public static void radiusReplace(Location loc, int r, Material old, Material m) {
    int x = loc.getBlockX();
    int y = loc.getBlockY();
    int z = loc.getBlockZ();
    
    World w = loc.getWorld();
    
    for(int tx=-r; tx< r+1; tx++){
        for(int ty=-r; ty< r+1; ty++){
            for(int tz=-r; tz< r+1; tz++){
                if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
                    if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
                        continue;
                    }
                    if(ty+y > 0) {
                      Block b =  w.getBlockAt(tx+x, ty+y, tz+z);
                      if(b.getType() == old) {
                        b.setType(m);
                      }
                      
                    }
                }
            }
        }
    }
  }

  public static Location getWall(Location loc, int r, Vector direction) {
	  r= (int) ((int) r*1.8);
      Location ret = loc.clone();
      for(int i = 0; i < r; r++) {
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
      for(int i = 0; i < r+2; r++) {
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
      for(int i = 0; i < r+2; r++) {
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
  
  public static void genStalagmites(Location loc, int r, int amount) {
    Random rand = new Random();
    
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
    
    if(isSlabConditionBottom(tx) 
        || isSlabConditionBottom(tz)
        || isSlabConditionBottom(tx1) 
        || isSlabConditionBottom(tz1)) {
      return true;
    }else return false;
  }
  
  public boolean isTopSlabPos(Location loc) {
    Location temp = loc.clone();
    Location tx = temp.clone().add(1,0,0);
    Location tz = temp.clone().add(0,0,1);
    Location tx1 = temp.clone().add(-1,0,0);
    Location tz1 = temp.clone().add(0,0,-1);
    
    if(isSlabConditionTop(tx) 
        || isSlabConditionTop(tz)
        || isSlabConditionTop(tx1) 
        || isSlabConditionTop(tz1)) {
      return true;
    }else return false;
  }
  
  
  public boolean isSlabConditionBottom(Location l) {
    Material m = l.getBlock().getType();
    if(m != Material.GLOWSTONE && m != Material.AIR) {
      Material mx = l.getBlock().getRelative(BlockFace.UP).getType();
      if(mx == Material.AIR || mx == Material.GLOWSTONE) {
        return true;
      }
    }
    return false;
  }
  
  public boolean isSlabConditionTop(Location l) {
    Material m = l.getBlock().getType();
    if(m != Material.GLOWSTONE && m != Material.AIR) {
      Material mx = l.getBlock().getRelative(BlockFace.DOWN).getType();
      if(mx == Material.AIR || mx == Material.GLOWSTONE) {
        return true;
      }
    }
    return false;
  }
  
  
  
  
  

}
