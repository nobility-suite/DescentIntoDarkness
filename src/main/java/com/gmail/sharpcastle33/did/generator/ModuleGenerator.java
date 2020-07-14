package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class ModuleGenerator {

	ArrayList<Location> centroids = new ArrayList<>();


	public  void read(Random rand, String cave, Location start, int size, CaveStyle style, Vector dir) {
		Bukkit.getLogger().log(Level.WARNING, "Beginning module generation... " + cave.length() + " modules.");
		Bukkit.getLogger().log(Level.WARNING, "Cave string: " + cave);
		Location loc = start;


		for (char c : cave.toCharArray()) {
			int tempSize = size + getSizeMod(rand);
			//Bukkit.getLogger().log(Level.WARNING, "Applying... " + ch[i]);
			if (c == 'A') {
				dir = redirectLeft(rand, dir);
			}
			if (c == 'D') {
				dir = redirectRight(rand, dir);
			}
			//Bukkit.getLogger().log(Level.WARNING, "New Vector: " + ch[i] + ", " + dir);
			loc = apply(rand, c, loc, tempSize, style, size, dir);
			centroids.add(loc.clone());
		}

		Bukkit.getLogger().log(Level.WARNING, "Beginning smoothing pass... " + centroids.size() + " centroids.");

		for(Location l : centroids) {
			smooth(rand, l,size+2);
		}

		for(Location l : centroids) {
			for (PainterStep painterStep : style.getPainterSteps()) {
				painterStep.apply(rand, l, size+2);
			}
		}

		//	public void generateOres(Material ore, int rarity, int size, int radius, int caveRadius) {

		generateOres(rand,Material.COAL_ORE, 100, 7, 4, size);
		generateOres(rand,Material.DIAMOND_ORE, 100, 11, 4, size);
		generateOres(rand,Material.EMERALD_ORE, 100, 12, 3, size);


	}

	public  void smooth(Random rand, Location loc, int r) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		World w = loc.getWorld();
		assert w != null;

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
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
		assert w != null;
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

	public  Location vary(Random rand, Location loc) {
		int x = rand.nextInt(2)-1;
		int y = rand.nextInt(2)-1;
		int z = rand.nextInt(2)-1;
		return loc.add(new Vector(x,y,z));
	}

	public  Vector randomRedirect(Random rand, Vector current) {
		int choice = rand.nextInt(100);
		Vector clone = current.clone();
		if(choice <= 50) {
			return clone;
		}else if(choice <= 70) {
			return clone.rotateAroundY(Math.PI/12);
		}else if(choice <= 90) {
			return clone.rotateAroundY(-Math.PI/12);
		}else if(choice <= 95) {
			return clone.rotateAroundY(Math.PI/6);
		}else {
			return clone.rotateAroundY(-Math.PI/6);
		}
	}

	public  Vector redirectRight(Random rand, Vector current) {
		int choice = rand.nextInt(100);
		Vector clone = current.clone();
		if(choice <= 45) {
			return clone.rotateAroundY(-Math.PI/12);
		}else if(choice <= 90) {
			return clone.rotateAroundY(-Math.PI/12);
		}else if(choice <= 95) {
			return clone.rotateAroundY(-Math.PI/6);
		}else {
			return clone.rotateAroundY(-Math.PI/6);
		}
	}

	public  Vector redirectLeft(Random rand, Vector current) {
		int choice = rand.nextInt(100);
		Vector clone = current.clone();
		if(choice <= 45) {
			return clone.rotateAroundY(Math.PI/12);
		}else if(choice <= 90) {
			return clone.rotateAroundY(Math.PI/12);
		}else if(choice <= 95) {
			return clone.rotateAroundY(Math.PI/6);
		}else {
			return clone.rotateAroundY(Math.PI/6);
		}
	}

	public  Location apply(Random rand, char c, Location loc, int size, CaveStyle style, int realSize, Vector dir) {
		switch(c) {
			case 'W':
				deleteSphere(loc,size);
				return getNext(rand,c,loc,size,dir);
			case 'A':
				//dir = redirectLeft(dir);
				deleteSphere(loc,size);
				return getNext(rand,c,loc,size,dir);
			case 'S':
				deleteSphere(loc,size);
				return getNext(rand,c,loc,size,dir);
			case 'D':
				//dir = redirectRight(dir);
				deleteSphere(loc,size);
				return getNext(rand,c,loc,size,dir);
			case 'X':
				Vector clone = dir.clone();
				int coinflip = rand.nextBoolean() ? 1 : -1;

				int newSize = rand.nextInt(20);
				int sizeMod = rand.nextInt(2);
				CaveGenerator.generateCave(loc.getWorld(),rand,size-sizeMod,style,loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),20+newSize,false,clone.rotateAroundY(Math.PI/2*coinflip));
				return getNext(rand,c,loc,size,dir);
			case 'x':
				return generateSmallBranch(rand,loc,size,style,dir);
			case 'O':
				int lengthMod = rand.nextInt(4);
				int length = 8+lengthMod;
				createDropshaft(rand,loc,size,length);
				if(size <=7) {
					return loc.add(0,-(length-4),0);
				}
				return loc.add(0,-(length-2),0);
			case 'L':
				return generateLargeRoom(rand,loc,size);
			case 'R':
				return generateSmallRoom(rand,loc,size);
			case 'P':
				return generatePoolRoom(rand,loc,size);
			case 'H':
				return generateShelfRoom(rand,loc,size,dir);
			case 'C':
//				if(size>7) {
//					return generateChasm(loc,size,dir);
//				}else return generateLargeRoom(loc,size);
				return generateLargeRoom(rand,loc,size);
				
			/*case 'Q':
				return rampUp(loc, size, new Vector(1,1,0));
			case 'E':
				return rampUp(loc, size, new Vector(1,-1,0));*/



			default:
				break;
		}
		return loc;
	}

	private  Location generateSmallBranch(Random rand, Location loc, int size, CaveStyle style, Vector dir) {
		Vector clone = dir.clone();

		clone.rotateAroundY(2 * Math.PI * (rand.nextDouble() * 3/4 + 1.0/8));

		if(size < 7) {
			size = 6;
		}else if(size < 11) {
			size = 7;
		}else {
			size = size/2 + 2;
		}

		generateSmallRoom(rand,loc,size);

		int newSize = rand.nextInt(20);
		int sizeMod = rand.nextInt(1);
		CaveGenerator.generateCave(loc.getWorld(),rand,size-sizeMod,style,loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),20+newSize,false,clone);
		return getNext(rand,'X',loc,size,dir);
	}

	private  Location findFloor(Location loc) {
		while(loc.getY() > 1 && loc.getBlock().getType() == Material.AIR) {
			loc = loc.add(new Vector(0,-1,0));
		}
		return loc;
	}

	private  void floodFill(Material fill, Location loc) {
		assert fill != Material.AIR; // would cause an infinite loop
		if(loc.getBlock().getType() == Material.AIR) {
			loc.getBlock().setType(fill);
			floodFill(fill,loc.clone().add(1,0,0));
			floodFill(fill,loc.clone().add(-1,0,0));
			floodFill(fill,loc.clone().add(0,0,-1));
			floodFill(fill,loc.clone().add(0,0,1));
		}
	}

	private  Location generateChasm(Random rand, Location loc, int size, Vector caveDir) {

		ArrayList<Location> centers = new ArrayList<>();

		size = size-1;
		Location ret = loc.clone();
		Vector retVec = caveDir.clone();
		retVec.multiply(size);
		int chasmSize = rand.nextInt(2)+3;
		int chasmSizeBackward = rand.nextInt(3)+2;
		int coinflip = rand.nextBoolean() ? 1 : -1;
		Vector dir = caveDir.clone();
		dir.rotateAroundY(Math.PI / 2 * coinflip);

		int vert = size/2 + 1;
		Location start = loc.clone();
		start.add(new Vector(0,vert*-2,0));

		start.add(dir.clone().multiply(-1*size*(chasmSizeBackward)));

		Location og = start.clone();
		chasmSize = chasmSize+chasmSizeBackward;
		for(int i = 0; i < 3; i++) {
			Location set = start.clone();
			vary(rand, set);
			centers.add(set);
			for(int j = 0; j < chasmSize; j++) {
				deleteSphere(set,size);
				set.add(dir.clone().multiply(size));
			}
			start.add(new Vector(0,vert,0));
		}

		for(Location l : centers) {
			centroids.add(l.clone());
			smooth(rand, l,size+2);
		}

		Location lava = findFloor(og).add(new Vector(0,2,0));
		//floodFill(Material.LAVA,lava);

		return ret.add(retVec);
	}

	public  Location generateSmallRoom(Random rand, Location loc, int r) {
		int amount = rand.nextInt(4)+4;
		r -= 1;

		r = Math.max(r, 4);

		for(int i = 0; i < amount; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-2)+2;
			int ty = rand.nextInt(r);
			int tz = rand.nextInt(r-2)+2;

			if(rand.nextBoolean()) {tx*=-1; }
			if(rand.nextBoolean()) {tz*=-1; }

			int sizeMod = rand.nextInt(1);

			if(rand.nextBoolean()) {sizeMod*=-1; }


			deleteSphere(clone.add(new Vector(tx,ty,tz)),r+sizeMod);
			centroids.add(clone.clone());

		}

		switch (rand.nextInt(4)) {
			case 0:
				return loc.add(new Vector(r - 3, 0, 0));
			case 1:
				return loc.add(new Vector(-1 * r + 3, 0, 0));
			case 2:
				return loc.add(new Vector(0, 0, -1 * r + 3));
			case 3:
				return loc.add(new Vector(0, 0, r - 3));
		}

		return loc;
	}

	public  Location generatePoolRoom(Random rand, Location loc, int r) {
		int amount = rand.nextInt(4)+3;
		r -= 1;

		for(int i = 0; i < amount; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-2)+2;
			int ty = rand.nextInt(r);
			int tz = rand.nextInt(r-2)+2;

			if(rand.nextBoolean()) {tx*=-1; }
			if(rand.nextBoolean()) {tz*=-1; }

			int sizeMod = rand.nextInt(1);

			if(rand.nextBoolean()) {sizeMod*=-1; }

			deleteSphere(clone.add(new Vector(tx,ty,tz)),r+sizeMod);


		}

		for(int i = 0; i < amount-1; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-3)+2;
			int ty = rand.nextInt(r-1);
			int tz = rand.nextInt(r-3)+2;

			if(rand.nextBoolean()) {tx*=-1; }
			if(rand.nextBoolean()) {tz*=-1; }

			int sizeMod = rand.nextInt(1);

			if(rand.nextBoolean()) {sizeMod*=-1; }

			deleteSphere(clone.add(new Vector(tx,-ty,tz)),r+sizeMod);


		}

		Location pool = findFloor(loc.clone()).add(new Vector(0,1,0));
		floodFill(Material.WATER,pool);

		switch (rand.nextInt(4)) {
			case 0:
				return loc.add(new Vector(r - 2, 0, 0));
			case 1:
				return loc.add(new Vector(-1 * r + 2, 0, 0));
			case 2:
				return loc.add(new Vector(0, 0, -1 * r + 2));
			case 3:
				return loc.add(new Vector(0, 0, r - 2));
		}



		return loc;
	}

	public  Location generateLargeRoom(Random rand, Location loc, int r) {
		int amount = rand.nextInt(5)+3;


		if(r < 3) {
			r = 3;
		}

		for(int i = 0; i < amount; i++) {
			Location clone = loc.clone();
			int tx = rand.nextInt(r-2)+2;
			int ty = rand.nextInt(r);
			int tz = rand.nextInt(r-2)+2;

			if(rand.nextBoolean()) {tx*=-1; }
			if(rand.nextBoolean()) {tz*=-1; }

			int sizeMod = rand.nextInt(2);

			if(rand.nextBoolean()) {sizeMod*=-1; }

			deleteSphere(clone.add(new Vector(tx,ty,tz)),r+sizeMod);
			centroids.add(clone.clone());


		}

		switch (rand.nextInt(4)) {
			case 0:
				return loc.add(new Vector(2 * r - 2, 0, 0));
			case 1:
				return loc.add(new Vector(-2 * r + 2, 0, 0));
			case 2:
				return loc.add(new Vector(0, 0, -2 * r + 2));
			case 3:
				return loc.add(new Vector(0, 0, 2 * r - 2));
		}

		return loc;
	}

	public  Location generateShelfRoom(Random rand, Location loc, int r, Vector direction) {
		if(rand.nextBoolean()) {
			return generateShelfFromBottom(rand,loc,r,direction);
		} else {
			return generateShelfFromTop(rand,loc,r,direction);
		}
	}

	public  Location generateShelfFromBottom(Random rand, Location loc, int r, Vector direction) {
		Location next = generateLargeRoom(rand,loc,r);
		next = generateSmallRoom(rand,next,r);

		int coinflip = rand.nextBoolean() ? 1 : -1;

		Location shelf = loc.clone().add(new Vector(0,rand.nextInt(5)+6,0));
		Vector adjust = direction.clone();
		adjust.rotateAroundY(Math.PI / 2 + rand.nextDouble() * Math.PI / 18 * coinflip);
		shelf.add(adjust);

		Vector dir = direction.clone();
		int size = Math.max(r-2,5);

		for(int i = 0; i < 3; i++) {
			generateSmallRoom(rand,shelf,size);
			vary(rand, shelf);
			shelf = shelf.add(dir.clone().multiply(size));
		}

		return next;
	}

	public  Location generateShelfFromTop(Random rand, Location loc, int r, Vector direction) {


		int coinflip = rand.nextBoolean() ? 1 : -1;

		Location shelf = loc.clone().add(new Vector(0,-1*rand.nextInt(5)+6,0));
		Vector adjust = direction.clone();
		adjust.rotateAroundY(Math.PI / 2 +rand.nextDouble() * Math.PI / 18 * coinflip);
		shelf.add(adjust);

		Vector dir = direction.clone();
		int size = Math.max(r-2,5);
		Location next = loc.clone();
		for(int i = 0; i < 3; i++) {
			generateSmallRoom(rand,next,size);
			vary(rand, next);
			next = shelf.add(dir.clone().multiply(size));
		}

		shelf = generateLargeRoom(rand,loc,r);
		shelf = generateSmallRoom(rand,shelf,r);


		return next;
	}


	public  Location rampUp(Random rand, Location loc, int r, Vector direction) {
		Location next = loc;
		for(int i = 0; i < r; i++) {
			next = loc.add(direction);
			deleteSphere(next,r+getSizeMod(rand));
		}
		return vary(rand, next);
	}

	public  void deleteSphere(Location loc, int r) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();

		World w = loc.getWorld();
		assert w != null;

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

	public  void createDropshaft(Random rand, Location loc, int r, int length) {
		int i = 0;
		if(r >= 6) {
			r=r-1;
		}


		while(i < length) {
			loc = vary(rand, loc);
			int n = rand.nextInt(2)+2;
			loc.add(0,-n,0);
			i+=n;
			centroids.add(loc.clone());
			deleteSphere(loc,r);
		}
	}

	public  Location getNext(Random rand, char c, Location loc, int r, Vector dir) {
		r = r-2;
		loc = vary(rand, loc);
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

	public  int getSizeMod(Random rand) {
		return rand.nextInt(3)-2;
	}

	public void generateOres(Random rand, Material ore, int rarity, int size, int radius, int caveRadius) {
		for(Location loc : centroids) {
			int chance = rand.nextInt(rarity);
			if(chance == 0) {
				placeOreCluster(rand,loc,caveRadius,size,radius,ore);
			}
		}
	}


	public void generateWaterfalls(Random rand, Location loc, int caveRadius, int amount, int rarity, int placeRadius) {
		for(Location l : centroids) {
			if(rand.nextInt(rarity) == 0) {
				placeWaterfalls(loc,caveRadius,amount,placeRadius);
			}
		}
	}

	public int placeWaterfalls(Location loc, int caveRadius, int amount, int placeRadius) {
		for(int i = 0; i < amount; i++) {
			//TODO
		}

		return 0;

	}

	public int placeOreCluster(Random rand, Location loc, int caveRadius, int size, int radius, Material ore) {
		Location toPlace;
		boolean wall = false;
		switch(rand.nextInt(6)) {
			case 1:
				toPlace = TerrainGenerator.getWall(loc, size, new Vector(caveRadius,0,0));
				wall = true;
				toPlace.getBlock().setType(Material.ORANGE_WOOL);
				break;
			case 2:
				toPlace = TerrainGenerator.getWall(loc, size, new Vector(-caveRadius,0,0));
				wall = true;
				toPlace.getBlock().setType(Material.PURPLE_WOOL);
				break;
			case 3:
				toPlace = TerrainGenerator.getWall(loc, size, new Vector(0,0,caveRadius));
				wall = true;
				toPlace.getBlock().setType(Material.YELLOW_WOOL);
				break;
			case 4:
				toPlace = TerrainGenerator.getWall(loc, size, new Vector(0,0,-caveRadius));
				wall = true;
				toPlace.getBlock().setType(Material.GREEN_WOOL);
				break;
			case 5:
				toPlace = TerrainGenerator.getCeiling(loc, caveRadius);
				toPlace.getBlock().setType(Material.WHITE_WOOL);
				break;
			default:
				toPlace = TerrainGenerator.getFloor(loc, caveRadius);
				toPlace.getBlock().setType(Material.RED_WOOL);
				break;

		}
		return generateOreCluster(rand,toPlace,size,radius,ore,wall);

	}

	public int generateOreCluster(Random rand, Location loc, int size, int radius, Material ore, boolean wall) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		World w = loc.getWorld();
		assert w != null;
		int count = 0;

		for(int tx = -radius; tx< radius +1; tx++){
			for(int ty = -radius; ty< radius +1; ty++){
				for(int tz = -radius; tz< radius +1; tz++){
					if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= radius -2){
						if(ty+y > 0) {
							Block b =  w.getBlockAt(tx+x, ty+y, tz+z);

							if(b.getType() != Material.AIR) {
								if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == radius -2)) {
									if(rand.nextBoolean())
										continue;
								}
								b.setType(ore);
								count++;
							}

						}
					}
				}
			}
		}

		return count;
	}


}
