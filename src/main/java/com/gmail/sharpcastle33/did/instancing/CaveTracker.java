package com.gmail.sharpcastle33.did.instancing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

public class CaveTracker {

	private final int id;
	private final World world;
	private final Location start;
	private final List<UUID> members = new ArrayList<>();
	private int danger;

	public CaveTracker(int id, World world, Location start) {
		this.id = id;
		this.world = world;
		this.start = start;
		this.danger = 0;
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

	public List<UUID> getMembers() {
		return members;
	}

	public void addMember(UUID p) {
		this.members.add(p);
	}

	public void removeMember(UUID p) {
		this.members.remove(p);
	}

	public int getDanger() {
		return danger;
	}

	public void setDanger(int danger) {
		this.danger = danger;
	}

}
