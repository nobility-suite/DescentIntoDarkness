package com.gmail.sharpcastle33.did.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import net.md_5.bungee.api.ChatColor;

public class CaveEntranceListener implements Listener {
	
	
	//Hacky, piggybacks on join command
	public void interactEvent(PlayerInteractEntityEvent event) {
		Entity e = event.getRightClicked();
		if(e instanceof Villager) {
			Villager v = (Villager) e;
			String name = v.getCustomName();
			if(name.startsWith(ChatColor.BLUE + "Cave")) {
				String[] toParse = name.split("[");
				String parse = toParse[1];
				parse.replace("]", "");
				int id = Integer.parseInt(parse);
				
				String[] args = {parse};
				
				CommandListener.join(event.getPlayer(), args);
			}
		}
	}

}
