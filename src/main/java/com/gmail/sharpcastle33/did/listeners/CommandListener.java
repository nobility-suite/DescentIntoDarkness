package com.gmail.sharpcastle33.did.listeners;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.sharpcastle33.did.main;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.gmail.sharpcastle33.dungeonmaster.DungeonMaster;

import net.md_5.bungee.api.ChatColor;

public class CommandListener implements CommandExecutor{

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player p = (Player) sender;
		
		if(args.length == 1) {
			if(args[0].equals("generate")) {
				p.sendMessage(ChatColor.DARK_RED + "Generating...");
				CaveGenerator.generateBlank(p.getWorld());
				p.sendMessage(ChatColor.GREEN + "Done!");

			}
			
			if(args[0].equals("start")) {
				DungeonMaster dungeonMaster = main.plugin.getDungeonMaster();
				dungeonMaster.start(p);
			}
		}else if(args.length == 2) {
			if(args[0].equals("generate")) {
				if(args[1].equals("cave")) {
					p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
					String s = CaveGenerator.generateCave(p.getWorld(), 9);
					p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s);
				}
			}
		}else if(args.length == 3) {
			if(args[0].equals("generate")) {
				if(args[1].equals("cave")) {
					p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
					int size = Integer.parseInt(args[2]);
					String s = CaveGenerator.generateCave(p.getWorld(), size);
					p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s);
				}
			}
		}
		
		return true;
	}
}
