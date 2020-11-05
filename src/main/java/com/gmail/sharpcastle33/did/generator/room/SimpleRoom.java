package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.math.Vector3;
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
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts) {
		centroids.add(new Centroid(location, caveRadius, tags));
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
	}
}
