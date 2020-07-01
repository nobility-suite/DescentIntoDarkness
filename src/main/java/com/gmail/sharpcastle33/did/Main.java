package com.gmail.sharpcastle33.did;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.sharpcastle33.did.listeners.CommandListener;
import com.gmail.sharpcastle33.did.listeners.OreListener;
import com.gmail.sharpcastle33.dungeonmaster.DungeonMaster;

public class Main extends JavaPlugin{

	public DungeonMaster dungeonMaster;
	public static Main plugin;
	
	@Override
	public void onEnable() {
		plugin = this;
		dungeonMaster = new DungeonMaster();
		getCommand("did").setExecutor(new CommandListener());
		Bukkit.getPluginManager().registerEvents(new OreListener(), plugin);
	}
	
	public DungeonMaster getDungeonMaster() {
		return this.dungeonMaster;
	}
}
