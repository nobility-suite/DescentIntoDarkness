package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class BranchRoom extends Room {
	private final double minAngle;
	private final double maxAngle;
	private final int minSizeReduction;
	private final int maxSizeReduction;
	private final int minBranchLength;
	private final int maxBranchLength;
	private final char branchSymbol;

	public BranchRoom(char symbol, List<String> tags, double minAngle, double maxAngle, int minSizeReduction,
					  int maxSizeReduction, int minBranchLength, int maxBranchLength, char branchSymbol) {
		super(symbol, RoomType.BRANCH, tags);
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
		this.minSizeReduction = minSizeReduction;
		this.maxSizeReduction = maxSizeReduction;
		this.minBranchLength = minBranchLength;
		this.maxBranchLength = maxBranchLength;
		this.branchSymbol = branchSymbol;
	}

	public BranchRoom(char symbol, ConfigurationSection map) {
		super(symbol, RoomType.BRANCH, map);
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
		String branchStr = map.getString("branchSymbol", "C");
		if (branchStr == null || branchStr.length() != 1) {
			throw new InvalidConfigException("branchSymbol must be a character");
		}
		this.branchSymbol = branchStr.charAt(0);
	}

	@Override
	public Vector3 adjustLocation(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
								  Object[] userData) {
		return location;
	}

	@Override
	public void addCentroids(CaveGenContext ctx, Vector3 location, Vector3 direction, int caveRadius,
							 List<String> tags, Object[] userData, List<Centroid> centroids,
							 List<Integer> roomStarts, List<List<Vector3>> roomLocations) {
		int dir = ctx.rand.nextBoolean() ? 1 : -1;
		int newLength = minBranchLength + ctx.rand.nextInt(maxBranchLength - minBranchLength + 1);
		int sizeReduction = minSizeReduction + ctx.rand.nextInt(maxSizeReduction - minSizeReduction + 1);
		Vector3 newDir = Util.rotateAroundY(direction,
				Math.toRadians((minAngle + ctx.rand.nextDouble() * (maxAngle - minAngle)) * dir));
		CaveGenerator.generateBranch(ctx, caveRadius - sizeReduction, location, newLength, branchSymbol, false, newDir, centroids,
				roomStarts, roomLocations);
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("minAngle", minAngle);
		map.set("maxAngle", maxAngle);
		map.set("minSizeReduction", minSizeReduction);
		map.set("maxSizeReduction", maxSizeReduction);
		map.set("minBranchLength", minBranchLength);
		map.set("maxBranchLength", maxBranchLength);
		map.set("branchSymbol", String.valueOf(branchSymbol));
	}

	@Override
	public boolean isBranch() {
		return true;
	}

	@Override
	public char getBranchSymbol() {
		return branchSymbol;
	}
}
