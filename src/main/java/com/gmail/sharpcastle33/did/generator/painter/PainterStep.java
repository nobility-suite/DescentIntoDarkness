package com.gmail.sharpcastle33.did.generator.painter;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class PainterStep {
	private final PainterStepType type;
	private final List<String> tags;
	private final boolean tagsInverted;

	public PainterStep(PainterStepType type, ConfigurationSection map) {
		this.type = type;
		this.tags = ConfigUtil.deserializeSingleableList(map.get("tags"), Function.identity(), ArrayList::new);
		this.tagsInverted = map.getBoolean("tagsInverted", !map.contains("tags"));
	}

	public final PainterStepType getType() {
		return type;
	}

	public final List<String> getTags() {
		return tags;
	}

	public boolean areTagsInverted() {
		return tagsInverted;
	}

	public static PainterStep deserialize(ConfigurationSection map) {
		PainterStepType type = ConfigUtil.parseEnum(PainterStepType.class, ConfigUtil.requireString(map, "type"));
		return type.parse(map);
	}

	public abstract void apply(CaveGenContext ctx, Centroid centroid, Predicate<BlockVector3> canTryToPaint) throws MaxChangedBlocksException;

}
