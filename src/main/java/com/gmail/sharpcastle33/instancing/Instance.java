package com.gmail.sharpcastle33.instancing;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Instance {

	private final int id;
	private final World world;
	private final Location start;
	private ArrayList<Player> members;
	private int danger;

	public Instance(int id, World world, Location start) {
		this.id = id;
		this.world = world;
		this.start = start;
		this.danger = 0;
		members = new ArrayList<>();
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

	public ArrayList<Player> getMembers() {
		return members;
	}

	public void addMember(Player p) {
		this.members.add(p);
	}

	public void removeMember(Player p) {
		this.members.remove(p);
	}

	public int getDanger() {
		return danger;
	}

	public void setDanger(int danger) {
		this.danger = danger;
	}

}
