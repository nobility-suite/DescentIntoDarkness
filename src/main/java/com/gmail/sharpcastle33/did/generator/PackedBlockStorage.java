package com.gmail.sharpcastle33.did.generator;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackedBlockStorage {
	private final Map<BlockState, Integer> palette = new HashMap<>();
	private final List<BlockState> inversePalette = new ArrayList<>();
	private int bitsPerBlock = 4;
	private int maxNumBlocks = 16;
	private int blocksPerWord = 16;
	private final BlockState defaultBlock;
	private final Map<BlockVector3, long[]> subchunks = new HashMap<>();

	public PackedBlockStorage(BlockState defaultBlock) {
		this.defaultBlock = defaultBlock;
	}

	private static BlockVector3 subchunkPos(BlockVector3 pos) {
		return BlockVector3.at(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
	}

	public BlockState getBlock(BlockVector3 pos) {
		long[] packedArray = subchunks.get(subchunkPos(pos));
		if (packedArray == null) {
			return defaultBlock;
		}

		int index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
		int indexInWord = index % blocksPerWord;
		long word = packedArray[index / blocksPerWord];
		int id = (int) (word >>> (indexInWord * bitsPerBlock)) & (maxNumBlocks - 1);
		if (id <= 0 || id > inversePalette.size()) {
			return defaultBlock;
		}
		return inversePalette.get(id - 1);
	}

	public void setBlock(BlockVector3 pos, BlockState block) {
		int prevSize = palette.size();
		int id = block.equals(defaultBlock) ? 0 : palette.computeIfAbsent(block, k -> prevSize + 1);
		if (palette.size() != prevSize) {
			inversePalette.add(block);
			if ((prevSize & (prevSize + 1)) == 0) {
				expandBitsPerBlock();
			}
		}

		long[] packedArray = subchunks.computeIfAbsent(subchunkPos(pos), k -> new long[(4096 + blocksPerWord - 1) / blocksPerWord]);
		int index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
		int indexInWord = index % blocksPerWord;
		int wordIndex = index / blocksPerWord;
		long word = packedArray[wordIndex];
		word &= ~((long) (maxNumBlocks - 1) << (indexInWord * bitsPerBlock));
		word |= (long) id << (indexInWord * bitsPerBlock);
		packedArray[wordIndex] = word;
	}

	private void expandBitsPerBlock() {
		int prevBitsPerBlock = bitsPerBlock;
		int prevMaxNumBlocks = maxNumBlocks;
		int prevBlocksPerWord = blocksPerWord;
		bitsPerBlock = prevBitsPerBlock + 1;
		maxNumBlocks = prevMaxNumBlocks << 1;
		blocksPerWord = 64 / bitsPerBlock;

		subchunks.replaceAll((pos, oldArray) -> {
			long[] newArray = new long[(4096 + blocksPerWord - 1) / blocksPerWord];
			for (int index = 0; index < 4096; index++) {
				int indexInOldWord = index % prevBlocksPerWord;
				int indexInNewWord = index % blocksPerWord;
				long oldWord = oldArray[index / prevBlocksPerWord];
				long id = (oldWord >>> (indexInOldWord * prevBitsPerBlock)) & (prevMaxNumBlocks - 1);
				newArray[index / blocksPerWord] |= id << (indexInNewWord * bitsPerBlock);
			}
			return newArray;
		});
	}
}
