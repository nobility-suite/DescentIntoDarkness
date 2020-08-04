package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.gmail.sharpcastle33.did.config.Ore;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class OreListener implements Listener {

	private final Random rand = new Random();

	@EventHandler
	public void oreBreak(BlockBreakEvent event) {
		Player p = event.getPlayer();
		CaveTracker cave = DescentIntoDarkness.plugin.getCaveTrackerManager().getCave(p);
		if (cave == null) {
			return;
		}

		BlockStateHolder<?> block = BukkitAdapter.adapt(event.getBlock().getBlockData());
		for (Ore ore : cave.getStyle().getOres()) {
			if (ore.getBlock().equalsFuzzy(block)) {
				MobSpawnEntry spawnEntry = DescentIntoDarkness.plugin.getMobSpawnManager().getRandomSpawnEntry();
				if (spawnEntry != null) {
					cave.addPlayerMobPollution(p.getUniqueId(), spawnEntry, ore.getPollution());
				}
				if (ore.getDropItem() != null) {
					ItemStack toDrop = new ItemStack(ore.getDropItem());
					toDrop.setAmount(ore.getMinDropAmount() + rand.nextInt(ore.getMaxDropAmount() - ore.getMinDropAmount() + 1));
					p.getWorld().dropItemNaturally(event.getBlock().getLocation(), toDrop);
					event.setDropItems(false);
					event.setExpToDrop(0);
				}
				break;
			}
		}
	}

}
