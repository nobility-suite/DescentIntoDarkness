package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class NilRoom extends Room {
	public NilRoom(char symbol, List<String> tags) {
		super(symbol, RoomType.NIL, tags);
	}

	public NilRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.NIL, map);
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, RoomData roomData, Object[] userData) {
		return roomData.location;
	}

	@Override
	public void addCentroids(CaveGenContext ctx, RoomData roomData, Object[] userData, List<Centroid> centroids) {
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
	}
}
