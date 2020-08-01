package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CaveTrackerManager {

	private final ArrayList<CaveTracker> caveTrackers = new ArrayList<>();
	private final Map<UUID, Location> overworldPlayerLocations = new HashMap<>();
	private int nextInstanceId;

	public CompletableFuture<CaveTracker> createCave(CaveStyle style) {
		int id = nextInstanceId++;
		String name = getWorldName(id);
		World world = createFlatWorld(id, style, World.Environment.THE_END);
		if (world == null) {
			return Util.completeExceptionally(new RuntimeException("Could not create world"));
		}
		return DescentIntoDarkness.plugin.supplyAsync(() -> {
			Random rand = new Random();
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(world), style, rand)) {
				CaveGenerator.generateCave(ctx, Vector3.at(6969, 210, 6969), rand.nextInt(5) + 7);
			} catch (WorldEditException e) {
				DescentIntoDarkness.plugin.runSyncLater(() -> DescentIntoDarkness.multiverseCore.getMVWorldManager().deleteWorld(name));
				throw new RuntimeException("Could not generate cave", e);
			}
			Location spawnPoint = new Location(world, 6969, 210, 6969);
			while (style.isTransparentBlock(BukkitAdapter.adapt(spawnPoint.getBlock().getBlockData()))) {
				spawnPoint.add(0, -1, 0);
			}
			spawnPoint.add(0, 1, 0);
			CaveTracker caveTracker = new CaveTracker(id, world, spawnPoint);
			DescentIntoDarkness.plugin.runSyncNow(() -> caveTrackers.add(caveTracker));
			return caveTracker;
		});
	}

	public void deleteCave(CaveTracker caveTracker) {
		List<UUID> members = caveTracker.getMembers();
		for (int i = members.size() - 1; i >= 0; i--) {
			UUID player = members.get(i);

			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
			//noinspection ConstantConditions - dumb bukkit
			if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
				continue;
			}

			Location spawnLocation = overworldPlayerLocations.remove(player);
			if (spawnLocation == null) {
				spawnLocation = offlinePlayer.getBedSpawnLocation();
				if (spawnLocation == null) {
					spawnLocation = DescentIntoDarkness.multiverseCore.getMVWorldManager().getSpawnWorld().getSpawnLocation();
				}
			}
			if (spawnLocation != null) {
				Util.teleportOfflinePlayer(offlinePlayer, spawnLocation);
			}

			caveTracker.removeMember(player);
		}

		caveTrackers.remove(caveTracker);
		DescentIntoDarkness.multiverseCore.getMVWorldManager().deleteWorld(getWorldName(caveTracker.getId()));
	}

	public void destroy() {
		Bukkit.getLogger().log(Level.INFO, "Deleting " + caveTrackers.size() + " cave instances");
		while (!caveTrackers.isEmpty()) {
			deleteCave(caveTrackers.get(0));
		}
	}

	public boolean isInCave(Player p) {
		return getCave(p) != null;
	}

	public boolean teleportPlayerTo(Player p, @Nullable CaveTracker newCave) {
		CaveTracker existingCave = getCave(p);
		if (existingCave == newCave) {
			return true;
		}

		if (existingCave == null) {
			overworldPlayerLocations.put(p.getUniqueId(), p.getLocation());
		} else {
			existingCave.removeMember(p.getUniqueId());
		}

		if (newCave == null) {
			Location newLocation = overworldPlayerLocations.remove(p.getUniqueId());
			if (newLocation == null) {
				newLocation = p.getBedSpawnLocation();
				if (newLocation == null) {
					newLocation = DescentIntoDarkness.multiverseCore.getMVWorldManager().getSpawnWorld().getSpawnLocation();
				}
			}
			if (newLocation != null) {
				p.teleport(newLocation);
			}
		} else {
			Location start = newCave.getStart();
			String destStr = String.format("e:%s:%f,%f,%f", getWorldName(newCave.getId()), start.getX(), start.getY(), start.getZ());
			MVDestination dest = DescentIntoDarkness.multiverseCore.getDestFactory().getDestination(destStr);
			if (DescentIntoDarkness.multiverseCore.getSafeTTeleporter().teleport(Bukkit.getConsoleSender(), p, dest) != TeleportResult.SUCCESS) {
				return false;
			}
			newCave.addMember(p.getUniqueId());
		}

		return true;
	}

	@Nullable
	public CaveTracker getCave(Player p) {
		for(CaveTracker i : caveTrackers) {
			List<UUID> members = i.getMembers();
			if(members.contains(p.getUniqueId())) {
				return i;
			}
		}
		return null;
	}

	private String getWorldName(int id) {
		return "did_cave_" + id;
	}

	private World createFlatWorld(int id, CaveStyle style, World.Environment environment) {
		MVWorldManager worldManager = DescentIntoDarkness.multiverseCore.getMVWorldManager();
		String worldName = getWorldName(id);
		String generator = "DescentIntoDarkness:full_" + style.getBaseBlock().getAsString();
		if (!worldManager.addWorld(worldName, environment, "0", WorldType.FLAT, Boolean.FALSE, generator, false)) {
			return null;
		}
		World world = worldManager.getMVWorld(worldName).getCBWorld();
		world.setGameRule(GameRule.DO_MOB_SPAWNING, false);

		// this is not what players are coming here to do...
		world.getEntitiesByClass(EnderDragon.class).forEach(Entity::remove);

		return world;
	}

}
