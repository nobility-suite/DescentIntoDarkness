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

public class CavernRoom extends Room {
	private final int minCentroids;
	private final int maxCentroids;
	private final int minSpread;
	private final int maxSpread;
	private final int centroidSizeVariance;
	private final int minTurn;
	private final int maxTurn;

	public CavernRoom(char symbol, List<String> tags, int minCentroids, int maxCentroids, int minSpread, int maxSpread, int centroidSizeVariance, int minTurn, int maxTurn) {
		super(symbol, RoomType.CAVERN, tags);
		this.minCentroids = minCentroids;
		this.maxCentroids = maxCentroids;
		this.minSpread = minSpread;
		this.maxSpread = maxSpread;
		this.centroidSizeVariance = centroidSizeVariance;
		this.minTurn = minTurn;
		this.maxTurn = maxTurn;
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
		this.minTurn = map.getInt("minTurn", 0);
		this.maxTurn = map.getInt("maxTurn", 90);
	}

	@Override
	public Object[] createUserData(CaveGenContext ctx, RoomData roomData) {
		List<Centroid> centroids = new ArrayList<>();

		int count = minCentroids + ctx.rand.nextInt(maxCentroids - minCentroids + 1);

		int spread = roomData.caveRadius - 1;
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

			centroids.add(new Centroid(roomData.location.add(tx, ty, tz), spread + sizeMod, roomData));
		}

		if (count > 0) {
			double minDot = Double.POSITIVE_INFINITY;
			Vector3 minPos = null;
			for (Centroid centroid : centroids) {
				double dot = centroid.pos.dot(roomData.direction);
				if (dot < minDot) {
					minDot = dot;
					minPos = centroid.pos;
				}
			}
			assert minPos != null;

			Vector3 shift = roomData.location.subtract(minPos);
			for (Centroid centroid : centroids) {
				centroid.pos = centroid.pos.add(shift);
			}

			Util.ensureConnected(centroids, roomData.caveRadius, pos -> new Centroid(pos, roomData.caveRadius, roomData));
		}

		return new Object[] {centroids};
	}

	@Override
	public Vector3 adjustDirection(CaveGenContext ctx, RoomData roomData, Object[] userData) {
		double angle = minTurn + ctx.rand.nextDouble() * (maxTurn - minTurn);
		if (ctx.rand.nextBoolean()) {
			angle = -angle;
		}
		return Util.rotateAroundY(roomData.direction, Math.toRadians(angle));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Vector3 adjustLocation(CaveGenContext ctx,
								  RoomData roomData, Object[] userData) {
		List<Centroid> centroids = (List<Centroid>) userData[0];
		if (centroids.size() > 0) {
			double maxDot = Double.NEGATIVE_INFINITY;
			Vector3 maxPos = null;
			for (Centroid centroid : centroids) {
				double dot  = centroid.pos.dot(roomData.direction);
				if (dot > maxDot) {
					maxDot = dot;
					maxPos = centroid.pos;
				}
			}
			assert maxPos != null;

			return ModuleGenerator.vary(ctx, maxPos).add(roomData.direction.multiply(roomData.caveRadius));
		}

		return super.adjustLocation(ctx, roomData, userData);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addCentroids(CaveGenContext ctx,
							 RoomData roomData, Object[] userData, List<Centroid> centroids) {
		centroids.addAll((List<Centroid>) userData[0]);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minCentroids", minCentroids);
		map.set("maxCentroids", maxCentroids);
		map.set("minSpread", minSpread);
		map.set("maxSpread", maxSpread);
		map.set("centroidSizeVariance", centroidSizeVariance);
		map.set("minTurn", minTurn);
		map.set("maxTurn", maxTurn);
	}
}
