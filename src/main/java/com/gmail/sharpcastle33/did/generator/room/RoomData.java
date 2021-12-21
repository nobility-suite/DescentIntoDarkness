package com.gmail.sharpcastle33.did.generator.room;

import com.sk89q.worldedit.math.Vector3;

import java.util.List;

public final class RoomData {
	public final Vector3 location;
	public final Vector3 direction;
	public final int caveRadius;
	public final List<String> tags;
	public final List<List<Vector3>> roomLocations;
	public final int roomIndex;

	public RoomData(Vector3 location, Vector3 direction, int caveRadius, List<String> tags, List<List<Vector3>> roomLocations, int roomIndex) {
		this.location = location;
		this.direction = direction;
		this.caveRadius = caveRadius;
		this.tags = tags;
		this.roomLocations = roomLocations;
		this.roomIndex = roomIndex;
	}

	public RoomData withLocation(Vector3 location) {
		return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
	}

	public RoomData withDirection(Vector3 direction) {
		return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
	}

	public RoomData withCaveRadius(int caveRadius) {
		return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
	}

	public RoomData withTags(List<String> tags) {
		return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
	}

	public RoomData withRoomLocations(List<List<Vector3>> roomLocations) {
		return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
	}

	public RoomData withRoomIndex(int roomIndex) {
		return new RoomData(location, direction, caveRadius, tags, roomLocations, roomIndex);
	}
}
