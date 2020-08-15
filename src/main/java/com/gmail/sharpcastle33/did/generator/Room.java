package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Room {
	private final char symbol;
	private final Type type;
	private final List<String> tags;

	public Room(char symbol, Type type, List<String> tags) {
		this.symbol = symbol;
		this.type = type;
		this.tags = tags;
	}

	public Room(char symbol, Type type, ConfigurationSection map) {
		this.symbol = symbol;
		this.type = type;
		this.tags = ConfigUtil.deserializeSingleableList(map.get("tags"), Function.identity(), ArrayList::new);
	}

	public char getSymbol() {
		return symbol;
	}

	public List<String> getTags() {
		return tags;
	}

	public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags) {
		return null;
	}

	public Vector3 adjustDirection(CaveGenContext ctx, Vector3 direction, Object[] userData) {
		return direction;
	}

	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
		return ModuleGenerator.vary(ctx, location).add(direction.multiply(caveRadius - 2));
	}

	public abstract void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids);

	public final void serialize(ConfigurationSection map) {
		map.set("type", ConfigUtil.enumToString(type));
		if (!tags.isEmpty()) {
			map.set("tags", ConfigUtil.serializeSingleableList(tags, Function.identity()));
		}
		serialize0(map);
	}

	protected abstract void serialize0(ConfigurationSection map);

	public static Room deserialize(char symbol, ConfigurationSection map) {
		Type type = ConfigUtil.parseEnum(Type.class, ConfigUtil.requireString(map, "type"));
		return type.deserialize(symbol, map);
	}

	public boolean isBranch() {
		return false;
	}

	public static class SimpleRoom extends Room {
		public SimpleRoom(char symbol, List<String> tags) {
			super(symbol, Type.SIMPLE, tags);
		}

		public SimpleRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.SIMPLE, map);
		}

		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			centroids.add(new Centroid(location, caveRadius, tags));
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
		}
	}

	public static class TurnRoom extends Room {
		private final double minAngle;
		private final double maxAngle;

		public TurnRoom(char symbol, List<String> tags, double minAngle, double maxAngle) {
			super(symbol, Type.TURN, tags);
			this.minAngle = minAngle;
			this.maxAngle = maxAngle;
		}

		public TurnRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.TURN, map);
			this.minAngle = ConfigUtil.parseDouble(ConfigUtil.requireString(map, "minAngle"));
			this.maxAngle = ConfigUtil.parseDouble(ConfigUtil.requireString(map, "maxAngle"));
		}

		@Override
		public Vector3 adjustDirection(CaveGenContext ctx, Vector3 direction, Object[] userData) {
			return Util.rotateAroundY(direction, Math.toRadians(minAngle + ctx.rand.nextDouble() * (maxAngle - minAngle)));
		}

		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			centroids.add(new Centroid(location, caveRadius, tags));
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			map.set("minAngle", minAngle);
			map.set("maxAngle", maxAngle);
		}
	}

	public static class BranchRoom extends Room {
		private final double minAngle;
		private final double maxAngle;
		private final int minSizeReduction;
		private final int maxSizeReduction;
		private final int minBranchLength;
		private final int maxBranchLength;

		public BranchRoom(char symbol, List<String> tags, double minAngle, double maxAngle, int minSizeReduction, int maxSizeReduction, int minBranchLength, int maxBranchLength) {
			super(symbol, Type.BRANCH, tags);
			this.minAngle = minAngle;
			this.maxAngle = maxAngle;
			this.minSizeReduction = minSizeReduction;
			this.maxSizeReduction = maxSizeReduction;
			this.minBranchLength = minBranchLength;
			this.maxBranchLength = maxBranchLength;
		}

		public BranchRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.BRANCH, map);
			this.minAngle = map.getDouble("minAngle", 90);
			this.maxAngle = map.getDouble("maxAngle", 90);
			this.minSizeReduction = map.getInt("minSizeReduction", 1);
			this.maxSizeReduction = map.getInt("maxSizeReduction", 1);
			if (minSizeReduction < 1 || maxSizeReduction < minSizeReduction) {
				throw new InvalidConfigException("Invalid size reduction range");
			}
			this.minBranchLength = map.getInt("minBranchLength", 20);
			this.maxBranchLength = map.getInt("maxBranchLength", 39);
			if (minBranchLength <= 0 || maxBranchLength < minBranchLength) {
				throw new InvalidConfigException("Invalid branch length range");
			}
		}

		@Override
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
			return location;
		}

		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			int dir = ctx.rand.nextBoolean() ? 1 : -1;
			int newLength = minBranchLength + ctx.rand.nextInt(maxBranchLength - minBranchLength + 1);
			int sizeReduction = minSizeReduction + ctx.rand.nextInt(maxSizeReduction - minSizeReduction + 1);
			Vector3 newDir = Util.rotateAroundY(direction, Math.toRadians((minAngle + ctx.rand.nextDouble() * (maxAngle - minAngle)) * dir));
			CaveGenerator.generateBranch(ctx, caveRadius - sizeReduction, location, newLength, false, newDir, centroids);
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			map.set("minAngle", minAngle);
			map.set("maxAngle", maxAngle);
			map.set("minSizeReduction", minSizeReduction);
			map.set("maxSizeReduction", maxSizeReduction);
			map.set("minBranchLength", minBranchLength);
			map.set("maxBranchLength", maxBranchLength);
		}

		@Override
		public boolean isBranch() {
			return true;
		}
	}

	public static class DropshaftRoom extends Room {
		private final int minDepth;
		private final int maxDepth;
		private final int minStep;
		private final int maxStep;

		public DropshaftRoom(char symbol, List<String> tags, int minDepth, int maxDepth, int minStep, int maxStep) {
			super(symbol, Type.DROPSHAFT, tags);
			this.minDepth = minDepth;
			this.maxDepth = maxDepth;
			this.minStep = minStep;
			this.maxStep = maxStep;
		}

		public DropshaftRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.DROPSHAFT, map);
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
		public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags) {
			return new Object[] { minDepth + ctx.rand.nextInt(maxDepth - minDepth + 1) };
		}

		@Override
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
			int depth = (Integer) userData[0];
			if (caveRadius <= 7) {
				return location.add(0, -(depth - 4), 0);
			} else {
				return location.add(0, -(depth - 2), 0);
			}
		}

		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			int depth = (Integer) userData[0];
			int i = 0;
			int radius = caveRadius >= 6 ? caveRadius - 1 : caveRadius;
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

	public static class CavernRoom extends Room {
		private final int minCentroids;
		private final int maxCentroids;
		private final int minSpread;
		private final int maxSpread;
		private final int centroidSizeVariance;
		private final int nextLocationScale;
		private final int nextLocationOffset;

		public CavernRoom(char symbol, List<String> tags, int minCentroids, int maxCentroids, int minSpread, int maxSpread, int centroidSizeVariance, int nextLocationScale, int nextLocationOffset) {
			super(symbol, Type.CAVERN, tags);
			this.minCentroids = minCentroids;
			this.maxCentroids = maxCentroids;
			this.minSpread = minSpread;
			this.maxSpread = maxSpread;
			this.centroidSizeVariance = centroidSizeVariance;
			this.nextLocationScale = nextLocationScale;
			this.nextLocationOffset = nextLocationOffset;
		}

		public CavernRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.CAVERN, map);
			this.minCentroids = map.getInt("minCentroids", 4);
			this.maxCentroids = map.getInt("maxCentroids", 7);
			if (minCentroids <= 0 || maxCentroids < minCentroids) {
				throw new InvalidConfigException("Invalid centroid count range");
			}
			this.minSpread = map.getInt("minSpread", 3);
			this.maxSpread = map.getInt("maxSpread", 4);
			if (minSpread < 3 || maxSpread < minSpread) {
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
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
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
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			int count = minCentroids + ctx.rand.nextInt(maxCentroids - minCentroids + 1);

			int spread = caveRadius - 1;
			if (spread < minSpread) {
				spread = minSpread;
			} else if (spread > maxSpread) {
				spread = maxSpread;
			}

			for (int i = 0; i < count; i++) {
				int tx = ctx.rand.nextInt(spread - 2) + 2;
				int ty = ctx.rand.nextInt(spread);
				int tz = ctx.rand.nextInt(spread - 2) + 2;

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

	public static class RavineRoom extends Room {
		// The maximum distance unit spheres can be apart to leave no gaps, if they are arranged in an axis-aligned grid.
		private static final double GAP_FACTOR = 2 / Math.sqrt(3);

		private final int minLength;
		private final int maxLength;
		private final int minHeight;
		private final int maxHeight;
		private final int minWidth;
		private final int maxWidth;
		private final double minTurn;
		private final double maxTurn;
		private final double heightVaryChance;

		public RavineRoom(char symbol, List<String> tags, int minLength, int maxLength, int minHeight, int maxHeight, int minWidth, int maxWidth, int minTurn, int maxTurn, double heightVaryChance) {
			super(symbol, Type.RAVINE, tags);
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
			super(symbol, Type.RAVINE, map);
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
		public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags) {
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
			return new Object[] { length, height, width, turn, origin };
		}

		@Override
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
			int length = (Integer) userData[0];
			int height = (Integer) userData[1];
			int width = (Integer) userData[2];
			double turn = (Double) userData[3];
			Vector3 origin = (Vector3) userData[4];
			// Get an exit location by getting an entrance location but inverting stuff
			return getRandomEntranceLocation(ctx, origin, direction.multiply(-1), length, height, width, -turn);
		}

		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			int length = (Integer) userData[0];
			int height = (Integer) userData[1];
			int width = (Integer) userData[2];
			double turn = (Double) userData[3];
			Vector3 origin = (Vector3) userData[4];

			double turnPerBlock = Math.toRadians(turn / length);
			for (int dir : new int[] {-1, 1}) {
				Vector3 localPosition = origin;
				Vector3 localDirection = Util.rotateAroundY(direction, Math.PI / 2 * dir);
				int distanceSinceCentroids = dir == -1 ? Integer.MAX_VALUE - 1 : 0;

				for (int distance = 0; distance < length / 2; distance++) {
					double localWidth = width * Math.cos((double)distance / length * Math.PI);
					int centroidWidth = Math.max(Math.min((int)Math.ceil(localWidth), 10), 3);
					int centroidRadius = (centroidWidth + 1) / 2;
					double gap = centroidRadius * GAP_FACTOR;
					int numCentroidsAcross = (int)Math.ceil(localWidth / gap);
					int numCentroidsVertically = (int)Math.ceil(height / gap);

					// don't spawn centroids too frequently
					distanceSinceCentroids++;
					if (distanceSinceCentroids > (centroidRadius - 1) * GAP_FACTOR) {
						distanceSinceCentroids = 0;

						Vector3 horizontalVector = Util.rotateAroundY(localDirection, Math.PI / 2);
						for (int y = 0; y < numCentroidsVertically; y++) {
							for (int x = 0; x < numCentroidsAcross; x++) {
								Vector3 centroidPos = localPosition.add(
										horizontalVector.multiply(-localWidth * 0.5 + gap * 0.5 + x * localWidth / numCentroidsAcross)
								).add(0, gap * 0.5 + (double)y * height / numCentroidsVertically, 0);
								centroids.add(new Centroid(centroidPos, centroidRadius + 2, tags));
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

		private Vector3 getRandomEntranceLocation(CaveGenContext ctx, Vector3 origin, Vector3 direction, int length, int height, int width, double turn) {
			Vector3 pos = origin;

			// follow the center of the ravine our chosen distance along it
			final int PROPORTION_OF_LENGTH = 5;
			double turnPerBlock = Math.toRadians(turn / length);
			int distance = ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH) - ctx.rand.nextInt((length + PROPORTION_OF_LENGTH - 1) / PROPORTION_OF_LENGTH);
			Vector3 localDirection = Util.rotateAroundY(direction, Math.copySign(Math.PI / 2, distance));
			for (int i = 0; i < Math.abs(distance); i++) {
				pos = pos.add(localDirection);
				localDirection = Util.rotateAroundY(localDirection, turnPerBlock * Math.signum(distance));
			}

			// move to the edge of the ravine
			double localWidth = width * Math.cos((double)distance / length * Math.PI);
			pos = pos.add(Util.rotateAroundY(localDirection.multiply(localWidth * 0.5), Math.copySign(Math.PI / 2, distance)));

			// pick a random height
			final int PROPORTION_OF_HEIGHT = 5;
			int up = height / 2 + ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT) - ctx.rand.nextInt((height + PROPORTION_OF_HEIGHT - 1) / PROPORTION_OF_HEIGHT);
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

	public static class ShelfRoom extends Room {
		private Room smallRoom;
		private Room largeRoom;
		private final int minShelfHeight;
		private final int maxShelfHeight;
		private final int minShelfSize;
		private final int maxShelfSize;

		public ShelfRoom(char symbol, List<String> tags, int minShelfHeight, int maxShelfHeight, int minShelfSize, int maxShelfSize) {
			super(symbol, Type.SHELF, tags);
			this.minShelfHeight = minShelfHeight;
			this.maxShelfHeight = maxShelfHeight;
			this.minShelfSize = minShelfSize;
			this.maxShelfSize = maxShelfSize;
			createRooms();
		}

		public ShelfRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.SHELF, map);
			this.minShelfHeight = map.getInt("minShelfHeight", 6);
			this.maxShelfHeight = map.getInt("maxShelfHeight", 10);
			if (maxShelfHeight < minShelfHeight) {
				throw new InvalidConfigException("Invalid shelf height range");
			}
			this.minShelfSize = map.getInt("minShelfSize", 3);
			this.maxShelfSize = map.getInt("maxShelfSize", 3);
			if (maxShelfSize < minShelfSize) {
				throw new InvalidConfigException("Invalid shelf size range");
			}
			createRooms();
		}

		private void createRooms() {
			smallRoom = new CavernRoom('r', getTags(), 4, 7, 4, Integer.MAX_VALUE, 0, 1, 3);
			largeRoom = new CavernRoom('l', getTags(), 3, 7, 3, Integer.MAX_VALUE, 1, 2, 2);
		}

		@Override
		public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags) {
			List<Centroid> centroids = new ArrayList<>();
			Vector3 newLocation;
			if (ctx.rand.nextBoolean()) {
				newLocation = generateFromBottom(ctx, location, direction, caveRadius, tags, centroids);
			} else {
				newLocation = generateFromTop(ctx, location, direction, caveRadius, tags, centroids);
			}
			return new Object[] { newLocation, centroids };
		}

		private Vector3 generateFromBottom(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, List<Centroid> centroids) {
			Vector3 next = location;
			next = generateRoom(largeRoom, ctx, next, direction, caveRadius, tags, centroids);
			next = generateRoom(smallRoom, ctx, next, direction, caveRadius, tags, centroids);

			Vector3 shelf = location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
			int dir = ctx.rand.nextBoolean() ? 1 : -1;
			shelf = shelf.add(Util.rotateAroundY(direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

			int shelfRadius = Math.max(caveRadius - 2, 5);
			int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
			for (int i = 0; i < shelfSize; i++) {
				shelf = generateRoom(smallRoom, ctx, shelf, direction, shelfRadius, tags, centroids);
				shelf = ModuleGenerator.vary(ctx, shelf);
				shelf = shelf.add(direction.multiply(shelfRadius));
			}

			return next;
		}

		private Vector3 generateFromTop(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, List<Centroid> centroids) {
			Vector3 shelf = location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
			int dir = ctx.rand.nextBoolean() ? 1 : -1;
			shelf = shelf.add(Util.rotateAroundY(direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

			int shelfRadius = Math.max(caveRadius - 2, 5);
			int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
			Vector3 next = location;
			for (int i = 0; i < shelfSize; i++) {
				next = generateRoom(smallRoom, ctx, next, direction, shelfRadius, tags, centroids);
				next = ModuleGenerator.vary(ctx, next);
				next = next.add(direction.multiply(shelfRadius));
			}

			shelf = generateRoom(largeRoom, ctx, shelf, direction, caveRadius, tags, centroids);
			shelf = generateRoom(smallRoom, ctx, shelf, direction, caveRadius, tags, centroids);

			return next;
		}

		private Vector3 generateRoom(Room room, CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, List<Centroid> centroids) {
			Object[] userData = room.createUserData(ctx, location, direction, caveRadius, tags);
			room.addCentroids(ctx, location, direction, caveRadius, tags, userData, centroids);
			return room.adjustLocation(ctx, location, direction, caveRadius, userData);
		}

		@Override
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
			return (Vector3) userData[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
			centroids.addAll((List<Centroid>) userData[1]);
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
			map.set("minShelfHeight", minShelfHeight);
			map.set("maxShelfHeight", maxShelfHeight);
			map.set("minShelfSize", minShelfSize);
			map.set("maxShelfSize", maxShelfSize);
		}
	}

	public static class NilRoom extends Room {
		public NilRoom(char symbol, List<String> tags) {
			super(symbol, Type.NIL, tags);
		}

		public NilRoom(char symbol, ConfigurationSection map) {
			super(symbol, Type.NIL, map);
		}

		@Override
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
			return location;
		}

		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<String> tags, Object[] userData, List<Centroid> centroids) {
		}

		@Override
		protected void serialize0(ConfigurationSection map) {
		}
	}

	public enum Type {
		SIMPLE(SimpleRoom::new),
		TURN(TurnRoom::new),
		BRANCH(BranchRoom::new),
		DROPSHAFT(DropshaftRoom::new),
		CAVERN(CavernRoom::new),
		RAVINE(RavineRoom::new),
		SHELF(ShelfRoom::new),
		NIL(NilRoom::new),
		;

		private final BiFunction<Character, ConfigurationSection, Room> deserializer;

		Type(BiFunction<Character, ConfigurationSection, Room> deserializer) {
			this.deserializer = deserializer;
		}

		public Room deserialize(char symbol, ConfigurationSection map) {
			return deserializer.apply(symbol, map);
		}
	}
}
