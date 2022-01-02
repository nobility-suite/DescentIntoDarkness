package com.gmail.sharpcastle33.did.provider;

import com.fastasyncworldedit.core.registry.state.PropertyKey;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.FuzzyBlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public interface BlockProvider {
	BlockStateHolder<?> get(CaveGenContext ctx, Centroid centroid);
	boolean canProduce(BlockStateHolder<?> block);
	List<BlockStateHolder<?>> getCanProduce();

	BlockProvider AIR = new Single(Util.requireDefaultState(BlockTypes.AIR));
	BlockProvider OAK_LOG = new Single(Util.requireDefaultState(BlockTypes.OAK_LOG));
	BlockProvider OAK_LEAVES = new Single(Util.requireDefaultState(BlockTypes.OAK_LEAVES));
	BlockProvider CHORUS_PLANT = new Single(FuzzyBlockState.builder().type(BlockTypes.CHORUS_PLANT).build());
	BlockProvider CHORUS_FLOWER = new Single(Util.requireDefaultState(BlockTypes.CHORUS_FLOWER).with(PropertyKey.AGE, 5));
	BlockProvider GLOWSTONE = new Single(Util.requireDefaultState(BlockTypes.GLOWSTONE));
	BlockProvider NETHER_PORTAL = new Single(Util.requireDefaultState(BlockTypes.NETHER_PORTAL));
	BlockProvider OBSIDIAN = new Single(Util.requireDefaultState(BlockTypes.OBSIDIAN));

	final class Single implements BlockProvider {
		private final BlockStateHolder<?> block;

		public Single(BlockStateHolder<?> block) {
			this.block = block;
		}

		@Override
		public BlockStateHolder<?> get(CaveGenContext ctx, Centroid centroid) {
			return block;
		}

		@Override
		public boolean canProduce(BlockStateHolder<?> block) {
			return this.block.equalsFuzzy(block);
		}

		@Override
		public List<BlockStateHolder<?>> getCanProduce() {
			return List.of(block);
		}
	}

	final class Weighted implements BlockProvider {
		private final BlockStateHolder<?>[] blocks;
		private final int[] weights;

		public Weighted(BlockStateHolder<?>[] blocks, int[] weights) {
			this.blocks = blocks;
			this.weights = weights;
		}

		@Override
		public BlockStateHolder<?> get(CaveGenContext ctx, Centroid centroid) {
			int total = 0;
			for (int weight : weights) {
				total += weight;
			}
			int choice = ctx.rand.nextInt(total);
			for (int i = 0; i < blocks.length; i++) {
				if (choice < weights[i]) {
					return blocks[i];
				}
				choice -= weights[i];
			}
			return blocks[blocks.length - 1];
		}

		@Override
		public boolean canProduce(BlockStateHolder<?> block) {
			for (BlockStateHolder<?> b : blocks) {
				if (b.equalsFuzzy(block)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public List<BlockStateHolder<?>> getCanProduce() {
			return List.of(blocks);
		}
	}

	final class RoomWeighted implements BlockProvider {
		private final BlockProvider[] blocks;
		private final int[] weights;

		public RoomWeighted(BlockProvider[] blocks, int[] weights) {
			this.blocks = blocks;
			this.weights = weights;
		}

		@Override
		public BlockStateHolder<?> get(CaveGenContext ctx, Centroid centroid) {
			int total = 0;
			for (int weight : weights) {
				total += weight;
			}
			long seed = ctx.caveSeed + 133742069L * centroid.roomIndex;
			int choice = new Random(seed).nextInt(total);
			for (int i = 0; i < blocks.length; i++) {
				if (choice < weights[i]) {
					return blocks[i].get(ctx, centroid);
				}
				choice -= weights[i];
			}
			return blocks[blocks.length - 1].get(ctx, centroid);
		}

		@Override
		public boolean canProduce(BlockStateHolder<?> block) {
			for (BlockProvider b : blocks) {
				if (b.canProduce(block)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public List<BlockStateHolder<?>> getCanProduce() {
			List<BlockStateHolder<?>> list = new ArrayList<>();
			for (BlockProvider b : blocks) {
				list.addAll(b.getCanProduce());
			}
			return list;
		}
	}
}
