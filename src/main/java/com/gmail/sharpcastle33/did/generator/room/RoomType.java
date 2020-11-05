package com.gmail.sharpcastle33.did.generator.room;

import org.bukkit.configuration.ConfigurationSection;

import java.util.function.BiFunction;

public enum RoomType {
	SIMPLE(SimpleRoom::new),
	TURN(TurnRoom::new),
	VERTICAL(VerticalRoom::new),
	BRANCH(BranchRoom::new),
	DROPSHAFT(DropshaftRoom::new),
	CAVERN(CavernRoom::new),
	RAVINE(RavineRoom::new),
	PIT_MINE(PitMineRoom::new),
	SHELF(ShelfRoom::new),
	NIL(NilRoom::new),
	;

	private final BiFunction<Character, ConfigurationSection, Room> deserializer;

	RoomType(BiFunction<Character, ConfigurationSection, Room> deserializer) {
		this.deserializer = deserializer;
	}

	public Room deserialize(char symbol, ConfigurationSection map) {
		return deserializer.apply(symbol, map);
	}
}
