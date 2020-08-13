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
	protected final List<String> tags;

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

	public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius) {
		return null;
	}

	public Vector3 adjustDirection(CaveGenContext ctx, Vector3 direction, Object[] userData) {
		return direction;
	}

	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
		return ModuleGenerator.vary(ctx, location).add(direction.multiply(caveRadius - 2));
	}

	public abstract void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids);

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
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids) {
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
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids) {
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
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids) {
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
		public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius) {
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
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids) {
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
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids) {
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
			smallRoom = new CavernRoom('r', tags, 4, 7, 4, Integer.MAX_VALUE, 0, 1, 3);
			largeRoom = new CavernRoom('l', tags, 3, 7, 3, Integer.MAX_VALUE, 1, 2, 2);
		}

		@Override
		public Object[] createUserData(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius) {
			List<Centroid> centroids = new ArrayList<>();
			Vector3 newLocation;
			if (ctx.rand.nextBoolean()) {
				newLocation = generateFromBottom(ctx, location, direction, caveRadius, centroids);
			} else {
				newLocation = generateFromTop(ctx, location, direction, caveRadius, centroids);
			}
			return new Object[] { newLocation, centroids };
		}

		private Vector3 generateFromBottom(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<Centroid> centroids) {
			Vector3 next = location;
			next = generateRoom(largeRoom, ctx, next, direction, caveRadius, centroids);
			next = generateRoom(smallRoom, ctx, next, direction, caveRadius, centroids);

			Vector3 shelf = location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
			int dir = ctx.rand.nextBoolean() ? 1 : -1;
			shelf = shelf.add(Util.rotateAroundY(direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

			int shelfRadius = Math.max(caveRadius - 2, 5);
			int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
			for (int i = 0; i < shelfSize; i++) {
				shelf = generateRoom(smallRoom, ctx, shelf, direction, shelfRadius, centroids);
				shelf = ModuleGenerator.vary(ctx, shelf);
				shelf = shelf.add(direction.multiply(shelfRadius));
			}

			return next;
		}

		private Vector3 generateFromTop(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<Centroid> centroids) {
			Vector3 shelf = location.add(0, minShelfHeight + ctx.rand.nextInt(maxShelfHeight - minShelfHeight + 1), 0);
			int dir = ctx.rand.nextBoolean() ? 1 : -1;
			shelf = shelf.add(Util.rotateAroundY(direction, Math.PI / 2 + ctx.rand.nextDouble() * Math.PI / 18 * dir));

			int shelfRadius = Math.max(caveRadius - 2, 5);
			int shelfSize = minShelfSize + ctx.rand.nextInt(maxShelfSize - minShelfSize + 1);
			Vector3 next = location;
			for (int i = 0; i < shelfSize; i++) {
				next = generateRoom(smallRoom, ctx, next, direction, shelfRadius, centroids);
				next = ModuleGenerator.vary(ctx, next);
				next = next.add(direction.multiply(shelfRadius));
			}

			shelf = generateRoom(largeRoom, ctx, shelf, direction, caveRadius, centroids);
			shelf = generateRoom(smallRoom, ctx, shelf, direction, caveRadius, centroids);

			return next;
		}

		private Vector3 generateRoom(Room room, CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, List<Centroid> centroids) {
			Object[] userData = room.createUserData(ctx, location, direction, caveRadius);
			room.addCentroids(ctx, location, direction, caveRadius, userData, centroids);
			return room.adjustLocation(ctx, location, direction, caveRadius, userData);
		}

		@Override
		public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData) {
			return (Vector3) userData[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius, Object[] userData, List<Centroid> centroids) {
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

	public enum Type {
		SIMPLE(SimpleRoom::new),
		TURN(TurnRoom::new),
		BRANCH(BranchRoom::new),
		DROPSHAFT(DropshaftRoom::new),
		CAVERN(CavernRoom::new),
		SHELF(ShelfRoom::new),
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
