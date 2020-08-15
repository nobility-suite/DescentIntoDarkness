package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.generator.GrammarGraph;
import com.gmail.sharpcastle33.did.generator.PainterStep;
import com.gmail.sharpcastle33.did.generator.Room;
import com.gmail.sharpcastle33.did.generator.Structure;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CaveStyle {
	public static final CaveStyle DEFAULT = new CaveStyle("default");

	private final String name;

	// block properties
	private BlockStateHolder<?> airBlock = Util.requireDefaultState(BlockTypes.AIR);
	private BlockStateHolder<?> baseBlock = Util.requireDefaultState(BlockTypes.STONE);
	private final List<BlockStateHolder<?>> transparentBlocks = Lists.newArrayList(
			Util.requireDefaultState(BlockTypes.AIR),
			Util.requireDefaultState(BlockTypes.GLOWSTONE),
			FuzzyBlockState.builder().type(Objects.requireNonNull(BlockTypes.WATER)).build(),
			FuzzyBlockState.builder().type(Objects.requireNonNull(BlockTypes.LAVA)).build()
	);
	private final List<Ore> ores = Lists.newArrayList(
			new Ore("coal_ore", Util.requireDefaultState(BlockTypes.COAL_ORE), 10, null, 1, 1),
			new Ore("diamond_ore", Util.requireDefaultState(BlockTypes.DIAMOND_ORE), 30, null, 1, 1),
			new Ore("emerald_ore", Util.requireDefaultState(BlockTypes.EMERALD_ORE), 30, null, 1, 1)
	);
	private final List<MobSpawnEntry> spawnEntries = Lists.newArrayList(
			new MobSpawnEntry("zombie", "minecraft:zombie", 50, 100, 300, 10, 10, 20, 20),
			new MobSpawnEntry("skeleton", "minecraft:skeleton", 70, 100, 300, 10, 15, 25, 20),
			new MobSpawnEntry("creeper", "minecraft:creeper", 100, 100, 300, 20, 15, 25, 20)
	);
	private float naturalPollutionIncrease = 0.1f;
	private int spawnAttemptsPerTick = 10;
	private int sprintingPenalty = 5;

	// cave generation
	private int minLength = 90;
	private int maxLength = 90;
	private int minSize = 7;
	private int maxSize = 11;
	private final List<Room> rooms = Lists.newArrayList(
			new Room.SimpleRoom('w', Lists.newArrayList("forward")),
			new Room.TurnRoom('a', Lists.newArrayList("turn_left"), 15, 30),
			new Room.TurnRoom('d', Lists.newArrayList("turn_right"), -30, -15),
			new Room.BranchRoom('x', Lists.newArrayList("branch"), 90, 90, 1, 1, 20, 39),
			new Room.BranchRoom('y', Lists.newArrayList("small_branch"), 45, 180, 2, 6, 20, 39),
			new Room.DropshaftRoom('o', Lists.newArrayList("dropshaft"), 8, 11, 2, 3),
			new Room.CavernRoom('l', Lists.newArrayList("large_cavern"), 3, 7, 3, Integer.MAX_VALUE, 1, 2, 2),
			new Room.CavernRoom('r', Lists.newArrayList("cavern"), 4, 7, 4, Integer.MAX_VALUE, 0, 1, 3),
			new Room.ShelfRoom('h', Lists.newArrayList("shelf"), 6, 10, 3, 3),
			new Room.RavineRoom('c', Lists.newArrayList("ravine"), 70, 100, 80, 120, 10, 20, 0, 30, 0.2)
	);
	private GrammarGraph grammar;
	{
		Map<Character, GrammarGraph.RuleSet> rules = new HashMap<>();
		rules.put('C', new GrammarGraph.RuleSet(Lists.newArrayList(
				Pair.of(1, "SSS"),
				Pair.of(1, "SSSS"),
				Pair.of(1, "SSSSS"),
				Pair.of(1, "SSSSSS")
		), new ArrayList<>()));
		rules.put('S', new GrammarGraph.RuleSet(Lists.newArrayList(
				Pair.of(1, "XY")
		), new ArrayList<>()));
		rules.put('X', new GrammarGraph.RuleSet(Lists.newArrayList(
				Pair.of(1, "AAAA"),
				Pair.of(1, "AAAAA"),
				Pair.of(1, "AAAAAA"),
				Pair.of(1, "AAAAAAA"),
				Pair.of(1, "AAAAAAAA"),
				Pair.of(1, "AAAAAAAAA"),
				Pair.of(1, "AAAAAAAAAA")
		), new ArrayList<>()));
		rules.put('Y', new GrammarGraph.RuleSet(Lists.newArrayList(
				Pair.of(1, "BBBBB"),
				Pair.of(1, "BBBBBB"),
				Pair.of(1, "BBBBBBB"),
				Pair.of(1, "BBBBBBBB"),
				Pair.of(1, "BBBBBBBBB")
		), new ArrayList<>()));
		rules.put('A', new GrammarGraph.RuleSet(Lists.newArrayList(
				Pair.of(60, "w"),
				Pair.of(15, "a"),
				Pair.of(15, "d"),
				Pair.of(2, "x"),
				Pair.of(2, "o"),
				Pair.of(7, "r"),
				Pair.of(2, "yr")
		), new ArrayList<>()));
		rules.put('B', new GrammarGraph.RuleSet(Lists.newArrayList(
				Pair.of(80, "w"),
				Pair.of(20, "a"),
				Pair.of(20, "d"),
				Pair.of(5, "x"),
				Pair.of(2, "o"),
				Pair.of(2, "c"),
				Pair.of(19, "r"),
				Pair.of(29, "l"),
				Pair.of(8, "h")
		), new ArrayList<>()));
		grammar = new GrammarGraph(rules);
		grammar.validate(rooms.stream().map(Room::getSymbol).collect(Collectors.toSet()));
	}
	private final List<PainterStep> painterSteps = Lists.newArrayList(
			new PainterStep.ReplaceFloor(new ArrayList<>(), Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAVEL)),
			new PainterStep.ChanceReplace(new ArrayList<>(), Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.ANDESITE), 0.2),
			new PainterStep.ChanceReplace(new ArrayList<>(), Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.COBBLESTONE), 0.2),
			new PainterStep.ChanceReplace(new ArrayList<>(), Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.MOSSY_COBBLESTONE), 0.05)
	);
	private final List<Structure> structures = Lists.newArrayList(
			new Structure.VeinStructure("coal_ore", Lists.newArrayList(Structure.Edge.values()), 0.01, null, null, new ArrayList<>(), Util.requireDefaultState(BlockTypes.COAL_ORE), 4),
			new Structure.VeinStructure("diamond_ore", Lists.newArrayList(Structure.Edge.values()), 0.01, null, null, new ArrayList<>(), Util.requireDefaultState(BlockTypes.DIAMOND_ORE), 4),
			new Structure.VeinStructure("emerald_ore", Lists.newArrayList(Structure.Edge.values()), 0.01, null, null, new ArrayList<>(), Util.requireDefaultState(BlockTypes.EMERALD_ORE), 3)
	);
	private final List<Structure> portals = Lists.newArrayList();

	public CaveStyle(String name) {
		this.name = name;
	}

	public void serialize(ConfigurationSection map) {
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
		CaveStyle style = new CaveStyle(name);
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
		ConfigurationSection grammarSection = map.getConfigurationSection("grammar");
		if (grammarSection != null) {
			style.grammar = GrammarGraph.deserialize(grammarSection);
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
		style.grammar.validate(style.rooms.stream().map(Room::getSymbol).collect(Collectors.toSet()));

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
