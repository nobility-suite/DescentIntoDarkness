package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.Nullable;

public class CaveTrackerManager {

	private boolean hasInitialized = false;
	private final int instanceLimit;
	private final ArrayList<CaveTracker> caveTrackers = new ArrayList<>();
	private final Map<UUID, Location> overworldPlayerLocations = new HashMap<>();
	private int nextInstanceId;
	private Objective pollutionObjective;
	private final AtomicBoolean generatingCave = new AtomicBoolean(false);

	public CaveTrackerManager(int instanceLimit) {
		this.instanceLimit = instanceLimit;
	}

	public void initialize() {
		Bukkit.getLogger().log(Level.INFO, "Creating " + instanceLimit + " cave worlds...");
		for (int i = 0; i < instanceLimit; i++) {
			if (!DescentIntoDarkness.multiverseCore.getMVWorldManager().isMVWorld(getWorldName(i))) {
				Bukkit.getLogger().log(Level.INFO, "Cave world " + (i + 1) + " / " + instanceLimit);
				try {
					getOrCreateFlatWorld(i, Util.requireDefaultState(BlockTypes.STONE), World.Environment.THE_END).get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void update() {
		if (!hasInitialized) {
			initialize();
			hasInitialized = true;
		}

		if (!generatingCave.compareAndSet(false, true)) {
			return;
		}
		for (CaveTracker cave : caveTrackers) {
			if (cave.hasBeenJoined()) {
				long aliveTime = cave.getWorld().getFullTime() - cave.getJoinTime();
				if (aliveTime > DescentIntoDarkness.plugin.getCaveTimeLimit()) {
					deleteCave(cave);
					break;
				}
			}
		}
		if (caveTrackers.size() >= instanceLimit) {
			generatingCave.set(false);
			return;
		}
		CaveStyle style = getRandomStyle();
		if (style == null) {
			generatingCave.set(false);
			return;
		}
		createCave(style).whenComplete((cave, throwable) -> {
			if (throwable != null) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed to create cave", throwable);
			}
			generatingCave.set(false);
		});
	}

	private CaveStyle getRandomStyle() {
		if (DescentIntoDarkness.plugin.getCaveStyleWeights().isEmpty()) {
			return null;
		}

		int totalWeight = DescentIntoDarkness.plugin.getCaveStyleWeights().values().stream().mapToInt(Integer::intValue).sum();
		int randVal = new Random().nextInt(totalWeight);
		String chosenStyle = null;
		for (Map.Entry<String, Integer> entry : DescentIntoDarkness.plugin.getCaveStyleWeights().entrySet()) {
			randVal -= entry.getValue();
			if (randVal < 0) {
				chosenStyle = entry.getKey();
			}
		}

		CaveStyle caveStyle = DescentIntoDarkness.plugin.getCaveStyles().get(chosenStyle);
		if (caveStyle == null) {
			Bukkit.getLogger().log(Level.SEVERE, "No such cave style: " + chosenStyle);
			DescentIntoDarkness.plugin.getCaveStyleWeights().remove(chosenStyle);
			return getRandomStyle();
		}
		return caveStyle;
	}

	public CompletableFuture<CaveTracker> findFreeCave() {
		int caveId = nextInstanceId;
		do {
			CaveTracker cave = getCaveById(caveId);
			if (cave != null && !cave.hasBeenJoined()) {
				return CompletableFuture.completedFuture(cave);
			}
			caveId = (caveId + 1) % instanceLimit;
		} while (caveId != nextInstanceId);

		CaveStyle randomStyle = getRandomStyle();
		if (randomStyle == null) {
			return Util.completeExceptionally(new RuntimeException("No cave styles to choose from"));
		}
		return createCave(randomStyle);
	}

	public CompletableFuture<CaveTracker> createCave(CaveStyle style) {
		int oldInstanceId = nextInstanceId;
		do {
			nextInstanceId = (nextInstanceId + 1) % instanceLimit;
			if (nextInstanceId == oldInstanceId) {
				return Util.completeExceptionally(new RuntimeException("Could not create cave instances: no free caves left"));
			}
		} while (getCaveById(nextInstanceId) != null);

		int id = nextInstanceId;
		String name = getWorldName(id);
		return getOrCreateFlatWorld(id, style.getBaseBlock(), World.Environment.THE_END)
			.thenApply(world -> {
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
				return DescentIntoDarkness.plugin.supplySyncNow(() -> {
					CaveTracker caveTracker = new CaveTracker(id, world, spawnPoint, style);
					caveTrackers.add(caveTracker);
					return caveTracker;
				});
		});
	}

	public void deleteCave(CaveTracker caveTracker) {
		Bukkit.getLogger().log(Level.INFO, "Deleting cave " + caveTracker.getId());

		List<UUID> members = caveTracker.getPlayers();
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

			caveTracker.removePlayer(player);
		}

		caveTracker.getTeam().unregister();
		caveTrackers.remove(caveTracker);
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
			existingCave.removePlayer(p.getUniqueId());
			if (existingCave.getPlayers().isEmpty()) {
				deleteCave(existingCave);
			}
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
			newCave.addPlayer(p.getUniqueId());
		}

		return true;
	}

	@Nullable
	public CaveTracker getCaveById(int id) {
		for (CaveTracker cave : caveTrackers) {
			if (cave.getId() == id) {
				return cave;
			}
		}
		return null;
	}

	@Nullable
	public CaveTracker getCave(Player p) {
		for(CaveTracker cave : caveTrackers) {
			List<UUID> members = cave.getPlayers();
			if(members.contains(p.getUniqueId())) {
				return cave;
			}
		}
		return null;
	}

	public List<CaveTracker> getCaves() {
		return Collections.unmodifiableList(caveTrackers);
	}

	public Objective getPollutionObjective() {
		if (pollutionObjective == null) {
			pollutionObjective = DescentIntoDarkness.plugin.getScoreboard().getObjective("pollution");
			if (pollutionObjective == null) {
				pollutionObjective = DescentIntoDarkness.plugin.getScoreboard().registerNewObjective("pollution", "dummy", "Pollution");
			}
			// Temporary, for debug
			pollutionObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
		}
		return pollutionObjective;
	}

	private String getWorldName(int id) {
		return "did_cave_" + id;
	}

	private CompletableFuture<World> getOrCreateFlatWorld(int id, BlockStateHolder<?> baseBlock, World.Environment environment) {
		MVWorldManager worldManager = DescentIntoDarkness.multiverseCore.getMVWorldManager();
		String worldName = getWorldName(id);

		// Sometimes worlds can linger after a server crash
		if (worldManager.isMVWorld(worldName)) {
			World world = worldManager.getMVWorld(worldName).getCBWorld();
			return DescentIntoDarkness.plugin.supplyAsync(() -> {
				for (int x = -1024; x < 1024; x += 256) {
					for (int z = -1024; z < 1024; z += 256) {
						Bukkit.getLogger().log(Level.INFO, "Filling base area " + x + ", " + z);
						try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
							session.setBlocks((Region)new CuboidRegion(BlockVector3.at(x, 1, z), BlockVector3.at(x + 255, 254, z + 255)), baseBlock);
						}
					}
				}

				return world;
			});
		}

		String generator = "DescentIntoDarkness:full_" + baseBlock.getAsString();
		if (!worldManager.addWorld(worldName, environment, "0", WorldType.FLAT, Boolean.FALSE, generator, false)) {
			return Util.completeExceptionally(new RuntimeException("Could not create world"));
		}
		World world = worldManager.getMVWorld(worldName).getCBWorld();
		world.setGameRule(GameRule.DO_MOB_SPAWNING, false);

		// this is not what players are coming here to do...
		world.getEntitiesByClass(EnderDragon.class).forEach(Entity::remove);

		return CompletableFuture.completedFuture(world);
	}

}
