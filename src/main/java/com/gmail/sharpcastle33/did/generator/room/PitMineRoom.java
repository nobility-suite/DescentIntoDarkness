package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class PitMineRoom extends Room {
	private final int minSteps;
	private final int maxSteps;
	private final int minStepHeight;
	private final int maxStepHeight;
	private final int minStepWidth;
	private final int maxStepWidth;
	private final int minBaseWidth;
	private final int maxBaseWidth;
	private final double minStepVariance;
	private final double maxStepVariance;

	public PitMineRoom(char symbol, List<String> tags, int minSteps, int maxSteps, int minStepHeight,
					   int maxStepHeight, int minStepWidth, int maxStepWidth, int minBaseWidth, int maxBaseWidth,
					   double minStepVariance, double maxStepVariance) {
		super(symbol, RoomType.PIT_MINE, tags);
		this.minSteps = minSteps;
		this.maxSteps = maxSteps;
		this.minStepHeight = minStepHeight;
		this.maxStepHeight = maxStepHeight;
		this.minStepWidth = minStepWidth;
		this.maxStepWidth = maxStepWidth;
		this.minBaseWidth = minBaseWidth;
		this.maxBaseWidth = maxBaseWidth;
		this.minStepVariance = minStepVariance;
		this.maxStepVariance = maxStepVariance;
	}

	public PitMineRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.PIT_MINE, map);
		this.minSteps = map.getInt("minSteps", 3);
		this.maxSteps = map.getInt("maxSteps", 5);
		if (minSteps <= 0 || maxSteps < minSteps) {
			throw new InvalidConfigException("Invalid steps range");
		}
		this.minStepHeight = map.getInt("minStepHeight", 2);
		this.maxStepHeight = map.getInt("maxStepHeight", 5);
		if (minStepHeight < 0 || maxStepHeight < minStepHeight) {
			throw new InvalidConfigException("Invalid step height range");
		}
		this.minStepWidth = map.getInt("minStepWidth", 4);
		this.maxStepWidth = map.getInt("maxStepWidth", 7);
		if (minStepWidth < 0 || maxStepWidth < minStepWidth) {
			throw new InvalidConfigException("Invalid step width range");
		}
		this.minBaseWidth = map.getInt("minBaseWidth", 15);
		this.maxBaseWidth = map.getInt("maxBaseWidth", 45);
		if (minBaseWidth <= 0 || maxBaseWidth < minBaseWidth) {
			throw new InvalidConfigException("Invalid base width range");
		}
		this.minStepVariance = map.getDouble("minStepVariance", -2);
		this.maxStepVariance = map.getDouble("maxStepVariance", 2);
		if (-minStepVariance > minStepWidth || maxStepVariance < minStepVariance) {
			throw new InvalidConfigException("Invalid step variance range");
		}
	}

	@Override
	public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								   List<String> tags, List<List<Vector3>> roomLocations) {
		int numSteps = minSteps + ctx.rand.nextInt(maxSteps - minSteps + 1);
		List<Step> steps = new ArrayList<>(numSteps);
		int radius = (minBaseWidth + ctx.rand.nextInt(maxBaseWidth - minBaseWidth + 1) + 1) / 2;
		int dy = 0;
		for (int i = 0; i < numSteps; i++) {
			int height = minStepHeight + ctx.rand.nextInt(maxStepHeight - minStepHeight + 1);
			steps.add(new Step(
					location.add(0, dy, 0),
					radius + minStepVariance + ctx.rand.nextDouble() * (maxStepVariance - minStepVariance),
					radius + minStepVariance + ctx.rand.nextDouble() * (maxStepVariance - maxStepVariance),
					2 * Math.PI * ctx.rand.nextDouble(),
					height
			));
			dy += height;
			radius += minStepWidth + ctx.rand.nextInt(maxStepWidth - minStepWidth + 1);
		}

		int entranceStep = ctx.rand.nextInt(numSteps);
		Vector3 entrancePos = steps.get(entranceStep).getEdge(Math.PI + Math.atan2(direction.getZ(),
				direction.getX()));
		// entrancePos should actually be at location, shift everything by this vector
		Vector3 shift = location.subtract(entrancePos);
		for (Step step : steps) {
			step.center = step.center.add(shift);
		}

		return new Object[]{steps};
	}

	@SuppressWarnings("unchecked")
	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								  Object[] userData) {
		List<Step> steps = (List<Step>) userData[0];
		Step exitStep = steps.get(ctx.rand.nextInt(steps.size()));
		double exitAngle = Math.atan2(direction.getZ(), direction.getX());
		exitAngle += -Math.PI / 2 + ctx.rand.nextDouble() * Math.PI; // -90 to 90 degrees
		return exitStep.getEdge(exitAngle);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts, List<List<Vector3>> roomLocations) {
		List<Step> steps = (List<Step>) userData[0];
		for (Step step : steps) {
			int centroidWidth = Math.max(3, Math.min(10, Math.min(step.height, (int) Math.ceil(Math.min(step.rx,
					step.rz)))));
			int centroidRadius = (centroidWidth + 1) / 2;
			double gap = centroidRadius * RavineRoom.GAP_FACTOR;
			int numCentroidsVertically = (int) Math.ceil(step.height / gap);
			int numCentroidRings = (int) Math.ceil(0.5 * (step.rx + step.rz) / gap);
			for (int ring = 0; ring < numCentroidRings; ring++) {
				double rx = gap * 0.5 + ring * step.rx / numCentroidRings;
				double rz = gap * 0.5 + ring * step.rz / numCentroidRings;
				int numCentroidsAround = (int) Math.ceil(Math.PI * (step.rx + step.rz) / gap);
				for (int d = 0; d < numCentroidsAround; d++) {
					double angle = Math.PI * 2 / numCentroidsAround * d;
					Vector3 xzPos = step.center.add(Util.rotateAroundY(Vector3.at(rx * Math.cos(angle), 0,
							rz * Math.sin(angle)), step.angle));
					for (int y = 0; y < numCentroidsVertically; y++) {
						centroids.add(new Centroid(xzPos.add(0,
								gap * 0.5 + (double) y * step.height / numCentroidsVertically, 0), centroidRadius,
								tags));
					}
				}
			}
		}
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minSteps", minSteps);
		map.set("maxSteps", maxSteps);
		map.set("minStepHeight", minStepHeight);
		map.set("maxStepHeight", maxStepHeight);
		map.set("minStepWidth", minStepWidth);
		map.set("maxStepWidth", maxStepWidth);
		map.set("minBaseWidth", minBaseWidth);
		map.set("maxBaseWidth", maxBaseWidth);
		map.set("minStepVariance", minStepVariance);
		map.set("maxStepVariance", maxStepVariance);
	}

	private static class Step {
		private Vector3 center;
		private final double rx;
		private final double rz;
		private final double angle;
		private final int height;

		private Step(Vector3 center, double rx, double rz, double angle, int height) {
			this.center = center;
			this.rx = rx;
			this.rz = rz;
			this.angle = angle;
			this.height = height;
		}

		public Vector3 getEdge(double angle) {
			return center.add(Util.rotateAroundY(Vector3.at(rx * Math.cos(angle - this.angle), height * 0.5,
					rz * Math.sin(angle - this.angle)), this.angle));
		}
	}
}
