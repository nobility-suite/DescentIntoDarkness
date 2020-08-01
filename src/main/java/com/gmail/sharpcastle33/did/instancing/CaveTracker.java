package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import org.bukkit.Location;
import org.bukkit.World;

public class CaveTracker {

	private final int id;
	private final World world;
	private final Location start;
	private final CaveStyle style;
	private final List<UUID> players = new ArrayList<>();
	private int totalPollution;
	private final Map<UUID, Integer> playerPollutions = new HashMap<>();

	public CaveTracker(int id, World world, Location start, CaveStyle style) {
		this.id = id;
		this.world = world;
		this.start = start;
		this.style = style;
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
	}

	public void removePlayer(UUID player) {
		playerPollutions.remove(player);
		this.players.remove(player);
	}

	public int getPlayerPollution(UUID player) {
		Integer pollution = playerPollutions.get(player);
		return pollution == null ? 0 : pollution;
	}

	public void addPlayerPollution(UUID player, int amt) {
		playerPollutions.merge(player, amt, Integer::sum);
		totalPollution += amt;
	}

	public int getTotalPollution() {
		return totalPollution;
	}
}
