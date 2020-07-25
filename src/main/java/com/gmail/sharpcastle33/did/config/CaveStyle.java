package com.gmail.sharpcastle33.did.config;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.generator.PainterStep;
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

	private BlockStateHolder<?> airBlock = Util.requireDefaultState(BlockTypes.AIR);
	private BlockStateHolder<?> baseBlock = Util.requireDefaultState(BlockTypes.STONE);
	private List<BlockStateHolder<?>> transparentBlocks = Lists.newArrayList(
			Util.requireDefaultState(BlockTypes.AIR),
			Util.requireDefaultState(BlockTypes.GLOWSTONE),
			FuzzyBlockState.builder().type(Objects.requireNonNull(BlockTypes.WATER)).build(),
			FuzzyBlockState.builder().type(Objects.requireNonNull(BlockTypes.LAVA)).build()
	);
	private List<PainterStep> painterSteps = Lists.newArrayList(
			new PainterStep.ReplaceFloor(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAVEL)),
			new PainterStep.ChanceReplace(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.ANDESITE), 0.2),
			new PainterStep.ChanceReplace(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.COBBLESTONE), 0.2),
			new PainterStep.ChanceReplace(Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.MOSSY_COBBLESTONE), 0.05)
	);

	public void serialize(ConfigurationSection map) {
		map.set("airBlock", airBlock.getAsString());
		map.set("baseBlock", baseBlock.getAsString());
		map.set("transparentBlocks", transparentBlocks.stream().map(BlockStateHolder::getAsString).collect(Collectors.toCollection(ArrayList::new)));
		map.set("painterSteps", painterSteps.stream().map(PainterStep::serialize).collect(Collectors.toCollection(ArrayList::new)));
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
		List<?> painterSteps = map.getList("painterSteps");
		if (painterSteps != null) {
			style.painterSteps.clear();
			painterSteps.stream().map(PainterStep::deserialize).forEachOrdered(style.painterSteps::add);
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

	public List<PainterStep> getPainterSteps() {
		return painterSteps;
	}
}
