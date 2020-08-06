package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.generator.PainterStep;
import com.gmail.sharpcastle33.did.generator.Structure;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
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
	private final List<PainterStep> painterSteps = Lists.newArrayList(
			new PainterStep.ReplaceFloor(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAVEL)),
			new PainterStep.ChanceReplace(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.ANDESITE), 0.2),
			new PainterStep.ChanceReplace(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.COBBLESTONE), 0.2),
			new PainterStep.ChanceReplace(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.MOSSY_COBBLESTONE), 0.05)
	);
	private final List<Structure> structures = Lists.newArrayList(
			new Structure.VeinStructure("coal_ore", Lists.newArrayList(Structure.Edge.values()), 0.01, null, null, Util.requireDefaultState(BlockTypes.COAL_ORE), 4),
			new Structure.VeinStructure("diamond_ore", Lists.newArrayList(Structure.Edge.values()), 0.01, null, null, Util.requireDefaultState(BlockTypes.DIAMOND_ORE), 4),
			new Structure.VeinStructure("emerald_ore", Lists.newArrayList(Structure.Edge.values()), 0.01, null, null, Util.requireDefaultState(BlockTypes.EMERALD_ORE), 3)
	);

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

		map.set("painterSteps", painterSteps.stream().map(PainterStep::serialize).collect(Collectors.toCollection(ArrayList::new)));
		ConfigurationSection structuresSection = map.createSection("structures");
		for (Structure structure : structures) {
			structure.serialize(structuresSection.createSection(structure.getName()));
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

	public List<PainterStep> getPainterSteps() {
		return painterSteps;
	}

	public List<Structure> getStructures() {
		return structures;
	}
}
