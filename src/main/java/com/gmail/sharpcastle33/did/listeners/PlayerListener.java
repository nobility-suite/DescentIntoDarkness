package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.Ore;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.gmail.sharpcastle33.did.instancing.CaveTrackerManager;
import com.onarandombox.MultiverseCore.event.MVRespawnEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
	@EventHandler
	public void onMVPlayerRespawn(MVRespawnEvent event) {
		Location newLocation = DescentIntoDarkness.instance.getCaveTrackerManager().respawnPlayer(event.getPlayer());
		if (newLocation != null) {
			event.setRespawnLocation(newLocation);
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event.isBedSpawn()) { // MV-Core does not handle this case
			Location newLocation = DescentIntoDarkness.instance.getCaveTrackerManager().respawnPlayer(event.getPlayer());
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
		if (CaveTrackerManager.isCaveWorld(event.getFrom().getWorld()) && !DescentIntoDarkness.instance.getCaveTrackerManager().isLeavingCave()) {
			DescentIntoDarkness.instance.getCaveTrackerManager().teleportPlayerTo(event.getPlayer(), null);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockPlaced(BlockPlaceEvent event) {
		CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveForPlayer(event.getPlayer());
		if (cave == null) {
			return;
		}

		boolean canPlace = true;
		if (cave.getStyle().getCannotPlace().contains(event.getItemInHand().getType())) {
			canPlace = false;
		} else {
			for (Ore ore : cave.getStyle().getOres()) {
				if (ore.getBlock().test(BukkitAdapter.adapt(event.getBlockPlaced().getBlockData()))) {
					canPlace = false;
					break;
				}
			}
		}
		if (!canPlace) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveForPlayer(event.getPlayer());
		if (cave == null) {
			return;
		}

		if (cave.getStyle().getCannotPlace().contains(event.getMaterial())) {
			event.setCancelled(true);
		}
	}
}
