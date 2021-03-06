package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ShelfRoom extends Room {
	private Room smallRoom;
	private Room largeRoom;
	private final int minShelfHeight;
	private final int maxShelfHeight;
	private final int minShelfSize;
	private final int maxShelfSize;

	public ShelfRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.SHELF, map);
		this.minShelfHeight = map.getInt("minShelfHeight", 6);
		this.maxShelfHeight = map.getInt("maxShelfHeight", 10);
		if (maxShelfHeight < minShelfHeight) {
			throw new InvalidConfigException("Invalid shelf height range");
		}
		this.minShelfSize = map.getInt("minShelfSize", 3);
		this.maxShelfSize = map.getInt("maxShelfSize", 3);
		if (maxShelfSize < minShelfSize) {
			throw new InvalidConfigException("Invalid shelf size range");
		}
		createRooms();
	}

	private void createRooms() {
		smallRoom = new CavernRoom('r', getTags(), 4, 7, 4, Integer.MAX_VALUE, 0, 0, 90);
		largeRoom = new CavernRoom('l', getTags(), 3, 7, 3, Integer.MAX_VALUE, 1, 0, 90);
	}

	@Override
	public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								   List<String> tags, List<List<Vector3>> roomLocations) {
		List<Centroid> centroids = new ArrayList<>();
		List<Integer> roomStarts = new ArrayList<>();
		Vector3 newLocation;
		if (ctx.rand.nextBoolean()) {
			newLocation = generateFromBottom(ctx, location, direction, caveRadius, tags, centroids, roomStarts,
					roomLocations);
		} else {
			newLocation = generateFromTop(ctx, location, direction, caveRadius, tags, centroids, roomStarts,
					roomLocations);
		}
		Util.ensureConnected(centroids, caveRadius, pos -> new Centroid(pos, caveRadius, tags));
		return new Object[]{newLocation, centroids};
	}

	private Vector3 generateFromBottom(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
									   List<String> tags, List<Centroid> centroids, List<Integer> roomStarts,
									   List<List<Vector3>> roomLocations) {
		Vector3 next = location;
		next = generateRoom(largeRoom, ctx, next, direction, caveRadius, tags, centroids, roomStarts, roomLocations);
		next = generateRoom(smallRoom, ctx, next, direction, caveRadius, tags, centroids, roomStarts, roomLocations);

		Vector3 shelf = location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
		int dir = ctx.rand.nextBoolean() ? 1 : -1;
		shelf = shelf.add(Util.rotateAroundY(direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

		int shelfRadius = Math.max(caveRadius, 5);
		int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
		for (int i = 0; i < shelfSize; i++) {
			shelf = generateRoom(smallRoom, ctx, shelf, direction, shelfRadius, tags, centroids, roomStarts,
					roomLocations);
			shelf = ModuleGenerator.vary(ctx, shelf);
			shelf = shelf.add(direction.multiply(shelfRadius));
		}

		return next;
	}

	private Vector3 generateFromTop(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
									List<String> tags, List<Centroid> centroids, List<Integer> roomStarts,
									List<List<Vector3>> roomLocations) {
		Vector3 shelf = location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
		int dir = ctx.rand.nextBoolean() ? 1 : -1;
		shelf = shelf.add(Util.rotateAroundY(direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

		int shelfRadius = Math.max(caveRadius, 5);
		int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
		Vector3 next = location;
		Vector3 newLocation = location;
		for (int i = 0; i < shelfSize; i++) {
			next = generateRoom(smallRoom, ctx, next, direction, shelfRadius, tags, centroids, roomStarts, roomLocations);
			next = ModuleGenerator.vary(ctx, next);
			newLocation = next;
			next = next.add(direction.multiply(shelfRadius));
		}

		shelf = generateRoom(largeRoom, ctx, shelf, direction, caveRadius, tags, centroids, roomStarts, roomLocations);
		shelf = generateRoom(smallRoom, ctx, shelf, direction, caveRadius, tags, centroids, roomStarts, roomLocations);

		return newLocation;
	}

	private Vector3 generateRoom(Room room, CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								 List<String> tags, List<Centroid> centroids, List<Integer> roomStarts,
								 List<List<Vector3>> roomLocations) {
		Object[] userData = room.createUserData(ctx, location, direction, caveRadius, tags, roomLocations);
		room.addCentroids(ctx, location, direction, caveRadius, tags, userData, centroids, roomStarts, roomLocations);
		direction = room.adjustDirection(ctx, direction, userData);
		return room.adjustLocation(ctx, location, direction, caveRadius, userData);
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								  Object[] userData) {
		return (Vector3) userData[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts, List<List<Vector3>> roomLocations) {
		centroids.addAll((List<Centroid>) userData[1]);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minShelfHeight", minShelfHeight);
		map.set("maxShelfHeight", maxShelfHeight);
		map.set("minShelfSize", minShelfSize);
		map.set("maxShelfSize", maxShelfSize);
	}
}
