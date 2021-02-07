package com.gmail.sharpcastle33.did.listeners;

import org.bukkit.Bukkit;
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
			Bukkit.getLogger().info(event.getPlayer().getDisplayName() + "-- clicked villager");
			Villager v = (Villager) e;
			String name = v.getCustomName();
			if(name.startsWith(ChatColor.BLUE + "Cave")) {
				Bukkit.getLogger().info(event.getPlayer().getDisplayName() + "-- clicked cave villager");

				String[] toParse = name.split("[");
				String parse = toParse[1];
				parse.replace("]", "");
				int id = Integer.parseInt(parse);
				
				String[] args = {"join",parse};
				Bukkit.getLogger().info(event.getPlayer().getDisplayName() + "-- clicked cave villager, joining cave: " + parse);

				CommandListener.join(event.getPlayer(), args);
			}
		}
	}

}
