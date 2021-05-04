package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

public class CaveTracker {

	private final int id;
	private boolean hasBeenJoined;
	private long joinTime;
	private final World world;
	private final Location start;
	private final CaveStyle style;
	private final List<UUID> players = new ArrayList<>();
	private int totalPollution;
	private final Map<MobSpawnEntry, MobEntry> mobEntries = new HashMap<>();
	private final Team team;
	private int spawnCooldown;
	private final Map<BlockVector3, Integer> blockBreakCounts = new HashMap<>();

	public CaveTracker(int id, World world, Location start, CaveStyle style) {
		this.id = id;
		this.world = world;
		this.start = start;
		this.style = style;

		Team team = DescentIntoDarkness.instance.getScoreboard().getTeam("cave_" + id);
		if (team == null) {
			team = DescentIntoDarkness.instance.getScoreboard().registerNewTeam("cave_" + id);
		}
		this.team = team;
	}

	public int getId() {
		return id;
	}

	public boolean hasBeenJoined() {
		return hasBeenJoined;
	}

	public long getJoinTime() {
		return joinTime;
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
		if (!hasBeenJoined) {
			hasBeenJoined = true;
			joinTime = world.getFullTime();
		}
		this.players.add(player);
		for (MobSpawnEntry spawnEntry : style.getSpawnEntries()) {
			team.addEntry(getPollutionScore(player, spawnEntry).getEntry());
		}
	}

	public void removePlayer(UUID player) {
		mobEntries.values().forEach(entry -> entry.playerPollutions.remove(player));
		this.players.remove(player);
		for (MobSpawnEntry spawnEntry : style.getSpawnEntries()) {
			team.removeEntry(getPollutionScore(player, spawnEntry).getEntry());
			Util.resetScore(getPollutionScore(player, spawnEntry));
		}
	}

	public MobEntry getMobEntry(MobSpawnEntry spawnEntry) {
		return mobEntries.computeIfAbsent(spawnEntry, MobEntry::new);
	}

	public void addPlayerMobPollution(UUID player, MobSpawnEntry spawnEntry, int amt) {
		int newPlayerPollution = getMobEntry(spawnEntry).addPlayerPollution(player, amt);
		totalPollution += amt;
		getPollutionScore(player, spawnEntry).setScore(newPlayerPollution);
	}

	public int getTotalPollution() {
		return totalPollution;
	}

	private Score getPollutionScore(UUID player, MobSpawnEntry spawnEntry) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
		//noinspection ConstantConditions - Bukkit derp
		String name = offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : player.toString();
		name += "_" + spawnEntry.getName();
		return DescentIntoDarkness.instance.getCaveTrackerManager().getPollutionObjective().getScore(name);
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

	public int getBlockBreakCount(BlockVector3 pos) {
		Integer count = blockBreakCounts.get(pos);
		return count == null ? 0 : count;
	}

	public void setBlockBreakCount(BlockVector3 pos, int count) {
		blockBreakCounts.put(pos, count);
	}

	public static class MobEntry {
		private final MobSpawnEntry spawnEntry;
		private int totalPollution;
		private final Map<UUID, Integer> playerPollutions = new HashMap<>();
		private int packSpawnThreshold;
		private boolean spawningPack;

		public MobEntry(MobSpawnEntry spawnEntry) {
			this.spawnEntry = spawnEntry;
			Random rand = new Random();
			packSpawnThreshold = spawnEntry.getMinPackCost() + rand.nextInt(spawnEntry.getMaxPackCost() - spawnEntry.getMinPackCost() + 1);
		}

		public MobSpawnEntry getSpawnEntry() {
			return spawnEntry;
		}

		public int getTotalPollution() {
			return totalPollution;
		}

		public int getPlayerPollution(UUID player) {
			Integer pollution = playerPollutions.get(player);
			return pollution == null ? 0 : pollution;
		}

		public int addPlayerPollution(UUID player, int amt) {
			totalPollution += amt;
			return playerPollutions.merge(player, amt, Integer::sum);
		}

		public int getPackSpawnThreshold() {
			return packSpawnThreshold;
		}

		public void setPackSpawnThreshold(int packSpawnThreshold) {
			this.packSpawnThreshold = packSpawnThreshold;
		}

		public boolean isSpawningPack() {
			return spawningPack;
		}

		public void setSpawningPack(boolean spawningPack) {
			this.spawningPack = spawningPack;
		}
	}
}
