package com.gmail.sharpcastle33.did.listeners;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class OreListener implements Listener{

	HashMap<Location,Integer> oreMap;

	public OreListener() {
		oreMap = new HashMap<>();
	}

	@EventHandler
	public void oreBreak(BlockBreakEvent event) {
		Player p = event.getPlayer();
		Block b = event.getBlock();

		if(b.getType() == Material.DIAMOND_ORE || b.getType() == Material.IRON_ORE || b.getType() == Material.COAL_ORE) {
			Location loc = b.getLocation();
			if(oreMap.containsKey(loc)) {
				int get = oreMap.get(loc);
				if(get == 1) {
					oreMap.remove(loc);
					b.setType(Material.AIR);
					World world = b.getLocation().getWorld();
					assert world != null;
					world.playSound(b.getLocation(), Sound.ITEM_SHIELD_BREAK, 1, 1);
					dropOre(b);
				}else {
					oreMap.put(loc, get - 1);
					event.setCancelled(true);
					dropOre(b);
				}
			}else{
				oreMap.put(loc, 9);
				event.setCancelled(true);
				dropOre(b);
			}
		}
	}

	public void dropOre(Block b) {
		Material mat = b.getType();
		World world = b.getLocation().getWorld();
		assert world != null;
		world.dropItemNaturally(b.getLocation(), new ItemStack(Material.COAL));
	}

}
