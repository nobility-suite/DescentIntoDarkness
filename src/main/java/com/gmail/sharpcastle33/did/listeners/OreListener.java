package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.gmail.sharpcastle33.did.config.Ore;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class OreListener implements Listener {

	private final Random rand = new Random();

	@EventHandler
	public void oreBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveForPlayer(player);
		if (cave == null) {
			return;
		}

		BlockStateHolder<?> block = BukkitAdapter.adapt(event.getBlock().getBlockData());
		BlockVector3 pos = BukkitAdapter.asBlockVector(event.getBlock().getLocation());
		boolean isOre = false;
		for (Ore ore : cave.getStyle().getOres()) {
			if (ore.getBlock().test(block)) {
				isOre = true;
				int blockBreakCount = cave.getBlockBreakCount(pos) + 1;
				TextComponent numberPart = new TextComponent(blockBreakCount + "/" + ore.getBreakAmount());
				numberPart.setColor(ChatColor.WHITE);
				TextComponent message = new TextComponent("Break progress: ");
				message.setColor(ChatColor.BLUE);
				message.setExtra(Lists.newArrayList(numberPart));
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
				if (blockBreakCount < ore.getBreakAmount()) {
					cave.setBlockBreakCount(pos, blockBreakCount);
					event.setCancelled(true);
				} else {
					cave.setBlockBreakCount(pos, 0);
				}

				MobSpawnEntry spawnEntry = DescentIntoDarkness.instance.getMobSpawnManager().getRandomSpawnEntry(cave);
				if (spawnEntry != null) {
					cave.addPlayerMobPollution(player.getUniqueId(), spawnEntry, ore.getPollution());
				}
				if (ore.getDropTable() != null) {
					List<Ore.Drop> dropTable = ore.getDropTable();
					int randVal = rand.nextInt(dropTable.stream().mapToInt(Ore.Drop::getWeight).sum());
					Ore.Drop drop = null;
					for (Ore.Drop d : dropTable) {
						randVal -= d.getWeight();
						if (randVal < 0) {
							drop = d;
							break;
						}
					}
					assert drop != null;
					int amount = drop.getMinAmount() + rand.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
					if (amount > 0) {
						ItemStack toDrop = new ItemStack(drop.getItem());
						toDrop.setAmount(amount);
						player.getWorld().dropItemNaturally(event.getBlock().getLocation(), toDrop);
					}
					event.setDropItems(false);
					event.setExpToDrop(0);
				} else if (blockBreakCount < ore.getBreakAmount()) {
					for (ItemStack drop : event.getBlock().getDrops(player.getInventory().getItemInMainHand())) {
						player.getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
					}
				}
				break;
			}
		}

		if (!isOre) {
			MobSpawnEntry spawnEntry = DescentIntoDarkness.instance.getMobSpawnManager().getRandomSpawnEntry(cave);
			if (spawnEntry != null) {
				cave.addPlayerMobPollution(player.getUniqueId(), spawnEntry, cave.getStyle().getBlockBreakPollution());
			}
		}
	}

	@EventHandler
	public void blockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveForPlayer(player);
		if (cave == null) {
			return;
		}

		MobSpawnEntry spawnEntry = DescentIntoDarkness.instance.getMobSpawnManager().getRandomSpawnEntry(cave);
		if (spawnEntry != null) {
			cave.addPlayerMobPollution(player.getUniqueId(), spawnEntry, cave.getStyle().getBlockPlacePollution());
		}
	}

}
