package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class MobSpawnManager implements Runnable {
	private static final float NATURAL_POLLUTION_INCREASE = 0.1f;
	private static final int ATTEMPTS_PER_TICK = 10;
	public static final List<MobSpawnEntry> MOB_SPAWN_ENTRIES = ImmutableList.of(
			new MobSpawnEntry("zombie", EntityType.ZOMBIE, 50, 1, 10, 20, 20),
			new MobSpawnEntry("skeleton", EntityType.SKELETON, 70, 1, 15, 25, 20),
			new MobSpawnEntry("creeper", EntityType.CREEPER, 100, 2, 15, 25, 20)
	);

	private final Random rand = new Random();

	private void spawnMobs(CaveTracker cave) {
		if (cave.getPlayers().isEmpty()) {
			return;
		}

		if (cave.getSpawnCooldown() > 0) {
			cave.setSpawnCooldown(cave.getSpawnCooldown() - 1);
			return;
		}

		// Prevent lack of mob spawning due to idleness
		if (rand.nextFloat() < NATURAL_POLLUTION_INCREASE) {
			MobSpawnEntry mobType = getRandomSpawnEntry();
			if (mobType != null) {
				Player victim = getRandomPlayer(cave, mobType, 0);
				if (victim != null) {
					cave.addPlayerPollution(victim.getUniqueId(), mobType, 1);
				}
			}
		}

		for (int i = 0; i < ATTEMPTS_PER_TICK; i++) {
			if (spawnMob(cave)) {
				break;
			}
		}
	}

	private boolean spawnMob(CaveTracker cave) {
		// pick random type of mob to spawn and check if the cave has enough total pollution to spawn it
		MobSpawnEntry spawnEntry = getRandomSpawnEntry();
		if (spawnEntry == null) {
			return false;
		}
		if (spawnEntry.getPollutionCost() > cave.getTotalPollution()) {
			return false;
		}

		// try to spawn that mob next to a player with enough pollution to afford it, otherwise spawn it next to a random player with pollution
		Player chosenPlayer = getRandomPlayer(cave, spawnEntry, spawnEntry.getPollutionCost());
		if (chosenPlayer == null) {
			chosenPlayer = getRandomPlayer(cave, spawnEntry, 1);
			if (chosenPlayer == null) {
				return false;
			}
		}


		// Pick a random distance between minDistance and maxDistance, then a random point on the sphere with that radius.
		// Makes distances uniformly likely, as opposed to
		Vector spawnLocation = new Vector(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian())
				.normalize()
				.multiply(spawnEntry.getMinDistance() + rand.nextDouble() * (spawnEntry.getMaxDistance() - spawnEntry.getMinDistance()))
				.add(chosenPlayer.getLocation().toVector());
		spawnLocation.setY(Math.floor(spawnLocation.getY()));

		// quick exit for the blocks the mob will definitely intersect
		if (!cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ()).isPassable()) {
			return false;
		}
		if (!cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY() + 1, spawnLocation.getBlockZ()).isPassable()) {
			return false;
		}
		if (!cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY() - 1, spawnLocation.getBlockZ()).getType().isSolid()) {
			return false;
		}

		// spawn mob and check its hitbox doesn't intersect anything
		Entity mob = cave.getWorld().spawnEntity(spawnLocation.toLocation(cave.getWorld()), spawnEntry.getMob());
		for (int x = (int)Math.floor(mob.getBoundingBox().getMinX()); x <= (int)Math.ceil(mob.getBoundingBox().getMaxX()); x++) {
			for (int y = (int)Math.floor(mob.getBoundingBox().getMinY()); y <= (int)Math.ceil(mob.getBoundingBox().getMaxY()); y++) {
				for (int z = (int)Math.floor(mob.getBoundingBox().getMinZ()); z <= (int)Math.ceil(mob.getBoundingBox().getMaxZ()); z++) {
					if (!cave.getWorld().getBlockAt(x, y, z).isPassable()) {
						mob.remove();
						return false;
					}
				}
			}
		}
		mob.setRotation(rand.nextFloat() * 360, 0);

		// deduct pollution
		cave.addPlayerPollution(chosenPlayer.getUniqueId(), spawnEntry, -spawnEntry.getPollutionCost());
		cave.setSpawnCooldown(cave.getSpawnCooldown() + spawnEntry.getCooldown());

		return true;
	}

	@Nullable
	private Player getRandomPlayer(CaveTracker cave, MobSpawnEntry spawnEntry, int minPollution) {
		int totalOnlinePollution = cave.getPlayers().stream()
				.filter(this::isPlayerOnline)
				.mapToInt(player -> cave.getPlayerPollution(player, spawnEntry))
				.filter(pollution -> pollution >= minPollution)
				.sum();

		if (totalOnlinePollution <= 0) {
			List<UUID> players = cave.getPlayers().stream().filter(this::isPlayerOnline).filter(player -> cave.getPlayerPollution(player, spawnEntry) >= minPollution).collect(Collectors.toList());
			if (players.isEmpty()) {
				return null;
			}
			return Bukkit.getPlayer(players.get(rand.nextInt(players.size())));
		}

		int index = rand.nextInt(totalOnlinePollution);
		Player chosenPlayer = null;
		for (UUID player : cave.getPlayers()) {
			if (!isPlayerOnline(player)) {
				continue;
			}
			int pollution = cave.getPlayerPollution(player, spawnEntry);
			if (pollution < minPollution) {
				continue;
			}
			index -= pollution;
			if (index <= 0) {
				chosenPlayer = Bukkit.getPlayer(player);
				break;
			}
		}
		return chosenPlayer;
	}

	@Nullable
	public MobSpawnEntry getRandomSpawnEntry() {
		int totalWeight = MOB_SPAWN_ENTRIES.stream().mapToInt(MobSpawnEntry::getWeight).sum();
		int index = rand.nextInt(totalWeight);
		for (MobSpawnEntry entry : MOB_SPAWN_ENTRIES) {
			index -= entry.getWeight();
			if (index <= 0) {
				return entry;
			}
		}
		return null;
	}

	private boolean isPlayerOnline(UUID player) {
		OfflinePlayer p = Bukkit.getOfflinePlayer(player);
		//noinspection ConstantConditions - Bukkit derp
		return p != null && p.isOnline();
	}

	@Override
	public void run() {
		for (CaveTracker cave : DescentIntoDarkness.plugin.getCaveTrackerManager().getCaves()) {
			spawnMobs(cave);
		}
	}
}
