package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

public class CaveGenContext implements AutoCloseable {
	private final EditSession session;
	public final CaveStyle style;
	public final Random rand;
	private boolean debug;
	private final PackedBlockStorage blockStorage;
	private final Set<BlockVector2> accessedChunks = new HashSet<>();
	private final Deque<Transform> blockTransformStack = new LinkedList<>(Collections.singletonList(new Identity()));
	private final Deque<Transform> inverseBlockTransformStack = new LinkedList<>(Collections.singletonList(new Identity()));
	private final Deque<Transform> locationTransformStack = new LinkedList<>(Collections.singletonList(new Identity()));
	private final Deque<Transform> inverseLocationTransformStack = new LinkedList<>(Collections.singletonList(new Identity()));
	private Region limit = null;
	private boolean canceled = false;

	private CaveGenContext(EditSession session, CaveStyle style, Random rand) {
		this.session = session;
		this.style = style;
		this.rand = rand;
		this.blockStorage = new PackedBlockStorage(style.getBaseBlock().toImmutableState());
	}

	public CaveGenContext setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	public boolean isDebug() {
		return debug;
	}

	public CaveGenContext limit(Region limit) {
		this.limit = limit;
		return this;
	}

	public static CaveGenContext create(World world, CaveStyle style, Random rand) {
		EditSession session = WorldEdit.getInstance().newEditSession(world);
		return new CaveGenContext(session, style, rand);
	}

	private void ensureChunkGenerated(BlockVector3 blockPos) {
		BlockVector2 chunkPos = BlockVector2.at(blockPos.getX() >> 4, blockPos.getZ() >> 4);
		if (accessedChunks.add(chunkPos)) {
			fillChunk(chunkPos);
		}
	}

	private void fillChunk(BlockVector2 chunkPos) {
		BlockVector3 from = BlockVector3.at(chunkPos.getX() * 16, 1, chunkPos.getZ() * 16);
		BlockVector3 to = from.add(15, 253, 15);
		fill(new CuboidRegion(from, to), style.getBaseBlock());
	}

	public boolean setBlock(BlockVector3 pos, BlockStateHolder<?> block) throws MaxChangedBlocksException {
		pos = getInverseLocationTransform().apply(pos.toVector3()).toBlockPoint();
		block = Util.transformBlock(block, getInverseBlockTransform());
		if (pos.getBlockY() <= 0 || pos.getBlockY() >= 255) {
			return false;
		}
		if (limit != null && !limit.contains(pos)) {
			return false;
		}
		ensureChunkGenerated(pos);
		if (session.setBlock(pos, block)) {
			blockStorage.setBlock(pos, block.toImmutableState());
			return true;
		} else {
			return false;
		}
	}

	public BlockState getBlock(BlockVector3 pos) {
		pos = getInverseLocationTransform().apply(pos.toVector3()).toBlockPoint();
		if (pos.getBlockY() < 0 || pos.getBlockY() > 255) {
			return Util.requireDefaultState(BlockTypes.AIR);
		}
		if (pos.getBlockY() == 0 || pos.getBlockY() == 255) {
			return Util.requireDefaultState(BlockTypes.BEDROCK);
		}
		if (limit != null && !limit.contains(pos)) {
			return style.getBaseBlock().toImmutableState();
		}
		ensureChunkGenerated(pos);
		BlockState block = blockStorage.getBlock(pos);
		block = (BlockState) Util.transformBlock(block, getBlockTransform());
		return block;
	}

	public void pushTransform(Transform blockTransform, Transform locationTransform) {
		blockTransform = Util.toDirectionTransform(blockTransform);
		blockTransformStack.push(getBlockTransform().combine(blockTransform));
		inverseBlockTransformStack.push(blockTransform.inverse().combine(getInverseBlockTransform()));
		locationTransformStack.push(getLocationTransform().combine(locationTransform));
		inverseLocationTransformStack.push(locationTransform.inverse().combine(getInverseLocationTransform()));
	}

	public void popTransform() {
		blockTransformStack.pop();
		inverseBlockTransformStack.pop();
		locationTransformStack.pop();
		inverseLocationTransformStack.pop();
	}

	/**
	 * Gets the current world space -> local space block transform
	 */
	public Transform getBlockTransform() {
		Transform transform = blockTransformStack.peek();
		assert transform != null;
		return transform;
	}

	/**
	 * Gets the current local space -> world space block transform
	 */
	public Transform getInverseBlockTransform() {
		Transform transform = inverseBlockTransformStack.peek();
		assert transform != null;
		return transform;
	}

	/**
	 * Gets the current world space -> local space location transform
	 */
	public Transform getLocationTransform() {
		Transform transform = locationTransformStack.peek();
		assert transform != null;
		return transform;
	}

	/**
	 * Gets the current local space -> world space location transform
	 */
	public Transform getInverseLocationTransform() {
		Transform transform = inverseLocationTransformStack.peek();
		assert transform != null;
		return transform;
	}

	public Extent asExtent() {
		return new AbstractDelegateExtent(session) {
			@Override
			public BlockState getBlock(BlockVector3 position) {
				return CaveGenContext.this.getBlock(position);
			}

			@Override
			public BlockState getBlock(int x, int y, int z) {
				return CaveGenContext.this.getBlock(BlockVector3.at(x, y, z));
			}

			@Override
			public BaseBlock getFullBlock(BlockVector3 position) {
				return CaveGenContext.this.getBlock(position).toBaseBlock();
			}

			@Override
			public BaseBlock getFullBlock(int x, int y, int z) {
				return CaveGenContext.this.getBlock(BlockVector3.at(x, y, z)).toBaseBlock();
			}

			@Override
			public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
				return CaveGenContext.this.setBlock(location, block);
			}

			@Override
			public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
				return CaveGenContext.this.setBlock(BlockVector3.at(x, y, z), block);
			}
		};
	}

	public boolean cancel() {
		canceled = true;
		session.cancel();
		return true;
	}

	@Override
	public void close() {
		if (canceled) {
			session.close();
			return;
		}

		// fill chunks neighboring accessed chunks
		Bukkit.getLogger().log(Level.INFO, "Filling neighbor chunks...");
		Set<BlockVector2> filledChunks = new HashSet<>(accessedChunks);
		for (BlockVector2 accessedChunk : accessedChunks) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockVector2 neighbor = accessedChunk.add(dx, dz);
					if (filledChunks.add(neighbor)) {
						fillChunk(neighbor);
					}
				}
			}
		}

		// bedrock wall around all generated chunks
		Bukkit.getLogger().log(Level.INFO, "Creating bedrock walls...");
		for (BlockVector2 filledChunk : filledChunks) {
			BlockVector2 north = filledChunk.add(0, -1);
			if (!filledChunks.contains(north)) {
				fill(new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16, 1, filledChunk.getZ() * 16 - 1),
						BlockVector3.at(filledChunk.getX() * 16 + 15, 254, filledChunk.getZ() * 16 - 1)
				), Util.requireDefaultState(BlockTypes.BEDROCK));
			}
			BlockVector2 east = filledChunk.add(1, 0);
			if (!filledChunks.contains(east)) {
				fill(new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16 + 16, 1, filledChunk.getZ() * 16),
						BlockVector3.at(filledChunk.getX() * 16 + 16, 254, filledChunk.getZ() * 16 + 15)
				), Util.requireDefaultState(BlockTypes.BEDROCK));
			}
			BlockVector2 south = filledChunk.add(0, 1);
			if (!filledChunks.contains(south)) {
				fill(new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16, 1, filledChunk.getZ() * 16 + 16),
						BlockVector3.at(filledChunk.getX() * 16 + 15, 254, filledChunk.getZ() * 16 + 16)
				), Util.requireDefaultState(BlockTypes.BEDROCK));
			}
			BlockVector2 west = filledChunk.add(-1, 0);
			if (!filledChunks.contains(west)) {
				fill(new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16 - 1, 1, filledChunk.getZ() * 16),
						BlockVector3.at(filledChunk.getX() * 16 - 1, 254, filledChunk.getZ() * 16 + 15)
				), Util.requireDefaultState(BlockTypes.BEDROCK));
			}
		}

		Bukkit.getLogger().log(Level.INFO, "Cave finished generating");

		session.close();
	}

	private void fill(Region region, BlockStateHolder<?> block) {
		// for some reason, setBlocks is too slow here, so we use a loop
		for (BlockVector3 pos : region) {
			session.setBlock(pos, block);
		}
	}
}
