package com.gmail.sharpcastle33.did.generator.painter;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum PainterStepType {
	REPLACE_ALL("replace_all", ReplaceAllStep::new),
	REPLACE_CEILING("replace_ceiling", ReplaceCeilingStep::new),
	REPLACE_FLOOR("replace_floor", ReplaceFloorStep::new),
	CEILING_LAYER("ceiling_layer", CeilingLayerStep::new),
	FLOOR_LAYER("floor_layer", FloorLayerStep::new),
	REPLACE_MESA("replace_mesa", ReplaceMesaStep::new),
	;
	private final String name;
	private final Function<ConfigurationSection, PainterStep> parser;

	PainterStepType(String name, Function<ConfigurationSection, PainterStep> parser) {
		this.name = name;
		this.parser = parser;
	}

	public String getName() {
		return name;
	}

	public PainterStep parse(ConfigurationSection map) {
		return parser.apply(map);
	}

	public static PainterStepType byName(String name) {
		return BY_NAME.get(name);
	}

	private static final Map<String, PainterStepType> BY_NAME = new HashMap<>();

	static {
		for (PainterStepType type : values()) {
			BY_NAME.put(type.getName(), type);
		}
	}
}
