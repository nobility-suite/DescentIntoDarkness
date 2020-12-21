package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class DropshaftRoom extends Room {
	private final int minDepth;
	private final int maxDepth;
	private final int minStep;
	private final int maxStep;

	public DropshaftRoom(char symbol, List<String> tags, int minDepth, int maxDepth, int minStep, int maxStep) {
		super(symbol, RoomType.DROPSHAFT, tags);
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
		this.minStep = minStep;
		this.maxStep = maxStep;
	}

	public DropshaftRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.DROPSHAFT, map);
		this.minDepth = map.getInt("minDepth", 8);
		this.maxDepth = map.getInt("maxDepth", 11);
		if (minDepth <= 0 || maxDepth < minDepth) {
			throw new InvalidConfigException("Invalid depth range");
		}
		this.minStep = map.getInt("minStep", 2);
		this.maxStep = map.getInt("maxStep", 3);
		if (minStep <= 0 || maxStep < minStep) {
			throw new InvalidConfigException("Invalid step range");
		}
	}

	@Override
	public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								   List<String> tags, List<List<Vector3>> roomLocations) {
		return new Object[]{minDepth + ctx.rand.nextInt(maxDepth - minDepth + 1)};
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								  Object[] userData) {
		int depth = (Integer) userData[0];
		if (caveRadius <= 5) {
			return location.add(0, -(depth - 4), 0);
		} else {
			return location.add(0, -(depth - 2), 0);
		}
	}

	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts, List<List<Vector3>> roomLocations) {
		int depth = (Integer) userData[0];
		int i = 0;
		int radius = caveRadius >= 4 ? caveRadius - 1 : caveRadius;
		Vector3 loc = location;
		while (i < depth) {
			centroids.add(new Centroid(loc, radius, tags));
			loc = ModuleGenerator.vary(ctx, loc);
			int step = minStep + ctx.rand.nextInt(maxStep - minStep + 1);
			loc = loc.add(0, -step, 0);
			i += step;
		}
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minDepth", minDepth);
		map.set("maxDepth", maxDepth);
		map.set("minStep", minStep);
		map.set("maxStep", maxStep);
	}
}
