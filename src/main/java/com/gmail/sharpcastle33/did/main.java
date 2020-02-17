package com.gmail.sharpcastle33.did;

import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.sharpcastle33.did.listeners.CommandListener;

public class main extends JavaPlugin{

	@Override
	public void onEnable() {
		getCommand("did").setExecutor(new CommandListener());
	}
}
