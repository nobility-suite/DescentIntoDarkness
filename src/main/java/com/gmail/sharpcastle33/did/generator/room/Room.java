package com.gmail.sharpcastle33.did.generator.room;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.gmail.sharpcastle33.did.generator.ModuleGenerator;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class Room {
	private final char symbol;
	private final RoomType type;
	private final List<String> tags;

	public Room(char symbol, RoomType type, List<String> tags) {
		this.symbol = symbol;
		this.type = type;
		this.tags = tags;
	}

	public Room(char symbol, RoomType type, ConfigurationSection map) {
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

	public Object[] createUserData(CaveGenContext ctx, RoomData roomData) {
		return null;
	}

	public Vector3 adjustDirection(CaveGenContext ctx, RoomData roomData, Object[] userData) {
		return roomData.direction;
	}

	public Vector3 adjustLocation(CaveGenContext ctx, RoomData roomData, Object[] userData) {
		return ModuleGenerator.vary(ctx, roomData.location).add(roomData.direction.multiply(roomData.caveRadius));
	}

	public abstract void addCentroids(CaveGenContext ctx, RoomData roomData, Object[] userData, List<Centroid> centroids);

	protected abstract void serialize0(ConfigurationSection map);

	public static Room deserialize(char symbol, ConfigurationSection map) {
		RoomType type = ConfigUtil.parseEnum(RoomType.class, ConfigUtil.requireString(map, "type"));
		return type.deserialize(symbol, map);
	}

	public boolean isBranch() {
		return false;
	}

	public char getBranchSymbol() {
		return 0;
	}

}
