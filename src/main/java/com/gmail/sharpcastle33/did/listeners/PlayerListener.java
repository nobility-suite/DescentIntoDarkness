package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.instancing.CaveTrackerManager;
import com.onarandombox.MultiverseCore.event.MVRespawnEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
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

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		if (CaveTrackerManager.isCaveWorld(event.getFrom().getWorld())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (CaveTrackerManager.isCaveWorld(event.getFrom().getWorld())) {
			Location newLocation = DescentIntoDarkness.plugin.getCaveTrackerManager().respawnPlayer(event.getPlayer());
			if (newLocation != null) {
				event.setTo(newLocation);
			}
		}
	}
}
