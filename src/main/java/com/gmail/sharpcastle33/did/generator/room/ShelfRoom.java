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
	public Object[] createUserData(CaveGenContext ctx, RoomData roomData) {
		List<Centroid> centroids = new ArrayList<>();
		Vector3 newLocation;
		if (ctx.rand.nextBoolean()) {
			newLocation = generateFromBottom(ctx, roomData, centroids);
		} else {
			newLocation = generateFromTop(ctx, roomData, centroids);
		}
		Util.ensureConnected(centroids, roomData.caveRadius, pos -> new Centroid(pos, roomData.caveRadius, roomData));
		return new Object[]{newLocation, centroids};
	}

	private Vector3 generateFromBottom(CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
		Vector3 next = roomData.location;
		next = generateRoom(largeRoom, ctx, roomData.withLocation(next), centroids);
		next = generateRoom(smallRoom, ctx, roomData.withLocation(next), centroids);

		Vector3 shelf = roomData.location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
		int dir = ctx.rand.nextBoolean() ? 1 : -1;
		shelf = shelf.add(Util.rotateAroundY(roomData.direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

		int shelfRadius = Math.max(roomData.caveRadius, 5);
		int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
		for (int i = 0; i < shelfSize; i++) {
			shelf = generateRoom(smallRoom, ctx, roomData.withLocation(shelf).withCaveRadius(shelfRadius), centroids);
			shelf = ModuleGenerator.vary(ctx, shelf);
			shelf = shelf.add(roomData.direction.multiply(shelfRadius));
		}

		return next;
	}

	private Vector3 generateFromTop(CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
		Vector3 shelf = roomData.location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
		int dir = ctx.rand.nextBoolean() ? 1 : -1;
		shelf = shelf.add(Util.rotateAroundY(roomData.direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

		int shelfRadius = Math.max(roomData.caveRadius, 5);
		int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
		Vector3 next = roomData.location;
		Vector3 newLocation = roomData.location;
		for (int i = 0; i < shelfSize; i++) {
			next = generateRoom(smallRoom, ctx, roomData.withLocation(next).withCaveRadius(shelfRadius), centroids);
			next = ModuleGenerator.vary(ctx, next);
			newLocation = next;
			next = next.add(roomData.direction.multiply(shelfRadius));
		}

		shelf = generateRoom(largeRoom, ctx, roomData.withLocation(shelf), centroids);
		shelf = generateRoom(smallRoom, ctx, roomData.withLocation(shelf), centroids);

		return newLocation;
	}

	private Vector3 generateRoom(Room room, CaveGenContext ctx, RoomData roomData, List<Centroid> centroids) {
		Object[] userData = room.createUserData(ctx, roomData);
		room.addCentroids(ctx, roomData, userData, centroids);
		Vector3 direction = room.adjustDirection(ctx, roomData, userData);
		return room.adjustLocation(ctx, roomData.withDirection(direction), userData);
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, RoomData roomData, Object[] userData) {
		return (Vector3) userData[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addCentroids(CaveGenContext ctx, RoomData roomData, Object[] userData, List<Centroid> centroids) {
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
