package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

public class CaveTracker {

	private final int id;
	@Nullable
	private DyeColor color;
	private boolean hasBeenJoined;
	private long joinTime;
	private final World world;
	private final Location start;
	private final long seed;
	private final CaveStyle style;
	private final List<UUID> players = new ArrayList<>();
	private final Map<UUID, Long> lastLeaveTime = new HashMap<>();
	private int totalPollution;
	private final Map<MobSpawnEntry, MobEntry> mobEntries = new HashMap<>();
	private final Team team;
	private int spawnCooldown;
	private final Map<BlockVector3, Integer> blockBreakCounts = new HashMap<>();
	private final List<BlockVector2> chunkPositions;

	public CaveTracker(World world, ConfigurationSection map) {
		this(map.getInt("id"), world, parseStart(world, map), map.getLong("seed"), parseCaveStyle(map), parseChunkPositions(map));
		this.hasBeenJoined = map.getBoolean("hasBeenJoined");
		this.joinTime = map.getLong("joinTime");
		this.totalPollution = map.getInt("totalPollution");
		this.spawnCooldown = map.getInt("spawnCooldown");
		for (String player : map.getStringList("players")) {
			try {
				this.players.add(UUID.fromString(player));
			} catch (IllegalArgumentException ignore) {
			}
		}
		ConfigurationSection playerLeaveTimes = map.getConfigurationSection("playerLeaveTimes");
		if (playerLeaveTimes != null) {
			for (String player : playerLeaveTimes.getKeys(false)) {
				try {
					this.lastLeaveTime.put(UUID.fromString(player), playerLeaveTimes.getLong(player));
				} catch (IllegalArgumentException ignore) {
				}
			}
		}
		ConfigurationSection mobEntries = map.getConfigurationSection("mobEntries");
		if (mobEntries != null) {
			for (String mobSpawnEntry : mobEntries.getKeys(false)) {
				MobSpawnEntry spawnEntry = style.getSpawnEntries().stream().filter(it -> it.getName().equals(mobSpawnEntry)).findAny().orElse(null);
				if (spawnEntry == null) {
					continue;
				}
				ConfigurationSection mobEntry = mobEntries.getConfigurationSection(mobSpawnEntry);
				if (mobEntry != null) {
					MobEntry entry = new MobEntry(spawnEntry, mobEntry);
					this.mobEntries.put(spawnEntry, entry);
				}
			}
		}
		List<Map<?, ?>> blockBreakCounts = map.getMapList("blockBreakCounts");
		for (Map<?, ?> entry : blockBreakCounts) {
			ConfigurationSection entrySection = ConfigUtil.asConfigurationSection(entry);
			BlockVector3 pos = BlockVector3.at(entrySection.getInt("x"), entrySection.getInt("y"), entrySection.getInt("z"));
			this.blockBreakCounts.put(pos, entrySection.getInt("count"));
		}
	}

	private static Location parseStart(World world, ConfigurationSection map) {
		ConfigurationSection start = ConfigUtil.asConfigurationSection(ConfigUtil.require(map, "start"));
		return new Location(world, start.getDouble("x"), start.getDouble("y"), start.getDouble("z"));
	}

	private static CaveStyle parseCaveStyle(ConfigurationSection map) {
		CaveStyle style = DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName().get(ConfigUtil.requireString(map, "style"));
		if (style == null) {
			throw new InvalidConfigException("Invalid cave style: " + map.getString("style"));
		}
		return style;
	}

	private static List<BlockVector2> parseChunkPositions(ConfigurationSection map) {
		List<Map<?, ?>> chunkPositions = map.getMapList("chunkPositions");
		List<BlockVector2> result = new ArrayList<>();
		for (Map<?, ?> entry : chunkPositions) {
			ConfigurationSection entrySection = ConfigUtil.asConfigurationSection(entry);
			result.add(BlockVector2.at(entrySection.getInt("x"), entrySection.getInt("z")));
		}
		return result;
	}

	public void serialize(ConfigurationSection map) {
		map.set("id", id);
		map.set("hasBeenJoined", hasBeenJoined);
		map.set("joinTime", joinTime);
		map.set("start.x", start.getX());
		map.set("start.y", start.getY());
		map.set("start.z", start.getZ());
		map.set("seed", seed);
		map.set("style", style.getName());
		map.set("totalPollution", totalPollution);
		map.set("spawnCooldown", spawnCooldown);
		map.set("players", players.stream().map(UUID::toString).toArray(String[]::new));
		ConfigurationSection playerLeaveTimes = map.createSection("playerLeaveTimes");
		lastLeaveTime.forEach((key, value) -> playerLeaveTimes.set(key.toString(), value));
		mobEntries.forEach((spawnEntry, entry) -> {
			ConfigurationSection mobEntry = map.createSection("mobEntries." + spawnEntry.getName());
			entry.serialize(mobEntry);
		});
		List<ConfigurationSection> blockBreakCounts = new ArrayList<>();
		this.blockBreakCounts.forEach((pos, count) -> {
			ConfigurationSection section = new MemoryConfiguration();
			section.set("x", pos.getBlockX());
			section.set("y", pos.getBlockY());
			section.set("z", pos.getBlockZ());
			section.set("count", count);
			blockBreakCounts.add(section);
		});
		map.set("blockBreakCounts", blockBreakCounts);
		List<ConfigurationSection> chunkPositions = new ArrayList<>();
		this.chunkPositions.forEach(pos -> {
			ConfigurationSection section = new MemoryConfiguration();
			section.set("x", pos.getBlockX());
			section.set("z", pos.getBlockZ());
			chunkPositions.add(section);
		});
		map.set("chunkPositions", chunkPositions);
	}

	public CaveTracker(int id, World world, Location start, long seed, CaveStyle style, List<BlockVector2> chunkPositions) {
		this.id = id;
		this.world = world;
		this.start = start;
		this.seed = seed;
		this.style = style;
		this.chunkPositions = chunkPositions;

		Team team = DescentIntoDarkness.instance.getScoreboard().getTeam("cave_" + id);
		if (team == null) {
			team = DescentIntoDarkness.instance.getScoreboard().registerNewTeam("cave_" + id);
		}
		this.team = team;
	}

	public int getId() {
		return id;
	}

	@Nullable
	public DyeColor getColor() {
		return color;
	}

	public void setColor(DyeColor color) {
		this.color = color;
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
		lastLeaveTime.put(player, world.getFullTime());
	}

	public OptionalLong getLastLeaveTime(UUID player) {
		Long lastLeaveTime = this.lastLeaveTime.get(player);
		if (lastLeaveTime == null) {
			return OptionalLong.empty();
		} else {
			return OptionalLong.of(lastLeaveTime);
		}
	}

	public List<BlockVector2> getChunkPositions() {
		return chunkPositions;
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

		public MobEntry(MobSpawnEntry spawnEntry, ConfigurationSection map) {
			this.spawnEntry = spawnEntry;
			this.totalPollution = map.getInt("totalPollution");
			this.packSpawnThreshold = map.getInt("packSpawnThreshold");
			this.spawningPack = map.getBoolean("spawningPack");
			ConfigurationSection playerPollutions = map.getConfigurationSection("playerPollutions");
			if (playerPollutions != null) {
				for (String key : playerPollutions.getKeys(false)) {
					try {
						this.playerPollutions.put(UUID.fromString(key), playerPollutions.getInt(key));
					} catch (IllegalArgumentException ignore) {
					}
				}
			}
		}

		public MobEntry(MobSpawnEntry spawnEntry) {
			this.spawnEntry = spawnEntry;
			Random rand = new Random();
			packSpawnThreshold = spawnEntry.getMinPackCost() + rand.nextInt(spawnEntry.getMaxPackCost() - spawnEntry.getMinPackCost() + 1);
		}

		public void serialize(ConfigurationSection map) {
			map.set("totalPollution", totalPollution);
			map.set("packSpawnThreshold", packSpawnThreshold);
			map.set("spawningPack", spawningPack);
			ConfigurationSection playerPollutions = map.createSection("playerPollutions");
			this.playerPollutions.forEach((key, value) -> playerPollutions.set(key.toString(), value));
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
