package com.gmail.sharpcastle33.did;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.ListTagBuilder;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

}
