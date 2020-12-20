package com.gmail.sharpcastle33.did.generator.structure;

import org.bukkit.configuration.ConfigurationSection;

import java.util.function.BiFunction;

public enum StructureType {
	SCHEMATIC(SchematicStructure::new),
	VEIN(VeinStructure::new),
	PATCH(PatchStructure::new),
	VINE_PATCH(VinePatchStructure::new),
	GLOWSTONE(GlowstoneStructure::new),
	WATERFALL(WaterfallStructure::new),
	TREE(TreeStructure::new),
	STALAGMITE(StalagmiteStructure::new),
	CHORUS_PLANT(ChorusPlantStructure::new),
	;

	private final BiFunction<String, ConfigurationSection, Structure> deserializer;

	StructureType(BiFunction<String, ConfigurationSection, Structure> deserializer) {
		this.deserializer = deserializer;
	}

	public Structure deserialize(String name, ConfigurationSection map) {
		return deserializer.apply(name, map);
	}
}
