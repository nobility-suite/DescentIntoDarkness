package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.generator.room.RoomData;
import com.sk89q.worldedit.math.Vector3;

import java.util.List;

public class Centroid {
	public Vector3 pos;
	public final int size;
	public final List<String> tags;
	public final int roomIndex;

	public Centroid(Vector3 pos, int size, RoomData roomData) {
		this(pos, size, roomData.tags, roomData.roomIndex);
	}

	public Centroid(Vector3 pos, int size, List<String> tags, int roomIndex) {
		this.pos = pos;
		this.size = size;
		this.tags = tags;
		this.roomIndex = roomIndex;
	}
}
