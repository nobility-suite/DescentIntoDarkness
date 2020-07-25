package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.Util;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;

public class ModuleGenerator {

	ArrayList<Vector3> centroids = new ArrayList<>();


	public  void read(CaveGenContext ctx, String cave, Vector3 start, int size, Vector3 dir) throws MaxChangedBlocksException {
		Bukkit.getLogger().log(Level.WARNING, "Beginning module generation... " + cave.length() + " modules.");
		Bukkit.getLogger().log(Level.WARNING, "Cave string: " + cave);
		Vector3 loc = start;


		for (char c : cave.toCharArray()) {
			int tempSize = size + getSizeMod(ctx);
			//Bukkit.getLogger().log(Level.WARNING, "Applying... " + ch[i]);
			if (c == 'A') {
				dir = redirectLeft(ctx, dir);
			}
			if (c == 'D') {
				dir = redirectRight(ctx, dir);
			}
			//Bukkit.getLogger().log(Level.WARNING, "New Vector: " + ch[i] + ", " + dir);
			loc = apply(ctx, c, loc, tempSize, size, dir);
			centroids.add(loc);
		}

		Bukkit.getLogger().log(Level.WARNING, "Beginning smoothing pass... " + centroids.size() + " centroids.");

		for(Vector3 l : centroids) {
			smooth(ctx, l.toBlockPoint(),size+2);
		}

		for(Vector3 l : centroids) {
			for (PainterStep painterStep : ctx.style.getPainterSteps()) {
				painterStep.apply(ctx, l.toBlockPoint(), size+2);
			}
		}

		//	public void generateOres(Material ore, int rarity, int size, int radius, int caveRadius) {

		generateOres(ctx,Util.requireDefaultState(BlockTypes.COAL_ORE), 100, 7, 4, size);
		generateOres(ctx,Util.requireDefaultState(BlockTypes.DIAMOND_ORE), 100, 11, 4, size);
		generateOres(ctx,Util.requireDefaultState(BlockTypes.EMERALD_ORE), 100, 12, 3, size);


	}

	public  void smooth(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
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

	public  int countAir(CaveGenContext ctx, BlockVector3 loc) {
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

	public  Vector3 vary(CaveGenContext ctx, Vector3 loc) {
		int x = ctx.rand.nextInt(2)-1;
		int y = ctx.rand.nextInt(2)-1;
		int z = ctx.rand.nextInt(2)-1;
		return loc.add(x,y,z);
	}

	public  Vector3 randomRedirect(CaveGenContext ctx, Vector3 current) {
		int choice = ctx.rand.nextInt(100);
		if(choice <= 50) {
			return current;
		}else if(choice <= 70) {
			return Util.rotateAroundY(current, Math.PI/12);
		}else if(choice <= 90) {
			return Util.rotateAroundY(current, -Math.PI/12);
		}else if(choice <= 95) {
			return Util.rotateAroundY(current, Math.PI/6);
		}else {
			return Util.rotateAroundY(current, -Math.PI/6);
		}
	}

	public  Vector3 redirectRight(CaveGenContext ctx, Vector3 current) {
		int choice = ctx.rand.nextInt(100);
		if(choice <= 45) {
			return Util.rotateAroundY(current, -Math.PI/12);
		}else if(choice <= 90) {
			return Util.rotateAroundY(current, -Math.PI/12);
		}else if(choice <= 95) {
			return Util.rotateAroundY(current, -Math.PI/6);
		}else {
			return Util.rotateAroundY(current, -Math.PI/6);
		}
	}

	public  Vector3 redirectLeft(CaveGenContext ctx, Vector3 current) {
		int choice = ctx.rand.nextInt(100);
		if(choice <= 45) {
			return Util.rotateAroundY(current, Math.PI/12);
		}else if(choice <= 90) {
			return Util.rotateAroundY(current, Math.PI/12);
		}else if(choice <= 95) {
			return Util.rotateAroundY(current, Math.PI/6);
		}else {
			return Util.rotateAroundY(current, Math.PI/6);
		}
	}

	public Vector3 apply(CaveGenContext ctx, char c, Vector3 loc, int size, int realSize, Vector3 dir) throws MaxChangedBlocksException {
		switch(c) {
			case 'W':
				deleteSphere(ctx,loc,size);
				return getNext(ctx,c,loc,size,dir);
			case 'A':
				//dir = redirectLeft(dir);
				deleteSphere(ctx,loc,size);
				return getNext(ctx,c,loc,size,dir);
			case 'S':
				deleteSphere(ctx,loc,size);
				return getNext(ctx,c,loc,size,dir);
			case 'D':
				//dir = redirectRight(dir);
				deleteSphere(ctx,loc,size);
				return getNext(ctx,c,loc,size,dir);
			case 'X':
				int coinflip = ctx.rand.nextBoolean() ? 1 : -1;

				int newSize = ctx.rand.nextInt(20);
				int sizeMod = ctx.rand.nextInt(2);
				CaveGenerator.generateCave(ctx,size-sizeMod,loc,20+newSize,false, Util.rotateAroundY(dir, Math.PI / 2 * coinflip));
				return getNext(ctx,c,loc,size,dir);
			case 'x':
				return generateSmallBranch(ctx,loc,size,dir);
			case 'O':
				int lengthMod = ctx.rand.nextInt(4);
				int length = 8+lengthMod;
				createDropshaft(ctx,loc,size,length);
				if(size <=7) {
					return loc.add(0,-(length-4),0);
				}
				return loc.add(0,-(length-2),0);
			case 'L':
				return generateLargeRoom(ctx,loc,size);
			case 'R':
				return generateSmallRoom(ctx,loc,size);
			case 'P':
				return generatePoolRoom(ctx,loc,size);
			case 'H':
				return generateShelfRoom(ctx,loc,size,dir);
			case 'C':
//				if(size>7) {
//					return generateChasm(loc,size,dir);
//				}else return generateLargeRoom(loc,size);
				return generateLargeRoom(ctx,loc,size);
				
			/*case 'Q':
				return rampUp(loc, size, new Vector(1,1,0));
			case 'E':
				return rampUp(loc, size, new Vector(1,-1,0));*/



			default:
				break;
		}
		return loc;
	}

	private  Vector3 generateSmallBranch(CaveGenContext ctx, Vector3 loc, int size, Vector3 dir) throws MaxChangedBlocksException {
		Vector3 clone = Util.rotateAroundY(dir, 2 * Math.PI * (ctx.rand.nextDouble() * 3/4 + 1.0/8));

		if(size < 7) {
			size = 6;
		}else if(size < 11) {
			size = 7;
		}else {
			size = size/2 + 2;
		}

		loc = generateSmallRoom(ctx,loc,size);

		int newSize = ctx.rand.nextInt(20);
		int sizeMod = ctx.rand.nextInt(1);
		CaveGenerator.generateCave(ctx,size-sizeMod,loc,20+newSize,false,clone);
		return getNext(ctx,'X',loc,size,dir);
	}

	private  BlockVector3 findFloor(CaveGenContext ctx, BlockVector3 loc) {
		while(loc.getY() > 1 && ctx.style.getAirBlock().equalsFuzzy(ctx.getBlock(loc))) {
			loc = loc.add(0,-1,0);
		}
		return loc;
	}

	private  void floodFill(CaveGenContext ctx, BlockStateHolder<?> fill, BlockVector3 loc) throws MaxChangedBlocksException {
		if (ctx.style.getAirBlock().equalsFuzzy(fill)) {
			// would cause an infinite loop
			throw new IllegalArgumentException("Cannot flood fill with air block");
		}
		if(ctx.style.getAirBlock().equalsFuzzy(ctx.getBlock(loc))) {
			ctx.setBlock(loc, fill);
			floodFill(ctx,fill,loc.add(1,0,0));
			floodFill(ctx,fill,loc.add(-1,0,0));
			floodFill(ctx,fill,loc.add(0,0,-1));
			floodFill(ctx,fill,loc.add(0,0,1));
		}
	}

	private  Vector3 generateChasm(CaveGenContext ctx, Vector3 loc, int size, Vector3 caveDir) throws MaxChangedBlocksException {

		ArrayList<Vector3> centers = new ArrayList<>();

		size = size-1;
		Vector3 retVec = caveDir;
		retVec = retVec.multiply(size);
		int chasmSize = ctx.rand.nextInt(2)+3;
		int chasmSizeBackward = ctx.rand.nextInt(3)+2;
		int coinflip = ctx.rand.nextBoolean() ? 1 : -1;
		Vector3 dir = caveDir;
		dir = Util.rotateAroundY(dir, Math.PI / 2 * coinflip);

		int vert = size/2 + 1;
		Vector3 start = loc;
		start = start.add(0,vert*-2,0);

		start = start.add(dir.multiply(-1*size*(chasmSizeBackward)));

		Vector3 og = start;
		chasmSize = chasmSize+chasmSizeBackward;
		for(int i = 0; i < 3; i++) {
			Vector3 set = start;
			set = vary(ctx, set);
			centers.add(set);
			for(int j = 0; j < chasmSize; j++) {
				deleteSphere(ctx,set,size);
				set = set.add(dir.multiply(size));
			}
			start = start.add(0,vert,0);
		}

		for(Vector3 l : centers) {
			centroids.add(l);
			smooth(ctx, l.toBlockPoint(),size+2);
		}

		BlockVector3 lava = findFloor(ctx, og.toBlockPoint()).add(0,2,0);
		//floodFill(Material.LAVA,lava);

		return loc.add(retVec);
	}

	public  Vector3 generateSmallRoom(CaveGenContext ctx, Vector3 loc, int r) throws MaxChangedBlocksException {
		int amount = ctx.rand.nextInt(4)+4;
		r -= 1;

		r = Math.max(r, 4);

		for(int i = 0; i < amount; i++) {
			int tx = ctx.rand.nextInt(r-2)+2;
			int ty = ctx.rand.nextInt(r);
			int tz = ctx.rand.nextInt(r-2)+2;

			if(ctx.rand.nextBoolean()) {tx*=-1; }
			if(ctx.rand.nextBoolean()) {tz*=-1; }

			int sizeMod = ctx.rand.nextInt(1);

			if(ctx.rand.nextBoolean()) {sizeMod*=-1; }

			Vector3 center = loc.add(tx, ty, tz);
			deleteSphere(ctx, center,r+sizeMod);
			centroids.add(center);

		}

		switch (ctx.rand.nextInt(4)) {
			case 0:
				return loc.add(r - 3, 0, 0);
			case 1:
				return loc.add(-1 * r + 3, 0, 0);
			case 2:
				return loc.add(0, 0, -1 * r + 3);
			case 3:
				return loc.add(0, 0, r - 3);
		}

		return loc;
	}

	public  Vector3 generatePoolRoom(CaveGenContext ctx, Vector3 loc, int r) throws MaxChangedBlocksException {
		int amount = ctx.rand.nextInt(4)+3;
		r -= 1;

		for(int i = 0; i < amount; i++) {
			int tx = ctx.rand.nextInt(r-2)+2;
			int ty = ctx.rand.nextInt(r);
			int tz = ctx.rand.nextInt(r-2)+2;

			if(ctx.rand.nextBoolean()) {tx*=-1; }
			if(ctx.rand.nextBoolean()) {tz*=-1; }

			int sizeMod = ctx.rand.nextInt(1);

			if(ctx.rand.nextBoolean()) {sizeMod*=-1; }

			deleteSphere(ctx, loc.add(tx,ty,tz),r+sizeMod);


		}

		for(int i = 0; i < amount-1; i++) {
			int tx = ctx.rand.nextInt(r-3)+2;
			int ty = ctx.rand.nextInt(r-1);
			int tz = ctx.rand.nextInt(r-3)+2;

			if(ctx.rand.nextBoolean()) {tx*=-1; }
			if(ctx.rand.nextBoolean()) {tz*=-1; }

			int sizeMod = ctx.rand.nextInt(1);

			if(ctx.rand.nextBoolean()) {sizeMod*=-1; }

			deleteSphere(ctx, loc.add(tx,-ty,tz),r+sizeMod);


		}

		BlockVector3 pool = findFloor(ctx, loc.toBlockPoint()).add(0,1,0);
		floodFill(ctx, Util.requireDefaultState(BlockTypes.WATER),pool);

		switch (ctx.rand.nextInt(4)) {
			case 0:
				return loc.add(r - 2, 0, 0);
			case 1:
				return loc.add(-1 * r + 2, 0, 0);
			case 2:
				return loc.add(0, 0, -1 * r + 2);
			case 3:
				return loc.add(0, 0, r - 2);
		}



		return loc;
	}

	public  Vector3 generateLargeRoom(CaveGenContext ctx, Vector3 loc, int r) throws MaxChangedBlocksException {
		int amount = ctx.rand.nextInt(5)+3;


		if(r < 3) {
			r = 3;
		}

		for(int i = 0; i < amount; i++) {
			int tx = ctx.rand.nextInt(r-2)+2;
			int ty = ctx.rand.nextInt(r);
			int tz = ctx.rand.nextInt(r-2)+2;

			if(ctx.rand.nextBoolean()) {tx*=-1; }
			if(ctx.rand.nextBoolean()) {tz*=-1; }

			int sizeMod = ctx.rand.nextInt(2);

			if(ctx.rand.nextBoolean()) {sizeMod*=-1; }

			Vector3 center = loc.add(tx, ty, tz);
			deleteSphere(ctx, center,r+sizeMod);
			centroids.add(center);


		}

		switch (ctx.rand.nextInt(4)) {
			case 0:
				return loc.add(2 * r - 2, 0, 0);
			case 1:
				return loc.add(-2 * r + 2, 0, 0);
			case 2:
				return loc.add(0, 0, -2 * r + 2);
			case 3:
				return loc.add(0, 0, 2 * r - 2);
		}

		return loc;
	}

	public  Vector3 generateShelfRoom(CaveGenContext ctx, Vector3 loc, int r, Vector3 direction) throws MaxChangedBlocksException {
		if(ctx.rand.nextBoolean()) {
			return generateShelfFromBottom(ctx,loc,r,direction);
		} else {
			return generateShelfFromTop(ctx,loc,r,direction);
		}
	}

	public  Vector3 generateShelfFromBottom(CaveGenContext ctx, Vector3 loc, int r, Vector3 direction) throws MaxChangedBlocksException {
		Vector3 next = generateLargeRoom(ctx,loc,r);
		next = generateSmallRoom(ctx,next,r);

		int coinflip = ctx.rand.nextBoolean() ? 1 : -1;

		Vector3 shelf = loc.add(0,ctx.rand.nextInt(5)+6,0);
		Vector3 adjust = direction;
		adjust = Util.rotateAroundY(adjust, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * coinflip);
		shelf = shelf.add(adjust);

		Vector3 dir = direction;
		int size = Math.max(r-2,5);

		for(int i = 0; i < 3; i++) {
			shelf = generateSmallRoom(ctx,shelf,size);
			shelf = vary(ctx, shelf);
			shelf = shelf.add(dir.multiply(size));
		}

		return next;
	}

	public  Vector3 generateShelfFromTop(CaveGenContext ctx, Vector3 loc, int r, Vector3 direction) throws MaxChangedBlocksException {


		int coinflip = ctx.rand.nextBoolean() ? 1 : -1;

		Vector3 shelf = loc.add(0,-1*ctx.rand.nextInt(5)+6,0);
		Vector3 adjust = direction;
		adjust = Util.rotateAroundY(adjust, Math.PI / 2 +ctx.rand.nextDouble() * Math.PI / 18 * coinflip);
		shelf = shelf.add(adjust);

		int size = Math.max(r-2,5);
		Vector3 next = loc;
		for(int i = 0; i < 3; i++) {
			next = generateSmallRoom(ctx,next,size);
			next = vary(ctx, next);
			next = shelf.add(direction.multiply(size));
		}

		shelf = generateLargeRoom(ctx,loc,r);
		shelf = generateSmallRoom(ctx,shelf,r);


		return next;
	}


	public  Vector3 rampUp(CaveGenContext ctx, Vector3 loc, int r, Vector3 direction) throws MaxChangedBlocksException {
		Vector3 next = loc;
		for(int i = 0; i < r; i++) {
			next = loc.add(direction);
			deleteSphere(ctx,next,r+getSizeMod(ctx));
		}
		return vary(ctx, next);
	}

	public  void deleteSphere(CaveGenContext ctx, Vector3 loc, int r) throws MaxChangedBlocksException {
		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();

		for(int tx=-r; tx< r+1; tx++){
			for(int ty=-r; ty< r+1; ty++){
				for(int tz=-r; tz< r+1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (r-2) * (r-2)){
						//delete(tx+x, ty+y, tz+z);
						if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == r-2)) {
							continue;
						}
						if(ty+y > 0) {
							ctx.setBlock(BlockVector3.at(tx + x, ty + y, tz + z), ctx.style.getAirBlock());
						}
					}
				}
			}
		}
	}

	public  void createDropshaft(CaveGenContext ctx, Vector3 loc, int r, int length) throws MaxChangedBlocksException {
		int i = 0;
		if(r >= 6) {
			r=r-1;
		}


		while(i < length) {
			loc = vary(ctx, loc);
			int n = ctx.rand.nextInt(2)+2;
			loc = loc.add(0,-n,0);
			i+=n;
			centroids.add(loc);
			deleteSphere(ctx,loc,r);
		}
	}

	public Vector3 getNext(CaveGenContext ctx, char c, Vector3 loc, int r, Vector3 dir) {
		r = r-2;
		loc = vary(ctx, loc);
		Vector3 apply = dir.multiply(r);
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

	public  int getSizeMod(CaveGenContext ctx) {
		return ctx.rand.nextInt(3)-2;
	}

	public void generateOres(CaveGenContext ctx, BlockStateHolder<?> ore, int rarity, int size, int radius, int caveRadius) throws MaxChangedBlocksException {
		for(Vector3 loc : centroids) {
			int chance = ctx.rand.nextInt(rarity);
			if(chance == 0) {
				placeOreCluster(ctx,loc.toBlockPoint(),caveRadius,size,radius,ore);
			}
		}
	}


	public void generateWaterfalls(CaveGenContext ctx, Vector3 loc, int caveRadius, int amount, int rarity, int placeRadius) {
		for(Vector3 l : centroids) {
			if(ctx.rand.nextInt(rarity) == 0) {
				placeWaterfalls(loc,caveRadius,amount,placeRadius);
			}
		}
	}

	public int placeWaterfalls(Vector3 loc, int caveRadius, int amount, int placeRadius) {
		for(int i = 0; i < amount; i++) {
			//TODO
		}

		return 0;

	}

	public int placeOreCluster(CaveGenContext ctx, BlockVector3 loc, int caveRadius, int size, int radius, BlockStateHolder<?> ore) throws MaxChangedBlocksException {
		BlockVector3 toPlace;
		boolean wall = false;
		switch(ctx.rand.nextInt(6)) {
			case 1:
				toPlace = TerrainGenerator.getWall(ctx, loc, size, BlockVector3.at(caveRadius,0,0));
				wall = true;
				ctx.setBlock(toPlace, Util.requireDefaultState(BlockTypes.ORANGE_WOOL));
				break;
			case 2:
				toPlace = TerrainGenerator.getWall(ctx, loc, size, BlockVector3.at(-caveRadius,0,0));
				wall = true;
				ctx.setBlock(toPlace, Util.requireDefaultState(BlockTypes.PURPLE_WOOL));
				break;
			case 3:
				toPlace = TerrainGenerator.getWall(ctx, loc, size, BlockVector3.at(0,0,caveRadius));
				wall = true;
				ctx.setBlock(toPlace, Util.requireDefaultState(BlockTypes.YELLOW_WOOL));
				break;
			case 4:
				toPlace = TerrainGenerator.getWall(ctx, loc, size, BlockVector3.at(0,0,-caveRadius));
				wall = true;
				ctx.setBlock(toPlace, Util.requireDefaultState(BlockTypes.GREEN_WOOL));
				break;
			case 5:
				toPlace = TerrainGenerator.getCeiling(ctx, loc, caveRadius);
				ctx.setBlock(toPlace, Util.requireDefaultState(BlockTypes.WHITE_WOOL));
				break;
			default:
				toPlace = TerrainGenerator.getFloor(ctx, loc, caveRadius);
				ctx.setBlock(toPlace, Util.requireDefaultState(BlockTypes.RED_WOOL));
				break;

		}
		return generateOreCluster(ctx,toPlace,size,radius,ore,wall);

	}

	public int generateOreCluster(CaveGenContext ctx, BlockVector3 loc, int size, int radius, BlockStateHolder<?> ore, boolean wall) throws MaxChangedBlocksException {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		int count = 0;

		for(int tx = -radius; tx< radius +1; tx++){
			for(int ty = -radius; ty< radius +1; ty++){
				for(int tz = -radius; tz< radius +1; tz++){
					if(Math.sqrt(Math.pow(tx, 2)  +  Math.pow(ty, 2)  +  Math.pow(tz, 2)) <= radius -2){
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);

							if(!ctx.style.getAirBlock().equalsFuzzy(ctx.getBlock(pos))) {
								if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == radius -2)) {
									if(ctx.rand.nextBoolean())
										continue;
								}
								ctx.setBlock(pos, ore);
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
