package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.onarandombox.MultiverseCore.event.MVRespawnEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
	@EventHandler
	public void onMVPlayerRespawn(MVRespawnEvent event) {
		Location newLocation = DescentIntoDarkness.plugin.getCaveTrackerManager().respawnPlayer(event.getPlayer());
		if (newLocation != null) {
			event.setRespawnLocation(newLocation);
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event.isBedSpawn()) { // MV-Core does not handle this case
			Location newLocation = DescentIntoDarkness.plugin.getCaveTrackerManager().respawnPlayer(event.getPlayer());
			if (newLocation != null) {
				event.setRespawnLocation(newLocation);
			}
		}
	}
}
