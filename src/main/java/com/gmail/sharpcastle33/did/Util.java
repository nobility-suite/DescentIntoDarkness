package com.gmail.sharpcastle33.did;

import com.gmail.sharpcastle33.did.generator.Centroid;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.ListTagBuilder;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.registry.state.PropertyKey;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Util {

	public static BlockState requireDefaultState(BlockType block) {
		return Objects.requireNonNull(block).getDefaultState();
	}

	public static Vector3 rotateAroundY(Vector3 vector, double radians) {
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return Vector3.at(cos * vector.getX() + sin * vector.getZ(), vector.getY(), cos * vector.getZ() - sin * vector.getX());
	}

	public static BlockVector3 applyDirection(Transform transform, BlockVector3 vector) {
		Vector3 m3 = transform.apply(Vector3.ZERO);
		Vector3 m0 = transform.apply(Vector3.UNIT_X).subtract(m3);
		Vector3 m1 = transform.apply(Vector3.UNIT_Y).subtract(m3);
		Vector3 m2 = transform.apply(Vector3.UNIT_Z).subtract(m3);

		return BlockVector3.at(
				m0.getX() * vector.getX() + m1.getX() * vector.getY() + m2.getX() * vector.getZ(),
				m0.getY() * vector.getX() + m1.getY() * vector.getY() + m2.getY() * vector.getZ(),
				m0.getZ() * vector.getX() + m1.getZ() * vector.getY() + m2.getZ() * vector.getZ()
		);
	}

	public static Direction getOpposite(Direction dir) {
		switch (dir) {
			case NORTH: return Direction.SOUTH;
			case EAST: return Direction.WEST;
			case SOUTH: return Direction.NORTH;
			case WEST: return Direction.EAST;
			case UP: return Direction.DOWN;
			case DOWN: return Direction.UP;
			case NORTHEAST: return Direction.SOUTHWEST;
			case NORTHWEST: return Direction.SOUTHEAST;
			case SOUTHEAST: return Direction.NORTHWEST;
			case SOUTHWEST: return Direction.NORTHEAST;
			case WEST_NORTHWEST: return Direction.EAST_SOUTHEAST;
			case WEST_SOUTHWEST: return Direction.EAST_NORTHEAST;
			case NORTH_NORTHWEST: return Direction.SOUTH_SOUTHEAST;
			case NORTH_NORTHEAST: return Direction.SOUTH_SOUTHWEST;
			case EAST_NORTHEAST: return Direction.WEST_SOUTHWEST;
			case EAST_SOUTHEAST: return Direction.WEST_NORTHWEST;
			case SOUTH_SOUTHEAST: return Direction.NORTH_NORTHWEST;
			case SOUTH_SOUTHWEST: return Direction.NORTH_NORTHEAST;
			default: throw new AssertionError("Stop adding new directions, sk89q!");
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends BlockStateHolder<T>> BlockStateHolder<?> transformBlock(BlockStateHolder<?> block, Transform transform) {
		if (transform.isIdentity()) {
			return block;
		}

		BlockType type = block.getBlockType();
		if (type.hasProperty(PropertyKey.NORTH)
				&& type.hasProperty(PropertyKey.SOUTH)
				&& type.hasProperty(PropertyKey.WEST)
				&& type.hasProperty(PropertyKey.EAST)
				&& type.hasProperty(PropertyKey.DOWN)
				&& type.hasProperty(PropertyKey.UP)) {
			Direction newNorth = Direction.findClosest(applyDirection(transform, Direction.NORTH.toBlockVector()).toVector3(), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert newNorth != null;
			Direction newSouth = Direction.findClosest(applyDirection(transform, Direction.SOUTH.toBlockVector()).toVector3(), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert newSouth != null;
			Direction newWest = Direction.findClosest(applyDirection(transform, Direction.WEST.toBlockVector()).toVector3(), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert newWest != null;
			Direction newEast = Direction.findClosest(applyDirection(transform, Direction.EAST.toBlockVector()).toVector3(), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert newEast != null;
			Direction newDown = Direction.findClosest(applyDirection(transform, Direction.DOWN.toBlockVector()).toVector3(), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert newDown != null;
			Direction newUp = Direction.findClosest(applyDirection(transform, Direction.UP.toBlockVector()).toVector3(), Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT);
			assert newUp != null;

			Object northState = block.getState(PropertyKey.NORTH);
			Object southState = block.getState(PropertyKey.SOUTH);
			Object westState = block.getState(PropertyKey.WEST);
			Object eastState = block.getState(PropertyKey.EAST);
			Object downState = block.getState(PropertyKey.DOWN);
			Object upState = block.getState(PropertyKey.UP);

			block = block.with(PropertyKey.valueOf(newNorth.name().toUpperCase(Locale.ROOT)), northState);
			block = block.with(PropertyKey.valueOf(newSouth.name().toUpperCase(Locale.ROOT)), southState);
			block = block.with(PropertyKey.valueOf(newWest.name().toUpperCase(Locale.ROOT)), westState);
			block = block.with(PropertyKey.valueOf(newEast.name().toUpperCase(Locale.ROOT)), eastState);
			block = block.with(PropertyKey.valueOf(newDown.name().toUpperCase(Locale.ROOT)), downState);
			block = block.with(PropertyKey.valueOf(newUp.name().toUpperCase(Locale.ROOT)), upState);

			return block;
		}
		return BlockTransformExtent.transform((T) block, transform);
	}

	public static <T> CompletableFuture<T> completeExceptionally(Throwable t) {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(t);
		return future;
	}

	/**
	 * Teleports a player the normal way if they are online, and by editing the NBT files if they are offline.
	 * Only works if the player has been online before.
	 */
	public static boolean teleportOfflinePlayer(OfflinePlayer player, Location destination) {
		if (player.isOnline()) {
			assert player.getPlayer() != null;
			return player.getPlayer().teleport(destination);
		}

		File worldFolder = DescentIntoDarkness.multiverseCore.getMVWorldManager().getFirstSpawnWorld().getCBWorld().getWorldFolder();
		File playerdataFolder = new File(worldFolder, "playerdata");
		File playerFile = new File(playerdataFolder, player.getUniqueId() + ".dat");
		if (!playerFile.exists()) {
			return false;
		}

		NamedTag playerData;
		try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new FileInputStream(playerFile)))) {
			playerData = in.readNamedTag();
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not teleport offline player", e);
			return false;
		}
		if (!(playerData.getTag() instanceof CompoundTag)) {
			Bukkit.getLogger().log(Level.SEVERE, "Corrupted player data");
			return false;
		}
		CompoundTag playerTag = (CompoundTag) playerData.getTag();

		World destWorld = destination.getWorld();
		if (destWorld != null) {
			World.Environment environment = destWorld.getEnvironment();
			int id;
			switch (environment) {
				case NETHER: id = -1; break;
				case THE_END: id = 1; break;
				default: id = 0; break;
			}
			UUID uuid = destWorld.getUID();
			playerTag = playerTag.createBuilder()
					.putInt("Dimension", id)
					.putLong("WorldUUIDLeast", uuid.getLeastSignificantBits())
					.putLong("WorldUUIDMost", uuid.getMostSignificantBits())
					.build();
		}

		playerTag = playerTag.createBuilder().put("Pos", ListTagBuilder.createWith(
				new DoubleTag(destination.getX()),
				new DoubleTag(destination.getY()),
				new DoubleTag(destination.getZ())).build()).build();

		try (NBTOutputStream out = new NBTOutputStream(new GZIPOutputStream(new FileOutputStream(playerFile)))) {
			out.writeNamedTag("", playerTag);
			out.flush();
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not teleport offline player", e);
			return false;
		}

		return true;
	}

	// because Bukkit is stupid and doesn't allow you to remove a single score from a player
	public static void resetScore(Score score) {
		Scoreboard scoreboard = score.getScoreboard();
		if (scoreboard == null) {
			return;
		}
		Set<Score> otherScores = new HashSet<>(scoreboard.getScores(score.getEntry()));
		otherScores.remove(score);
		scoreboard.resetScores(score.getEntry());
		for (Score otherScore : otherScores) {
			otherScore.getObjective().getScore(otherScore.getEntry()).setScore(otherScore.getScore());
		}
	}

	public static void ensureConnected(List<Centroid> centroidsInOut, int connectingCentroidRadius, Function<Vector3, Centroid> centroidSupplier) {
		// find the minimum spanning tree of the centroids using Kruskal's algorithm, to ensure they are connected
		List<Pair<Centroid, Centroid>> edges = new ArrayList<>();
		for (int i = 0; i < centroidsInOut.size() - 1; i++) {
			for (int j = i + 1; j < centroidsInOut.size(); j++) {
				edges.add(Pair.of(centroidsInOut.get(i), centroidsInOut.get(j)));
			}
		}
		edges.sort(Comparator.comparingDouble(edge -> edge.getLeft().pos.distance(edge.getRight().pos) - edge.getLeft().size - edge.getRight().size));
		Map<Centroid, Integer> nodeGroups = new HashMap<>();
		int groupCount = 0;
		ListIterator<Pair<Centroid, Centroid>> edgesItr = edges.listIterator();
		while (edgesItr.hasNext()) {
			Pair<Centroid, Centroid> edge = edgesItr.next();
			Integer groupA = nodeGroups.get(edge.getLeft());
			Integer groupB = nodeGroups.get(edge.getRight());
			if (groupA != null && groupA.equals(groupB)) {
				edgesItr.remove();
			} else {
				if (groupA != null && groupB != null) {
					nodeGroups.replaceAll((key, val) -> val.equals(groupB) ? groupA : val);
				} else {
					Integer group = groupA == null ? groupB == null ? groupCount++ : groupB : groupA;
					nodeGroups.put(edge.getLeft(), group);
					nodeGroups.put(edge.getRight(), group);
				}
			}
		}

		for (Pair<Centroid, Centroid> edge : edges) {
			double distance = edge.getLeft().pos.distance(edge.getRight().pos);
			double actualDistance = distance - edge.getLeft().size - edge.getRight().size;
			if (actualDistance < 0) {
				continue;
			}
			actualDistance = Math.max(1, actualDistance);
			Vector3 dir = edge.getRight().pos.subtract(edge.getLeft().pos).divide(distance);

			int numSegments = (int) Math.ceil(actualDistance / connectingCentroidRadius) + 1;
			double segmentLength = actualDistance / numSegments;

			Vector3 startPos = edge.getLeft().pos.add(dir.multiply(edge.getLeft().size));
			Vector3 segment = dir.multiply(segmentLength);
			for (int i = 1; i < numSegments; i++) {
				centroidsInOut.add(centroidSupplier.apply(startPos.add(segment.multiply(i))));
			}
		}
	}

}
