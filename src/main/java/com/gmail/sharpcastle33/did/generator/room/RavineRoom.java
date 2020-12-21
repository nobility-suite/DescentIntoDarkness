package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class RavineRoom extends Room {
	/**
	 * The maximum distance unit spheres can be apart to leave no gaps, if they are arranged in an axis-aligned grid.
	 */
	public static final double GAP_FACTOR = 2 / Math.sqrt(3);

	private final int minLength;
	private final int maxLength;
	private final int minHeight;
	private final int maxHeight;
	private final int minWidth;
	private final int maxWidth;
	private final double minTurn;
	private final double maxTurn;
	private final double heightVaryChance;

	public RavineRoom(char symbol, List<String> tags, int minLength, int maxLength, int minHeight, int maxHeight,
					  int minWidth, int maxWidth, int minTurn, int maxTurn, double heightVaryChance) {
		super(symbol, RoomType.RAVINE, tags);
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.minWidth = minWidth;
		this.maxWidth = maxWidth;
		this.minTurn = minTurn;
		this.maxTurn = maxTurn;
		this.heightVaryChance = heightVaryChance;
	}

	public RavineRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.RAVINE, map);
		this.minLength = map.getInt("minLength", 70);
		this.maxLength = map.getInt("maxLength", 100);
		if (minLength <= 0 || maxLength < minLength) {
			throw new InvalidConfigException("Invalid length range");
		}
		this.minHeight = map.getInt("minHeight", 80);
		this.maxHeight = map.getInt("maxHeight", 120);
		if (minHeight <= 0 || maxHeight < minHeight) {
			throw new InvalidConfigException("Invalid height range");
		}
		this.minWidth = map.getInt("minWidth", 10);
		this.maxWidth = map.getInt("maxWidth", 20);
		if (minWidth <= 0 || maxWidth < minWidth) {
			throw new InvalidConfigException("Invalid width range");
		}
		this.minTurn = map.getDouble("minTurn", 0);
		this.maxTurn = map.getDouble("maxTurn", 30);
		this.heightVaryChance = map.getDouble("heightVaryChance", 0.2);
	}

	@Override
	public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								   List<String> tags, List<List<Vector3>> roomLocations) {
		int length = minLength + ctx.rand.nextInt(maxLength - minLength + 1);
		int height = minHeight + ctx.rand.nextInt(maxHeight - minHeight + 1);
		int width = minWidth + ctx.rand.nextInt(maxWidth - minWidth + 1);
		double turn = minTurn + ctx.rand.nextDouble() * (maxTurn - minTurn);
		if (ctx.rand.nextBoolean()) {
			turn = -turn;
		}
		// move the origin to the center of the ravine
		Vector3 origin = location.add(direction.multiply(width * 0.5));
		Vector3 entrance = getRandomEntranceLocation(ctx, origin, direction, length, height, width, turn);
		// entrance should in fact be at location, move the origin of the ravine
		origin = origin.add(location.subtract(entrance));
		return new Object[]{length, height, width, turn, origin};
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								  Object[] userData) {
		int length = (Integer) userData[0];
		int height = (Integer) userData[1];
		int width = (Integer) userData[2];
		double turn = (Double) userData[3];
		Vector3 origin = (Vector3) userData[4];
		// Get an exit location by getting an entrance location but inverting stuff
		return getRandomEntranceLocation(ctx, origin, direction.multiply(-1), length, height, width, -turn);
	}

	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts, List<List<Vector3>> roomLocations) {
		int length = (Integer) userData[0];
		int height = (Integer) userData[1];
		int width = (Integer) userData[2];
		double turn = (Double) userData[3];
		Vector3 origin = (Vector3) userData[4];

		double turnPerBlock = Math.toRadians(turn / length);
		for (int dir : new int[]{-1, 1}) {
			Vector3 localPosition = origin;
			Vector3 localDirection = Util.rotateAroundY(direction, Math.PI / 2 * dir);
			int distanceSinceCentroids = dir == -1 ? Integer.MAX_VALUE - 1 : 0;

			for (int distance = 0; distance < length / 2; distance++) {
				double localWidth = width * Math.cos((double) distance / length * Math.PI);
				int centroidWidth = Math.max(Math.min((int) Math.ceil(localWidth), 10), 3);
				int centroidRadius = (centroidWidth + 1) / 2;
				double gap = centroidRadius * GAP_FACTOR;
				int numCentroidsAcross = (int) Math.ceil(localWidth / gap);
				int numCentroidsVertically = (int) Math.ceil(height / gap);

				// don't spawn centroids too frequently
				distanceSinceCentroids++;
				if (distanceSinceCentroids > (centroidRadius - 1) * GAP_FACTOR) {
					distanceSinceCentroids = 0;

					Vector3 horizontalVector = Util.rotateAroundY(localDirection, Math.PI / 2);
					for (int y = 0; y < numCentroidsVertically; y++) {
						for (int x = 0; x < numCentroidsAcross; x++) {
							Vector3 centroidPos = localPosition.add(
									horizontalVector.multiply(-localWidth * 0.5 + gap * 0.5 + x * localWidth / numCentroidsAcross)
							).add(0, gap * 0.5 + (double) y * height / numCentroidsVertically, 0);
							centroids.add(new Centroid(centroidPos, centroidRadius, tags));
						}
					}
				}

				localPosition = localPosition.add(localDirection);
				if (ctx.rand.nextDouble() < heightVaryChance) {
					localPosition = ModuleGenerator.vary(ctx, localPosition);
				}
				localDirection = Util.rotateAroundY(localDirection, turnPerBlock * dir);
			}
		}
	}

	private Vector3 getRandomEntranceLocation(CaveGenContext ctx, Vector3 origin, Vector3 direction, int length,
											  int height, int width, double turn) {
		Vector3 pos = origin;

		// follow the center of the ravine our chosen distance along it
		final int PROPORTION_OF_LENGTH = 5;
		double turnPerBlock = Math.toRadians(turn / length);
		int distance =
				ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH) - ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH);
		Vector3 localDirection = Util.rotateAroundY(direction, Math.copySign(Math.PI / 2, distance));
		for (int i = 0; i < Math.abs(distance); i++) {
			pos = pos.add(localDirection);
			localDirection = Util.rotateAroundY(localDirection, turnPerBlock * Math.signum(distance));
		}

		// move to the edge of the ravine
		double localWidth = width * Math.cos((double) distance / length * Math.PI);
		pos = pos.add(Util.rotateAroundY(localDirection.multiply(localWidth * 0.5), Math.copySign(Math.PI / 2,
				distance)));

		// pick a random height
		final int PROPORTION_OF_HEIGHT = 5;
		int up =
				height / 2 + ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT) - ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT);
		pos = pos.add(0, up, 0);

		return pos;
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minLength", minLength);
		map.set("maxLength", maxLength);
		map.set("minHeight", minHeight);
		map.set("maxHeight", maxHeight);
		map.set("minWidth", minWidth);
		map.set("maxWidth", maxWidth);
		map.set("minTurn", minTurn);
		map.set("maxTurn", maxTurn);
		map.set("heightVaryChance", heightVaryChance);
	}
}
