package com.gmail.sharpcastle33.dungeonmaster;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.gmail.sharpcastle33.did.Main;

import net.md_5.bungee.api.ChatColor;

public class DungeonMaster {
	
	
	// '.' = pause
	// 'x' = encounter
	// 'X' = large encounter
	// '!" = special encounter (2% base)
	public String tempo(Random rand, int len) {
		String ret = "";
		
		int lucky = 0; //Tempers randomness
		
		for(int i = 0; i < len; i++) {
			int rng = rand.nextInt(100-lucky)+lucky;
			
			if(rng <= 5) {
				ret += "...";
				lucky +=10;
			}else if(rng <= 10) {
				ret += "..";
				lucky+=5;
			}else if(rng <= 30) {
				ret += ".";
				lucky+=3;
			}else if(rng <= 70) {
				ret += ".";
				lucky+=1;
			}else if(rng <= 80) {
				ret+="x";
				lucky-=2;
			}else if(rng <= 92) {
				ret+="X";
				lucky-=10;
			}else if(rng <= 98) {
				ret+="!";
				lucky-=25;
			}else {
				ret+="x";
			}		
		}
		return ret;
	}

	public void start(Random rand, Player p) {

		String tempo = tempo(rand, 40);
		p.sendMessage(ChatColor.GREEN + "Tempo: " + tempo);
		long period = 20 * 7; //15 seconds timer
		
		ArrayList<Player> temp = new ArrayList<Player>();
		
		for(Player pl : Bukkit.getServer().getOnlinePlayers()) {
			temp.add(pl);
		}
		
	    new CaveTickTask(temp,tempo).runTaskTimer(Main.plugin, 20, period);
	}
	


}
