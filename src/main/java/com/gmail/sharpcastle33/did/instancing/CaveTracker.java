package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.gmail.sharpcastle33.did.listeners.MobSpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

public class CaveTracker {

	private final int id;
	private final World world;
	private final Location start;
	private final CaveStyle style;
	private final List<UUID> players = new ArrayList<>();
	private int totalPollution;
	private final Map<UUID, Map<String, Integer>> playerMobPollutions = new HashMap<>();
	private final Team team;
	private int spawnCooldown;

	public CaveTracker(int id, World world, Location start, CaveStyle style) {
		this.id = id;
		this.world = world;
		this.start = start;
		this.style = style;
		this.team = DescentIntoDarkness.plugin.getScoreboard().registerNewTeam("cave_" + id);
	}

	public int getId() {
		return id;
	}

	public World getWorld() {
		return world;
	}

	public Location getStart() {
		return start;
	}

	public CaveStyle getStyle() {
		return style;
	}

	public List<UUID> getPlayers() {
		return players;
	}

	public void addPlayer(UUID player) {
		this.players.add(player);
		for (MobSpawnEntry spawnEntry : MobSpawnManager.MOB_SPAWN_ENTRIES) {
			team.addEntry(getPollutionScore(player, spawnEntry).getEntry());
		}
	}

	public void removePlayer(UUID player) {
		playerMobPollutions.remove(player);
		this.players.remove(player);
		for (MobSpawnEntry spawnEntry : MobSpawnManager.MOB_SPAWN_ENTRIES) {
			team.removeEntry(getPollutionScore(player, spawnEntry).getEntry());
			Util.resetScore(getPollutionScore(player, spawnEntry));
		}
	}

	public int getPlayerPollution(UUID player, MobSpawnEntry spawnEntry) {
		Map<String, Integer> spawnEntries = playerMobPollutions.get(player);
		if (spawnEntries == null) {
			return 0;
		}
		Integer pollution = spawnEntries.get(spawnEntry.getName());
		return pollution == null ? 0 : pollution;
	}

	public void addPlayerPollution(UUID player, MobSpawnEntry spawnEntry, int amt) {
		playerMobPollutions.computeIfAbsent(player, k -> new HashMap<>()).merge(spawnEntry.getName(), amt, Integer::sum);
		totalPollution += amt;
		getPollutionScore(player, spawnEntry).setScore(getPlayerPollution(player, spawnEntry));
	}

	public int getTotalPollution() {
		return totalPollution;
	}

	private Score getPollutionScore(UUID player, MobSpawnEntry spawnEntry) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
		//noinspection ConstantConditions - Bukkit derp
		String name = offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : player.toString();
		name += "_" + spawnEntry.getName();
		return DescentIntoDarkness.plugin.getCaveTrackerManager().getPollutionObjective().getScore(name);
	}

	public Team getTeam() {
		return team;
	}

	public int getSpawnCooldown() {
		return spawnCooldown;
	}

	public void setSpawnCooldown(int spawnCooldown) {
		this.spawnCooldown = spawnCooldown;
	}
}
