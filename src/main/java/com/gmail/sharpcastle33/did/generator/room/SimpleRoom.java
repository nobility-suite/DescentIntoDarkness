package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class SimpleRoom extends Room {
	public SimpleRoom(char symbol, List<String> tags) {
		super(symbol, RoomType.SIMPLE, tags);
	}

	public SimpleRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.SIMPLE, map);
	}

	@Override
	public void addCentroids(CaveGenContext ctx, RoomData roomData, Object[] userData, List<Centroid> centroids) {
		centroids.add(new Centroid(roomData.location, roomData.caveRadius, roomData));
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
	}
}
