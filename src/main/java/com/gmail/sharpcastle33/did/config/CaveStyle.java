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
	public static final CaveStyle DEFAULT = new CaveStyle();

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

	public void serialize(ConfigurationSection map) {
		map.set("airBlock", airBlock.getAsString());
		map.set("baseBlock", baseBlock.getAsString());
		map.set("transparentBlocks", transparentBlocks.stream().map(BlockStateHolder::getAsString).collect(Collectors.toCollection(ArrayList::new)));
		ConfigurationSection oresSection = map.createSection("ores");
		for (Ore ore : ores) {
			ore.serialize(oresSection.createSection(ore.getName()));
		}

		map.set("painterSteps", painterSteps.stream().map(PainterStep::serialize).collect(Collectors.toCollection(ArrayList::new)));
		ConfigurationSection structuresSection = map.createSection("structures");
		for (Structure structure : structures) {
			structure.serialize(structuresSection.createSection(structure.getName()));
		}
	}

	public static CaveStyle deserialize(ConfigurationSection map) {
		CaveStyle style = new CaveStyle();
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

	public List<PainterStep> getPainterSteps() {
		return painterSteps;
	}

	public List<Structure> getStructures() {
		return structures;
	}
}
