package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class TurnRoom extends Room {
	private final double minAngle;
	private final double maxAngle;

	public TurnRoom(char symbol, List<String> tags, double minAngle, double maxAngle) {
		super(symbol, RoomType.TURN, tags);
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
	}

	public TurnRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.TURN, map);
		this.minAngle = ConfigUtil.parseDouble(ConfigUtil.requireString(map, "minAngle"));
		this.maxAngle = ConfigUtil.parseDouble(ConfigUtil.requireString(map, "maxAngle"));
	}

	@Override
	public Vector3 adjustDirection(CaveGenContext ctx, Vector3 direction, Object[] userData) {
		return Util.rotateAroundY(direction, Math.toRadians(minAngle + ctx.rand.nextDouble() * (maxAngle - minAngle)));
	}

	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts) {
		centroids.add(new Centroid(location, caveRadius, tags));
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minAngle", minAngle);
		map.set("maxAngle", maxAngle);
	}
}
