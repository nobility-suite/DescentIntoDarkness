package com.gmail.sharpcastle33.did.generator.painter;

import java.util.HashMap;
import java.util.Map;

public enum PainterStepType {
	REPLACE_ALL("replace_all"),
	REPLACE_CEILING("replace_ceiling"),
	REPLACE_FLOOR("replace_floor"),
	CEILING_LAYER("ceiling_layer"),
	FLOOR_LAYER("floor_layer"),
	REPLACE_MESA("replace_mesa"),
	;
	private final String name;

	PainterStepType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
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
