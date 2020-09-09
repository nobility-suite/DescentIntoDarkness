package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.Nullable;

public class CaveTrackerManager {
	private static final String WORLD_NAME = "did_caves";
	private static final int INSTANCE_WIDTH_CHUNKS = 625;

	private boolean hasInitialized = false;
	private final int instanceLimit;
	private World theWorld;
	private final ArrayList<CaveTracker> caveTrackers = new ArrayList<>();
	private final Map<UUID, Location> overworldPlayerLocations = new HashMap<>();
	private int nextInstanceId;
	private Objective pollutionObjective;
	private final AtomicBoolean generatingCave = new AtomicBoolean(false);
	private ThreadLocal<Boolean> isLeavingCave = new ThreadLocal<>();

	public CaveTrackerManager(int instanceLimit) {
		this.instanceLimit = instanceLimit;
	}

	public void initialize() {
		theWorld = getOrCreateFlatWorld();
		if (theWorld == null) {
			throw new RuntimeException("Failed to create world");
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
			Bukkit.getLogger().log(Level.INFO, "Cave " + cave.getId() + " is ready to join!");
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
		if (caveStyle == null || caveStyle.isAbstract()) {
			Bukkit.getLogger().log(Level.SEVERE, "Cannot instantiate cave style: " + chosenStyle);
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
		while (getCaveById(nextInstanceId) != null) {
			nextInstanceId = (nextInstanceId + 1) % instanceLimit;
			if (nextInstanceId == oldInstanceId) {
				return Util.completeExceptionally(new RuntimeException("Could not create cave instances: no free caves left"));
			}
		}

		int id = nextInstanceId;

		Bukkit.getLogger().log(Level.INFO, "Generating cave with ID " + id);

		return DescentIntoDarkness.plugin.supplyAsync(() -> {
			BlockVector2 caveChunkCoords = getInstanceChunkCoords(id);
			BlockVector3 spawnPos = BlockVector3.at(caveChunkCoords.getBlockX() * 16, 210, caveChunkCoords.getBlockZ() * 16);
			Random rand = new Random();
			CuboidRegion limit = new CuboidRegion(
					spawnPos.multiply(1, 0, 1).subtract(8 * INSTANCE_WIDTH_CHUNKS - 32, 0, 8 * INSTANCE_WIDTH_CHUNKS - 32),
					spawnPos.multiply(1, 0, 1).add(8 * INSTANCE_WIDTH_CHUNKS - 32, 255, 8 * INSTANCE_WIDTH_CHUNKS - 32)
			);
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(theWorld), style, rand).limit(limit)) {
				CaveGenerator.generateCave(ctx, spawnPos.toVector3());
			} catch (WorldEditException e) {
				throw new RuntimeException("Could not generate cave", e);
			}
			Location spawnPoint = BukkitAdapter.adapt(theWorld, spawnPos);
			while (style.isTransparentBlock(BukkitAdapter.adapt(spawnPoint.getBlock().getBlockData()))) {
				spawnPoint.add(0, -1, 0);
			}
			spawnPoint.add(0, 1, 0);
			return DescentIntoDarkness.plugin.supplySyncNow(() -> {
				CaveTracker caveTracker = new CaveTracker(id, theWorld, spawnPoint, style);
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
		return getCaveForPlayer(p) != null;
	}

	@Nullable
	public Location respawnPlayer(Player p) {
		CaveTracker existingCave = getCaveForPlayer(p);
		if (existingCave == null) {
			return null;
		}

		existingCave.removePlayer(p.getUniqueId());
		if (existingCave.getPlayers().isEmpty()) {
			deleteCave(existingCave);
		}

		Location newLocation = overworldPlayerLocations.remove(p.getUniqueId());
		if (newLocation == null) {
			newLocation = p.getBedSpawnLocation();
			if (newLocation == null) {
				newLocation = DescentIntoDarkness.multiverseCore.getMVWorldManager().getSpawnWorld().getSpawnLocation();
			}
		}
		return newLocation;
	}

	public boolean teleportPlayerTo(Player p, @Nullable CaveTracker newCave) {
		CaveTracker existingCave = getCaveForPlayer(p);
		if (existingCave == newCave) {
			return true;
		}

		if (newCave == null) {
			Location newLocation = respawnPlayer(p);
			if (newLocation != null) {
				isLeavingCave.set(true);
				try {
					p.teleport(newLocation);
				} finally {
					isLeavingCave.set(false);
				}
				return true;
			} else {
				return false;
			}
		}

		if (existingCave == null) {
			overworldPlayerLocations.put(p.getUniqueId(), p.getLocation());
		} else {
			existingCave.removePlayer(p.getUniqueId());
			if (existingCave.getPlayers().isEmpty()) {
				deleteCave(existingCave);
			}
		}

		Location start = newCave.getStart();
		String destStr = String.format("e:%s:%f,%f,%f", WORLD_NAME, start.getX(), start.getY(), start.getZ());
		MVDestination dest = DescentIntoDarkness.multiverseCore.getDestFactory().getDestination(destStr);
		if (DescentIntoDarkness.multiverseCore.getSafeTTeleporter().teleport(Bukkit.getConsoleSender(), p, dest) != TeleportResult.SUCCESS) {
			return false;
		}
		newCave.addPlayer(p.getUniqueId());

		return true;
	}

	public boolean isLeavingCave() {
		return isLeavingCave.get();
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
	public CaveTracker getCaveForPlayer(Player p) {
		for (CaveTracker cave : caveTrackers) {
			List<UUID> members = cave.getPlayers();
			if(members.contains(p.getUniqueId())) {
				return cave;
			}
		}
		return null;
	}

	public static boolean isCaveWorld(World world) {
		MultiverseWorld mvWorld = DescentIntoDarkness.multiverseCore.getMVWorldManager().getMVWorld(world);
		return mvWorld != null && mvWorld.getName().equals(WORLD_NAME);
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

	@Nullable
	private World getOrCreateFlatWorld() {
		MVWorldManager worldManager = DescentIntoDarkness.multiverseCore.getMVWorldManager();
		// Sometimes worlds can linger after a server crash
		if (worldManager.isMVWorld(WORLD_NAME)) {
			return worldManager.getMVWorld(WORLD_NAME).getCBWorld();
		}

		String generator = "DescentIntoDarkness:full_" + Util.requireDefaultState(BlockTypes.STONE).getAsString();
		if (!worldManager.addWorld(WORLD_NAME, World.Environment.THE_END, "0", WorldType.FLAT, Boolean.FALSE, generator, false)) {
			return null;
		}
		MultiverseWorld mvWorld = worldManager.getMVWorld(WORLD_NAME);
		mvWorld.setRespawnToWorld(worldManager.getSpawnWorld().getName());
		World world = mvWorld.getCBWorld();
		world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

		return world;
	}

	private BlockVector2 getInstanceChunkCoords(int instanceId) {
		int ring = (int) (0.5 * (Math.sqrt(instanceId + 1) - 1) + 0.00000001); // hopefully this addition compensates for rounding errors
		int radius = ring + 1;
		int instanceInRing = instanceId - 4 * (ring * ring + ring);
		int d = instanceInRing % (radius + radius);
		switch (instanceInRing / (radius + radius)) {
			case 0: return BlockVector2.at(d - radius, -radius).multiply(INSTANCE_WIDTH_CHUNKS);
			case 1: return BlockVector2.at(radius, d - radius).multiply(INSTANCE_WIDTH_CHUNKS);
			case 2: return BlockVector2.at(radius - d, radius).multiply(INSTANCE_WIDTH_CHUNKS);
			case 3: return BlockVector2.at(-radius, radius - d).multiply(INSTANCE_WIDTH_CHUNKS);
			default: throw new ArithmeticException("Earth is bad at math!");
		}
	}

}
