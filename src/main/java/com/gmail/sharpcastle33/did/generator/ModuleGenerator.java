package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.gmail.sharpcastle33.did.generator.room.Room;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;

public class ModuleGenerator {

	public static void read(CaveGenContext ctx, LayoutGenerator.Layout layout, Vector3 start, Vector3 dir, int caveRadius, List<Centroid> centroids, List<Integer> roomStarts, List<List<Vector3>> roomLocations) {
		String cave = layout.getValue();
		Bukkit.getLogger().log(Level.INFO, "Beginning module generation... " + cave.length() + " modules.");
		Bukkit.getLogger().log(Level.INFO, "Cave string: " + cave);

		Map<Character, Room> rooms = ctx.style.getRooms().stream()
				.collect(Collectors.groupingBy(Room::getSymbol, Collectors.reducing(null, (a, b) -> a == null ? b : a)));

		int roomStartIndex = roomStarts.size();

		List<Vector3> theseRoomLocations = new ArrayList<>();
		roomLocations.add(theseRoomLocations);

		Vector3 location = start;
		for (int i = 0; i < cave.length(); i++) {
			roomStarts.add(centroids.size());
			Room room = rooms.get(cave.charAt(i));
			List<String> tags = new ArrayList<>(layout.getTags().get(i));
			tags.addAll(room.getTags());
			Object[] userData = room.createUserData(ctx, location, dir, caveRadius, tags, roomLocations);
			room.addCentroids(ctx, location, dir, caveRadius, tags, userData, centroids, roomStarts, roomLocations);
			dir = room.adjustDirection(ctx, dir, userData);
			location = room.adjustLocation(ctx, location, dir, caveRadius, userData);
			theseRoomLocations.add(location);
		}

		for (int i = roomStartIndex; i < roomStarts.size(); i++) {
			int roomStart = roomStarts.get(i);
			int roomEnd = i == roomStarts.size() - 1 ? centroids.size() : roomStarts.get(i + 1);
			List<Centroid> roomCentroids = centroids.subList(roomStart, roomEnd);
			int minRoomY = roomCentroids.stream().mapToInt(centroid -> centroid.pos.getBlockY() - centroid.size).min().orElse(0);
			int maxRoomY = roomCentroids.stream().mapToInt(centroid -> centroid.pos.getBlockY() + centroid.size).max().orElse(255);
			for (Centroid centroid : roomCentroids) {
				deleteCentroid(ctx, centroid, minRoomY, maxRoomY);
			}
		}
	}

	private static void deleteCentroid(CaveGenContext ctx, Centroid centroid, int minRoomY, int maxRoomY) {
		int x = centroid.pos.getBlockX();
		int y = centroid.pos.getBlockY();
		int z = centroid.pos.getBlockZ();
		int r = centroid.size;

		for(int ty = -r; ty <= r; ty++) {
			BlockStateHolder<?> airBlock = ctx.style.getAirBlock(ty + y, centroid, minRoomY, maxRoomY);
			for(int tx = -r; tx <= r; tx++){
				for(int tz = -r; tz <= r; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= r * r){
						if (((tx != 0 || ty != 0) && (tx != 0 || tz != 0) && (ty != 0 || tz != 0)) || (Math.abs(tx + ty + tz) != r)) {
							ctx.setBlock(BlockVector3.at(tx + x, ty + y, tz + z), airBlock);
						}
					}
				}
			}
		}
	}

	public static Vector3 vary(CaveGenContext ctx, Vector3 loc) {
		int x = ctx.rand.nextInt(2 * ctx.style.getCentroidVaryHorizontal() + 1) - ctx.style.getCentroidVaryHorizontal();
		int y = ctx.style.getCentroidVaryMinY() + ctx.rand.nextInt(ctx.style.getCentroidVaryMaxY() - ctx.style.getCentroidVaryMinY() + 1);
		int z = ctx.rand.nextInt(2 * ctx.style.getCentroidVaryHorizontal() + 1) - ctx.style.getCentroidVaryHorizontal();
		return loc.add(x,y,z);
	}

	public static int generateOreCluster(CaveGenContext ctx, BlockVector3 loc, int radius, Predicate<BlockStateHolder<?>> oldBlocks, BlockStateHolder<?> ore) throws MaxChangedBlocksException {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		int count = 0;

		for(int tx = -radius; tx< radius +1; tx++){
			for(int ty = -radius; ty< radius +1; ty++){
				for(int tz = -radius; tz< radius +1; tz++){
					if(tx * tx  +  ty * ty  +  tz * tz <= (radius - 2) * (radius - 2)) {
						if(ty+y > 0) {
							BlockVector3 pos = BlockVector3.at(tx+x, ty+y, tz+z);
							BlockState block = ctx.getBlock(pos);
							if(oldBlocks.test(block)) {
								if(((tx == 0 && ty == 0) || (tx == 0 && tz == 0) || (ty == 0 && tz == 0)) && (Math.abs(tx+ty+tz) == radius - 2)) {
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
