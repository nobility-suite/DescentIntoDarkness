package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class CavernRoom extends Room {
	private final int minCentroids;
	private final int maxCentroids;
	private final int minSpread;
	private final int maxSpread;
	private final int centroidSizeVariance;
	private final int nextLocationScale;
	private final int nextLocationOffset;

	public CavernRoom(char symbol, List<String> tags, int minCentroids, int maxCentroids, int minSpread, int maxSpread
			, int centroidSizeVariance, int nextLocationScale, int nextLocationOffset) {
		super(symbol, RoomType.CAVERN, tags);
		this.minCentroids = minCentroids;
		this.maxCentroids = maxCentroids;
		this.minSpread = minSpread;
		this.maxSpread = maxSpread;
		this.centroidSizeVariance = centroidSizeVariance;
		this.nextLocationScale = nextLocationScale;
		this.nextLocationOffset = nextLocationOffset;
	}

	public CavernRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.CAVERN, map);
		this.minCentroids = map.getInt("minCentroids", 4);
		this.maxCentroids = map.getInt("maxCentroids", 7);
		if (minCentroids <= 0 || maxCentroids < minCentroids) {
			throw new InvalidConfigException("Invalid centroid count range");
		}
		this.minSpread = map.getInt("minSpread", 1);
		this.maxSpread = map.getInt("maxSpread", 2);
		if (minSpread < 1 || maxSpread < minSpread) {
			throw new InvalidConfigException("Invalid spread range");
		}
		this.centroidSizeVariance = map.getInt("centroidSizeVariance", 0);
		if (centroidSizeVariance < 0) {
			throw new InvalidConfigException("Invalid centroid size variance");
		}
		this.nextLocationScale = map.getInt("nextLocationScale", 1);
		this.nextLocationOffset = map.getInt("nextLocationOffset", 3);
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								  Object[] userData) {
		switch (ctx.rand.nextInt(4)) {
			case 0:
				return location.add(nextLocationScale * caveRadius - nextLocationOffset, 0, 0);
			case 1:
				return location.add(-nextLocationScale * caveRadius + nextLocationOffset, 0, 0);
			case 2:
				return location.add(0, 0, nextLocationScale * caveRadius - nextLocationOffset);
			case 3:
				return location.add(0, 0, -nextLocationScale * caveRadius + nextLocationOffset);
			default:
				throw new AssertionError("What?");
		}
	}

	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts) {
		int count = minCentroids + ctx.rand.nextInt(maxCentroids - minCentroids + 1);

		int spread = caveRadius - 1;
		if (spread < minSpread) {
			spread = minSpread;
		} else if (spread > maxSpread) {
			spread = maxSpread;
		}

		for (int i = 0; i < count; i++) {
			int tx = ctx.rand.nextInt(spread) + 2;
			int ty = ctx.rand.nextInt(spread + 2);
			int tz = ctx.rand.nextInt(spread) + 2;

			if (ctx.rand.nextBoolean()) {
				tx = -tx;
			}
			if (ctx.rand.nextBoolean()) {
				tz = -tz;
			}

			int sizeMod = ctx.rand.nextInt(centroidSizeVariance + 1);
			if (ctx.rand.nextBoolean()) {
				sizeMod = -sizeMod;
			}

			centroids.add(new Centroid(location.add(tx, ty, tz), spread + sizeMod, tags));
		}
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minCentroids", minCentroids);
		map.set("maxCentroids", maxCentroids);
		map.set("minSpread", minSpread);
		map.set("maxSpread", maxSpread);
		map.set("centroidSizeVariance", centroidSizeVariance);
		map.set("nextLocationScale", nextLocationScale);
		map.set("nextLocationOffset", nextLocationOffset);
	}
}
