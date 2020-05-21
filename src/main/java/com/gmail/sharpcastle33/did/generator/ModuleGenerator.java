package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class ModuleGenerator {
	
	ArrayList<Location> centroids = new ArrayList<Location>();

	
	public  void read(String cave, Location start, int size, Vector dir) {
		Bukkit.getLogger().log(Level.WARNING, "Beginning module generation... " + cave.length() + " modules.");
		Bukkit.getLogger().log(Level.WARNING, "Cave string: " + cave);
		Location loc = start;
		
		
		char[] ch = cave.toCharArray();
		for(int i = 0; i < ch.length; i++) {
			int tempSize = size + getSizeMod();
			//Bukkit.getLogger().log(Level.WARNING, "Applying... " + ch[i]);
			if(ch[i] == 'A') { dir = redirectLeft(dir); }
			if(ch[i] == 'D') { dir = redirectRight(dir);}
			//Bukkit.getLogger().log(Level.WARNING, "New Vector: " + ch[i] + ", " + dir);
			loc = apply(ch[i],loc,tempSize,size,dir);
			centroids.add(loc.clone());
		}
		
		Bukkit.getLogger().log(Level.WARNING, "Beginning smoothing pass... " + centroids.size() + " centroids.");

		for(Location l : centroids) {
			smooth(l,size+2);
			//l.getBlock().setType(Material.GLOWSTONE);
		}
		
		for(Location l : centroids) {
			TerrainGenerator.paintMagma(l, size+2);
			//l.getBlock().setType(Material.OBSIDIAN);
		}
		
		//	public void generateOres(Material ore, int cap, int size, int radius, int caveRadius) {

		generateOres(Material.DIAMOND_ORE, 100, 2, 4, size);
	}
	
	public  void smooth(Location loc, int r) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		World w = loc.getWorld();
		Random rand = new Random();
		
		for(int tx=-r; tx< r+1; tx++){
		    for(int ty=-r; ty< r+1; ty++){
		        for(int tz=-r; tz< r+1; tz++){
		            if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
		                //delete(tx+x, ty+y, tz+z);
		            	Block b = w.getBlockAt(tx+x, ty+y, tz+z);
		            	
		            	if(b.getType() == Material.STONE) {
		            		int amt = countAir(b);
		            		if(amt>=13) {
			            		//Bukkit.getServer().getLogger().log(Level.WARNING,"count: " + amt);
		            			if(rand.nextInt(100) < 95) {
			            			b.setType(Material.AIR);
		            			}
		            		}
		            	}
		            }
		        }
		    }
		}
	}
	
	public  int countAir(Block b) {
		int r = 1;
		int ret = 0;
		Location loc = b.getLocation();
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		World w = loc.getWorld();
		for(int tx=-r; tx< r+1; tx++){
		    for(int ty=-r; ty< r+1; ty++){
		        for(int tz=-r; tz< r+1; tz++){
		            if(w.getBlockAt(x+tx, y+ty, z+tz).getType() == Material.AIR){
		            	ret++;
		            }
		        }
		    }
		}
		return ret;
	}
	
	public  Location vary(Location loc) {
		Random rand = new Random();
		int x = rand.nextInt(2)-1;
		int y = rand.nextInt(2)-1;
		int z = rand.nextInt(2)-1;
		return loc.add(new Vector(x,y,z));
	}
	
	public  Vector randomRedirect(Vector current) {
		Random rand = new Random();
		int choice = rand.nextInt(100);
		Vector clone = current.clone();
		if(choice <= 50) {
			return clone;
		}else if(choice <= 70) {
			return clone.rotateAroundY(15);
		}else if(choice <= 90) {
			return clone.rotateAroundY(-15);
		}else if(choice <= 95) {
			return clone.rotateAroundY(30);
		}else {
			return clone.rotateAroundY(-30);
		}
	}
	
	public  Vector redirectRight(Vector current) {
		Random rand = new Random();
		int choice = rand.nextInt(100);
		Vector clone = current.clone();
        if(choice <= 45) {
			return clone.rotateAroundY(-15);
		}else if(choice <= 90) {
			return clone.rotateAroundY(-15);
		}else if(choice <= 95) {
			return clone.rotateAroundY(-30);
		}else {
			return clone.rotateAroundY(-30);
		}
	}
	
	public  Vector redirectLeft(Vector current) {
		Random rand = new Random();
		int choice = rand.nextInt(100);
		Vector clone = current.clone();
        if(choice <= 45) {
			return clone.rotateAroundY(15);
		}else if(choice <= 90) {
			return clone.rotateAroundY(15);
		}else if(choice <= 95) {
			return clone.rotateAroundY(30);
		}else {
			return clone.rotateAroundY(30);
		}
	}
	
	public  Location apply(char c, Location loc, int size, int realSize, Vector dir) {
		switch(c) {
			case 'W':
				deleteSphere(loc,size);
				return getNext(c,loc,size,dir);
			case 'A':
				//dir = redirectLeft(dir);
				deleteSphere(loc,size);
				return getNext(c,loc,size,dir);
			case 'S':
				deleteSphere(loc,size);
				return getNext(c,loc,size,dir);
			case 'D':
				//dir = redirectRight(dir);
				deleteSphere(loc,size);
				return getNext(c,loc,size,dir);
			case 'X':
				Vector clone = dir.clone();
				Random rand = new Random();
				int coinflip = rand.nextInt(1);
				if(coinflip == 0) { coinflip = -1; }
				
				int newSize = rand.nextInt(20);
				int sizeMod = rand.nextInt(2);
				CaveGenerator.generateCave(loc.getWorld(),size-sizeMod,loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),20+newSize,false,clone.rotateAroundY(90*coinflip));
				return getNext(c,loc,size,dir);
			case 'x':
				return generateSmallBranch(loc,size,dir);
			case 'O':
				Random rand2 = new Random();
				int lengthMod = rand2.nextInt(4);
				int length = 8+lengthMod;
				createDropshaft(loc,size,length);
				if(size <=7) {
					return loc.add(0,-(length-4),0);
				}
				return loc.add(0,-(length-2),0);
			case 'L':
				return generateLargeRoom(loc,size);
			case 'R':
				return generateSmallRoom(loc,size);
			case 'P':
				return generatePoolRoom(loc,size);
			case 'H':
				return generateShelfRoom(loc,size,dir);
			case 'C':
				if(size>7) {
					return generateChasm(loc,size,dir);
				}else return generateLargeRoom(loc,size);
				
			/*case 'Q':
				return rampUp(loc, size, new Vector(1,1,0));
			case 'E':
				return rampUp(loc, size, new Vector(1,-1,0));*/
				


			default:
			break;
		}
		return loc;
	}
	
	private  Location generateSmallBranch(Location loc, int size, Vector dir) {
		Vector clone = dir.clone();
		Random rand = new Random();
		
		clone.rotateAroundY(rand.nextInt(270)+45);
		
		if(size < 7) {
			size = 6;
		}else if(size < 11) {
			size = 7;
		}else {
			size = size/2 + 2;
		}
		
		generateSmallRoom(loc,size);
		
		int newSize = rand.nextInt(20);
		int sizeMod = rand.nextInt(1);
		CaveGenerator.generateCave(loc.getWorld(),size-sizeMod,loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),20+newSize,false,clone);
		return getNext('X',loc,size,dir);
	}

	private  Location findFloor(Location loc) {
		while(loc.getY() > 1 && loc.getBlock().getType() == Material.AIR) {
			loc = loc.add(new Vector(0,-1,0));
		}
		return loc;
	}
	
	private  void floodFill(Material fill, Location loc) {
		if(loc.getBlock().getType() == Material.AIR) {
			loc.getBlock().setType(fill);
			floodFill(fill,loc.clone().add(1,0,0));
			floodFill(fill,loc.clone().add(-1,0,0));
			floodFill(fill,loc.clone().add(0,0,-1));
			floodFill(fill,loc.clone().add(0,0,1));
		}
	}
	
	private  Location generateChasm(Location loc, int size, Vector caveDir) {
		
		ArrayList<Location> centers = new ArrayList<Location>();
		
		size = size-1;
		Random rand = new Random();
		Location ret = loc.clone();
		Vector retVec = caveDir.clone();
		retVec.multiply(size);
		int chasmSize = rand.nextInt(2)+3;
		int chasmSizeBackward = rand.nextInt(3)+2;
		int coinflip = rand.nextInt(1);
		if(coinflip == 0) { coinflip = -1; }
		Vector dir = caveDir.clone();
		dir.rotateAroundY(90*coinflip);
		
		int vert = size/2 + 1;
		Location start = loc.clone();
		start.add(new Vector(0,vert*-2,0));
		
		start.add(dir.clone().multiply(-1*size*(chasmSizeBackward)));
		
		Location og = start.clone();
		chasmSize = chasmSize+chasmSizeBackward;
		for(int i = 0; i < 3; i++) {
			Location set = start.clone();
			vary(set);
			centers.add(set);
			for(int j = 0; j < chasmSize; j++) {
				deleteSphere(set,size);
				set.add(dir.clone().multiply(size));
			}
			start.add(new Vector(0,vert,0));
		}
		
		for(Location l : centers) {
			centroids.add(l.clone());
			smooth(l,size+2);
		}
		
		Location lava = findFloor(og).add(new Vector(0,2,0));
		//floodFill(Material.LAVA,lava);
		
		return ret.add(retVec);
	}
	
	public  Location generateSmallRoom(Location loc, int r) {
		Random rand = new Random();
		int amount = rand.nextInt(4)+4;
		r -= 1;
		
		r = Math.max(r, 4);
		
		for(int i = 0; i < amount; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-2)+2;
			int ty = rand.nextInt(r);
			int tz = rand.nextInt(r-2)+2;
			
			int coinflip = rand.nextInt(1);
			if(coinflip == 0) {tx*=-1; }
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {tz*=-1; }
			
			int sizeMod = rand.nextInt(1);
			
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {sizeMod*=-1; }
			
			
			deleteSphere(clone.add(new Vector(tx,ty,tz)),r+sizeMod);
			centroids.add(clone.clone());
			
		}
		
		int direction = rand.nextInt(3);
		
		if(direction == 0) {
			return loc.add(new Vector(r-3,0,0));
		}else if(direction == 1) {
			return loc.add(new Vector(-1*r+3,0,0));
		}else if(direction == 2) {
			return loc.add(new Vector(0,0,-1*r+3));
		}else if(direction == 3) {
			return loc.add(new Vector(0,0,r-3));
		}
		
		return loc;
	}
	
	public  Location generatePoolRoom(Location loc, int r) {
		Random rand = new Random();
		int amount = rand.nextInt(4)+3;
		r -= 1;
		
		for(int i = 0; i < amount; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-2)+2;
			int ty = rand.nextInt(r);
			int tz = rand.nextInt(r-2)+2;
			
			int coinflip = rand.nextInt(1);
			if(coinflip == 0) {tx*=-1; }
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {tz*=-1; }
			
			int sizeMod = rand.nextInt(1);
			
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {sizeMod*=-1; }
			
			deleteSphere(clone.add(new Vector(tx,ty,tz)),r+sizeMod);
			
			
		}
		
		for(int i = 0; i < amount-1; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-3)+2;
			int ty = rand.nextInt(r-1);
			int tz = rand.nextInt(r-3)+2;
			
			int coinflip = rand.nextInt(1);
			if(coinflip == 0) {tx*=-1; }
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {tz*=-1; }
			
			int sizeMod = rand.nextInt(1);
			
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {sizeMod*=-1; }
			
			deleteSphere(clone.add(new Vector(tx,-ty,tz)),r+sizeMod);
			
			
		}
		
		Location pool = findFloor(loc.clone()).add(new Vector(0,1,0));
		floodFill(Material.WATER,pool);
		
		int direction = rand.nextInt(3);
		
		if(direction == 0) {
			return loc.add(new Vector(r-2,0,0));
		}else if(direction == 1) {
			return loc.add(new Vector(-1*r+2,0,0));
		}else if(direction == 2) {
			return loc.add(new Vector(0,0,-1*r+2));
		}else if(direction == 3) {
			return loc.add(new Vector(0,0,r-2));
		}
		

		
		return loc;
	}

	public  Location generateLargeRoom(Location loc, int r) {
		Random rand = new Random();
		int amount = rand.nextInt(5)+3;
		
		
		if(r < 3) {
			r = 3;
		}
		
		for(int i = 0; i < amount; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-2)+2;
			int ty = rand.nextInt(r);
			int tz = rand.nextInt(r-2)+2;
			
			int coinflip = rand.nextInt(1);
			if(coinflip == 0) {tx*=-1; }
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {tz*=-1; }
			
			int sizeMod = rand.nextInt(2);
			
			coinflip = rand.nextInt(1);
			if(coinflip == 0) {sizeMod*=-1; }
			
			deleteSphere(clone.add(new Vector(tx,ty,tz)),r+sizeMod);
			centroids.add(clone.clone());
			
			
		}
		
		int direction = rand.nextInt(3);
		
		if(direction == 0) {
			return loc.add(new Vector(2*r-2,0,0));
		}else if(direction == 1) {
			return loc.add(new Vector(-2*r+2,0,0));
		}else if(direction == 2) {
			return loc.add(new Vector(0,0,-2*r+2));
		}else if(direction == 3) {
			return loc.add(new Vector(0,0,2*r-2));
		}
		
		return loc;
	}
	
	public  Location generateShelfRoom(Location loc, int r, Vector direction) {
	  Random rand = new Random();
	  int coinflip = rand.nextInt(1);
	  
	  if(coinflip == 0) {
	    return generateShelfFromBottom(loc,r,direction);
	  }else return generateShelfFromTop(loc,r,direction);
	}
	
	public  Location generateShelfFromBottom(Location loc, int r, Vector direction) {
      Location next = generateLargeRoom(loc,r);
      next = generateSmallRoom(next,r);
      
      Random rand = new Random();
      int coinflip = rand.nextInt(1);
      if(coinflip == 0) { coinflip = -1; }
      
      Location shelf = loc.clone().add(new Vector(0,rand.nextInt(5)+6,0));
      Vector adjust = direction.clone();
      adjust.rotateAroundY(90+rand.nextInt(10) * coinflip);
      shelf.add(adjust);
      
      Vector dir = direction.clone();
      int size = Math.max(r-2,5);
      
      for(int i = 0; i < 3; i++) {
        generateSmallRoom(shelf,size);
        vary(shelf);
        shelf = shelf.add(dir.clone().multiply(size));
      }
      
      return next;
	}
	
	   public  Location generateShelfFromTop(Location loc, int r, Vector direction) {

	      
	      Random rand = new Random();
	      int coinflip = rand.nextInt(1);
	      if(coinflip == 0) { coinflip = -1; }
	      
	      Location shelf = loc.clone().add(new Vector(0,-1*rand.nextInt(5)+6,0));
	      Vector adjust = direction.clone();
	      adjust.rotateAroundY(90+rand.nextInt(10) * coinflip);
	      shelf.add(adjust);
	      
	      Vector dir = direction.clone();
	      int size = Math.max(r-2,5);
	      Location next = loc.clone();
	      for(int i = 0; i < 3; i++) {
	        generateSmallRoom(next,size);
	        vary(next);
	        next = shelf.add(dir.clone().multiply(size));
	      }
	      
	       shelf = generateLargeRoom(loc,r);
	       shelf = generateSmallRoom(shelf,r);
	       
	      
	      return next;
	    }
	
	
	public  Location rampUp(Location loc, int r, Vector direction) {
		Location next = loc;
		for(int i = 0; i < r; i++) {
			next = loc.add(direction);
			deleteSphere(next,r+getSizeMod());
		}
		return vary(next);
	}
	
	public  void deleteSphere(Location loc, int r) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		
		World w = loc.getWorld();
		
		for(int tx=-r; tx< r+1; tx++){
		    for(int ty=-r; ty< r+1; ty++){
		        for(int tz=-r; tz< r+1; tz++){
		            if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
		                //delete(tx+x, ty+y, tz+z);
		            	if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
		            		continue;
		            	}
		            	if(ty+y > 0)
		            	w.getBlockAt(tx+x, ty+y, tz+z).setType(Material.AIR);
		            }
		        }
		    }
		}
	}
	
	public  void createDropshaft(Location loc, int r, int length) {
		int i = 0;
		if(r >= 6) {
			r=r-1;
		}
		

		while(i < length) {
			loc = vary(loc);
			Random rand = new Random();
			int n = rand.nextInt(2)+2;
			loc.add(0,-n,0);
			i+=n;
			centroids.add(loc.clone());
			deleteSphere(loc,r);
		}
	}
	
	public  Location getNext(char c, Location loc, int r, Vector dir) {
		r = r-2;
		loc = vary(loc);
		Vector apply = dir.clone();
		apply.multiply(r); //dir.multiply(r);
		switch(c) {
		case 'W':
			return loc.add(apply);
		case 'A':
			return loc.add(apply);
		case 'S':
			return loc.add(apply);	
		case 'D':
			return loc.add(apply);
		case 'X':
			return loc;
		case 'E':
			return loc.add(apply);
		default:
			return loc.add(apply);
		}
	}
	
	public  int getSizeMod() {
		Random rand = new Random();
		return rand.nextInt(3)-2;
	}
	
	public void generateOres(Material ore, int cap, int size, int radius, int caveRadius) {
		Random rand = new Random();
		
		for(Location loc : centroids) {
			int chance = rand.nextInt(5);
			if(chance == 1) {
				placeOreCluster(loc,caveRadius,size,radius,ore);
			}
		}
	}
	
	public int placeOreCluster(Location loc, int caveRadius, int size, int radius, Material ore) {
		Random dir = new Random();
		Location toPlace = loc.clone();
		Boolean wall = false;
		switch(dir.nextInt(6)) {
		case 1:
			toPlace = loc.clone().add(new Vector(caveRadius,0,0));
			wall = true;
			break;
		case 2:
			toPlace = loc.clone().add(new Vector(-caveRadius,0,0));
			wall = true;
			break;
		case 3:
			toPlace = loc.clone().add(new Vector(0,0,caveRadius));
			wall = true;
			break;
		case 4:
			toPlace = loc.clone().add(new Vector(0,0,-caveRadius));
			wall = true;
			break;
		case 5:
			toPlace = TerrainGenerator.getCeiling(loc, caveRadius);
			break;
		default:
			toPlace = TerrainGenerator.getFloor(loc, caveRadius);
			break;
			
		}
		return generateOreCluster(toPlace,size,radius,ore,wall);
		
	}
	
	public int generateOreCluster(Location loc, int size, int radius, Material ore, boolean wall) {
		loc.getBlock().setType(ore);
		int count = 1;
		Random rand = new Random();
		
		int randx;
		int randy;
		int randz;
		int flipx;
		int flipy;
		int flipz;
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		
		for(int i = 0; i < size; i++) {
			if(wall) {
				randx = rand.nextInt(radius/2);
				randy = rand.nextInt(radius);
				randz = rand.nextInt(radius/2);
			}else {
				randx = rand.nextInt(radius);
				randy = rand.nextInt(radius/2);
				randz = rand.nextInt(radius);
			}
			
			flipy = rand.nextInt(1);
			flipx = rand.nextInt(1);
			flipz = rand.nextInt(1);
			
			if(flipy == 0) { flipy = -1; }
			if(flipx == 0) { flipx = -1; }
			if(flipz == 0) { flipz = -1; }
			
			randx*=flipx;
			randy*=flipy;
			randz*=flipz;
			x+=randx;
			y+=randy;
			z+=randz;
			
			int r = 1;
			
		    for(int tx=-r; tx< r+1; tx++){
		        for(int ty=-r; ty< r+1; ty++){
		            for(int tz=-r; tz< r+1; tz++){
		                if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= r-2){
		                    if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
		                        continue;
		                    }
		                    if(ty+y > 0) {
		                      Block b =  loc.getWorld().getBlockAt(tx+x, ty+y, tz+z);
		                      if(b.getType() != Material.AIR) {     
		                        b.setType(ore);
		                        count++;
		                      }
		                    }
		                }
		            }
		        }
		    }
		}
		
		return count;
	}
	

}
