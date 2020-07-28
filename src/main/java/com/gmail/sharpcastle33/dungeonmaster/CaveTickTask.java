package com.gmail.sharpcastle33.dungeonmaster;

import java.util.ArrayList;
import java.util.Random;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.sharpcastle33.did.generator.PostProcessor;

public class CaveTickTask extends BukkitRunnable{

	private Random rand;
	private String tempo;
	ArrayList<Player> members;
	int index;


	public CaveTickTask(ArrayList<Player> members, String tempo) {
		this.tempo = tempo;
		this.members = members;
		index = 0;
		rand = new Random();
	}

	public Player randomPlayer() {
		int index = rand.nextInt(this.members.size());
		return this.members.get(index);
	}

	public void spawnMobs(EntityType mob, int count, int attempts, int minRad, int maxRad) {
		for(int i = 0; i < count; i++) {
			spawn(mob,attempts,minRad,maxRad);
		}
	}

	public void spawn(EntityType mob, int attempts, int minRad, int maxRad) {
		Player p = randomPlayer();
		int diff = maxRad - minRad;
		assert diff >= 0;

		for(int i = 0; i < attempts; i++) {
			int tx = rand.nextInt(diff) + minRad;
			int ty = rand.nextInt(diff/2) + minRad/2;
			int tz = rand.nextInt(diff) + minRad;

			int flip = rand.nextBoolean() ? -1 : 1;

			tx = tx*flip;
			tz = tz*flip;

			int x = p.getLocation().getBlockX() + tx;
			int y = p.getLocation().getBlockY() + ty;
			int z = p.getLocation().getBlockZ() + tz;

			World world = p.getWorld();
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(world), CaveStyle.DEFAULT, rand)) {
				BlockVector3 pos = BlockVector3.at(x, y, z);
				if(ctx.getBlock(pos).getBlockType() == BlockTypes.AIR) {
					if(ctx.getBlock(BlockVector3.at(x, y + 1, z)).getBlockType() == BlockTypes.AIR) {
						BlockVector3 fin = PostProcessor.getFloor(ctx, pos, 16);
						world.spawnEntity(BukkitAdapter.adapt(world, fin), mob);
						return;
					}
				}
			}
		}
	}

	@Override
	public void run() {
		if(index >= tempo.length()) { index = 0; }
		char current = tempo.charAt(index);
		index++;
		if(current == '.') {
			float f = rand.nextFloat();
			f = f/2;

			if(rand.nextInt(5) == 1)
				for(Player p : this.members) {
					p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1, 1+f);
				}
		}

		for(Player p : this.members) {
			p.sendMessage("Event: " + current);
		}

		if(current == 'x' || current == 'X') {
			float f = rand.nextFloat();
			f = f/2;
			for(Player p : this.members) {
				p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1, 1+f);
			}
			if(current == 'X') { spawnMobs(EntityType.ZOMBIE, 2, 12, 8, 24);}
			spawnMobs(EntityType.ZOMBIE, 2, 12, 8, 16);
		}

		if(current == '!') {
			float f = rand.nextFloat();
			f = f/2;
			for(Player p : this.members) {
				p.playSound(p.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 1+f);
				spawnMobs(EntityType.CAVE_SPIDER, 6, 12, 8, 16);
			}
		}


	}

}
