package com.gmail.sharpcastle33.instancing;

import java.util.ArrayList;
import org.bukkit.entity.Player;

public class InstanceManager {

	ArrayList<Instance> instances;

	public InstanceManager() {
		init();
	}

	public void init() {
		instances = new ArrayList<>();
	}

	public Instance createInstance() {
		return null; //TODO
	}

	public void deleteInstance() {
		//TODO
	}

	public boolean inInstance(Player p) {
		for(Instance i : instances) {
			ArrayList<Player> members = i.getMembers();
			if(members.contains(p)) {
				return true;
			}
		}
		return false;
	}

	public Instance getInstance(Player p) {
		for(Instance i : instances) {
			ArrayList<Player> members = i.getMembers();
			if(members.contains(p)) {
				return i;
			}
		}
		return null;
	}

}
