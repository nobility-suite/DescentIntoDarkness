package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.generator.GrammarGraph;
import com.gmail.sharpcastle33.did.generator.PainterStep;
import com.gmail.sharpcastle33.did.generator.Room;
import com.gmail.sharpcastle33.did.generator.Structure;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CaveStyle {
	// meta properties
	private final String name;
	private boolean isAbstract;

	// block properties
	private BlockStateHolder<?> airBlock;
	private BlockStateHolder<?> baseBlock;
	private final List<BlockStateHolder<?>> transparentBlocks = new ArrayList<>();

	// spawning properties
	private final List<Ore> ores = new ArrayList<>();
	private final List<MobSpawnEntry> spawnEntries = new ArrayList<>();
	private float naturalPollutionIncrease;
	private int spawnAttemptsPerTick;
	private int sprintingPenalty;

	// cave generation
	private int minLength;
	private int maxLength;
	private int minSize;
	private int maxSize;
	private final List<Room> rooms = new ArrayList<>();
	private GrammarGraph grammar;
	private final List<PainterStep> painterSteps = new ArrayList<>();
	private final List<Structure> structures = new ArrayList<>();
	private final List<Structure> portals = new ArrayList<>();

	public CaveStyle(String name) {
		this.name = name;
	}

	public void serialize(ConfigurationSection map) {
		map.set("abstract", isAbstract);

		map.set("airBlock", airBlock.getAsString());
		map.set("baseBlock", baseBlock.getAsString());
		map.set("transparentBlocks", transparentBlocks.stream().map(BlockStateHolder::getAsString).collect(Collectors.toCollection(ArrayList::new)));
		ConfigurationSection oresSection = map.createSection("ores");
		for (Ore ore : ores) {
			ore.serialize(oresSection.createSection(ore.getName()));
		}
		ConfigurationSection spawnEntriesSection = map.createSection("spawnEntries");
		for (MobSpawnEntry spawnEntry : spawnEntries) {
			spawnEntry.serialize(spawnEntriesSection.createSection(spawnEntry.getName()));
		}
		map.set("naturalPollutionIncrease", (double)naturalPollutionIncrease);
		map.set("spawnAttemptsPerTick", spawnAttemptsPerTick);
		map.set("sprintingPenalty", sprintingPenalty);

		map.set("minLength", minLength);
		map.set("maxLength", maxLength);
		map.set("minSize", minSize);
		map.set("maxSize", maxSize);
		grammar.serialize(map.createSection("grammar"));
		ConfigurationSection roomsSection = map.createSection("rooms");
		for (Room room : rooms) {
			room.serialize(roomsSection.createSection(String.valueOf(room.getSymbol())));
		}
		map.set("painterSteps", painterSteps.stream().map(PainterStep::serialize).collect(Collectors.toCollection(ArrayList::new)));
		ConfigurationSection structuresSection = map.createSection("structures");
		for (Structure structure : structures) {
			structure.serialize(structuresSection.createSection(structure.getName()));
		}
		ConfigurationSection portalsSection = map.createSection("portals");
		for (Structure portal : portals) {
			portal.serialize(portalsSection.createSection(portal.getName()));
		}
	}

	public static CaveStyle deserialize(String name, ConfigurationSection map) {
		System.out.println("====== " + name + " ======");
		map.getValues(true).forEach((key, val) -> System.out.println(key + ": " + val));

		CaveStyle style = new CaveStyle(name);

		style.isAbstract = map.getBoolean("abstract", false);

		String airBlock = map.getString("airBlock");
		if (airBlock != null) {
			style.airBlock = ConfigUtil.parseBlock(airBlock);
		}
		String baseBlock = map.getString("baseBlock");
		if (baseBlock != null) {
			style.baseBlock = ConfigUtil.parseBlock(baseBlock);
		}
		List<?> transparentBlocks = map.getList("transparentBlocks");
		if (transparentBlocks != null) {
			style.transparentBlocks.clear();
			transparentBlocks.stream().map(block -> ConfigUtil.parseBlock(block.toString())).forEachOrdered(style.transparentBlocks::add);
		}
		ConfigurationSection oresSection = map.getConfigurationSection("ores");
		if (oresSection != null) {
			style.ores.clear();
			for (String key : oresSection.getKeys(false)) {
				ConfigurationSection oreSection = oresSection.getConfigurationSection(key);
				if (oreSection != null) {
					style.ores.add(Ore.deserialize(key, oreSection));
				}
			}
		}
		ConfigurationSection spawnEntriesSection = map.getConfigurationSection("spawnEntries");
		if (spawnEntriesSection != null) {
			style.spawnEntries.clear();
			for (String key : spawnEntriesSection.getKeys(false)) {
				ConfigurationSection spawnEntrySection = spawnEntriesSection.getConfigurationSection(key);
				if (spawnEntrySection != null) {
					style.spawnEntries.add(MobSpawnEntry.deserialize(key, spawnEntrySection));
				}
			}
		}
		style.naturalPollutionIncrease = (float)map.getDouble("naturalPollutionIncrease", 0.1);
		style.spawnAttemptsPerTick = map.getInt("spawnAttemptsPerTick", 10);
		style.sprintingPenalty = map.getInt("sprintingPenalty", 5);


		style.minLength = map.getInt("minLength", 90);
		style.maxLength = map.getInt("maxLength", 90);
		if (style.minLength <= 0 || style.maxLength < style.minLength) {
			throw new InvalidConfigException("Illegal length range");
		}
		style.minSize = map.getInt("minSize", 7);
		style.maxSize = map.getInt("maxSize", 11);
		if (style.minSize < 1 || style.maxSize < style.minSize) {
			throw new InvalidConfigException("Illegal size range");
		}
		ConfigurationSection roomsSection = map.getConfigurationSection("rooms");
		if (roomsSection != null) {
			style.rooms.clear();
			for (String key : roomsSection.getKeys(false)) {
				if (key.length() != 1) {
					throw new InvalidConfigException("Room symbol must be a single character");
				}
				ConfigurationSection roomSection = roomsSection.getConfigurationSection(key);
				if (roomSection != null) {
					style.rooms.add(Room.deserialize(key.charAt(0), roomSection));
				}
			}
		}
		ConfigurationSection grammarSection = map.getConfigurationSection("grammar");
		if (grammarSection != null) {
			style.grammar = GrammarGraph.deserialize(grammarSection);
			if (!style.isAbstract) {
				style.grammar.validate(style.rooms.stream().map(Room::getSymbol).collect(Collectors.toSet()));
			}
		}

		List<?> painterSteps = map.getList("painterSteps");
		if (painterSteps != null) {
			style.painterSteps.clear();
			painterSteps.stream().map(PainterStep::deserialize).forEachOrdered(style.painterSteps::add);
		}
		ConfigurationSection structuresSection = map.getConfigurationSection("structures");
		if (structuresSection != null) {
			style.structures.clear();
			for (String key : structuresSection.getKeys(false)) {
				ConfigurationSection structureSection = structuresSection.getConfigurationSection(key);
				if (structureSection != null) {
					style.structures.add(Structure.deserialize(key, structureSection));
				}
			}
		}
		ConfigurationSection portalsSection = map.getConfigurationSection("portals");
		if (portalsSection != null) {
			style.portals.clear();
			for (String key : portalsSection.getKeys(false)) {
				ConfigurationSection portalSection = portalsSection.getConfigurationSection(key);
				if (portalSection != null) {
					style.portals.add(Structure.deserialize(key, portalSection));
				}
			}
		}
		return style;
	}

	public String getName() {
		return name;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public BlockStateHolder<?> getAirBlock() {
		return airBlock;
	}

	public BlockStateHolder<?> getBaseBlock() {
		return baseBlock;
	}

	public boolean isTransparentBlock(BlockStateHolder<?> block) {
		if (airBlock.equalsFuzzy(block)) {
			return true;
		}
		for (BlockStateHolder<?> transparentBlock : transparentBlocks) {
			if (transparentBlock.equalsFuzzy(block)) {
				return true;
			}
		}
		return false;
	}

	public List<Ore> getOres() {
		return ores;
	}

	public List<MobSpawnEntry> getSpawnEntries() {
		return spawnEntries;
	}

	public float getNaturalPollutionIncrease() {
		return naturalPollutionIncrease;
	}

	public int getSpawnAttemptsPerTick() {
		return spawnAttemptsPerTick;
	}

	public int getSprintingPenalty() {
		return sprintingPenalty;
	}

	public int getMinLength() {
		return minLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public int getMinSize() {
		return minSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public List<Room> getRooms() {
		return rooms;
	}

	public GrammarGraph getGrammar() {
		return grammar;
	}

	public List<PainterStep> getPainterSteps() {
		return painterSteps;
	}

	public List<Structure> getStructures() {
		return structures;
	}

	public List<Structure> getPortals() {
		return portals;
	}
}
