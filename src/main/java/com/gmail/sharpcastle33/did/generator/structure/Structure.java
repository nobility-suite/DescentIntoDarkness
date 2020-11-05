package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class Structure {
	private final String name;
	private final StructureType type;
	protected final List<StructurePlacementEdge> edges;
	private final double chance;
	private final int count;
	protected final List<BlockStateHolder<?>> canPlaceOn;
	protected final List<BlockStateHolder<?>> canReplace;
	private final List<Direction> validDirections = new ArrayList<>();
	private final List<String> tags;
	private final boolean tagsInverted;

	protected Structure(String name, StructureType type, ConfigurationSection map) {
		this.name = name;
		this.type = type;
		this.edges = ConfigUtil.deserializeSingleableList(map.get("edges"), val -> ConfigUtil.parseEnum(StructurePlacementEdge.class, val), () -> Lists.newArrayList(StructurePlacementEdge.values()));
		this.chance = map.getDouble("chance", 1);
		this.count = map.getInt("count", 1);
		this.canPlaceOn = deserializePlacementRule(map.get("canPlaceOn"));
		this.canReplace = deserializePlacementRule(map.get("canReplace"));
		this.tags = ConfigUtil.deserializeSingleableList(map.get("tags"), Function.identity(), ArrayList::new);
		this.tagsInverted = map.getBoolean("tagsInverted", !map.contains("tags"));
		computeValidDirections();
	}

	protected Structure(String name, StructureType type, List<StructurePlacementEdge> edges, double chance, int count, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<String> tags, boolean tagsInverted) {
		this.name = name;
		this.type = type;
		this.edges = edges;
		this.chance = chance;
		this.count = count;
		this.canPlaceOn = canPlaceOn;
		this.canReplace = canReplace;
		this.tags = tags;
		this.tagsInverted = tagsInverted;
		computeValidDirections();
	}

	private void computeValidDirections() {
		if (edges.isEmpty()) {
			throw new InvalidConfigException("No edges to choose from");
		}
		for (StructurePlacementEdge edge : edges) {
			Collections.addAll(validDirections, edge.getDirections());
		}
	}

	public final String getName() {
		return name;
	}

	public final StructureType getType() {
		return type;
	}

	public List<Direction> getValidDirections() {
		return validDirections;
	}

	public double getChance() {
		return chance;
	}

	public int getCount() {
		return count;
	}

	public boolean canPlaceOn(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canPlaceOn == null) {
			return !ctx.style.isTransparentBlock(block);
		} else {
			return canPlaceOn.stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

	public List<String> getTags() {
		return tags;
	}

	public boolean areTagsInverted() {
		return tagsInverted;
	}

	public void serialize(ConfigurationSection map) {
		map.set("type", ConfigUtil.enumToString(type));
		map.set("edges", ConfigUtil.serializeSingleableList(edges, ConfigUtil::enumToString));
		map.set("chance", chance);
		map.set("count", count);
		if (canPlaceOn != null) {
			map.set("canPlaceOn", ConfigUtil.serializeSingleableList(canPlaceOn, BlockStateHolder::getAsString));
		}
		if (canReplace != null) {
			map.set("canReplace", ConfigUtil.serializeSingleableList(canReplace, BlockStateHolder::getAsString));
		}
		if (!tags.isEmpty()) {
			map.set("tags", ConfigUtil.serializeSingleableList(tags, Function.identity()));
		}
		serialize0(map);
	}

	protected abstract void serialize0(ConfigurationSection map);

	public static Structure deserialize(String name, ConfigurationSection map) {
		StructureType type = ConfigUtil.parseEnum(StructureType.class, ConfigUtil.requireString(map, "type"));
		return type.deserialize(name, map);
	}

	protected static List<StructurePlacementEdge> deserializeEdges(Object edges) {
		List<StructurePlacementEdge> ret = ConfigUtil.deserializeSingleableList(edges, val -> ConfigUtil.parseEnum(StructurePlacementEdge.class, val), () -> Lists.newArrayList(StructurePlacementEdge.values()));
		if (ret.isEmpty()) {
			throw new InvalidConfigException("No edges to choose from");
		}
		return ret;
	}

	protected static List<BlockStateHolder<?>> deserializePlacementRule(Object rule) {
		return ConfigUtil.deserializeSingleableList(rule, ConfigUtil::parseBlock, () -> null);
	}

	public abstract void place(CaveGenContext ctx, BlockVector3 pos, Direction side, boolean force) throws WorldEditException;

	protected boolean canReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
		if (canReplace == null) {
			return ctx.style.isTransparentBlock(block);
		} else {
			return canReplace.stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

}
