package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gmail.sharpcastle33.did.Main;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.gmail.sharpcastle33.dungeonmaster.DungeonMaster;

import net.md_5.bungee.api.ChatColor;

public class CommandListener implements CommandExecutor{

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Player p = (Player) sender;

		if (args[0].equals("generate")) {
			generate(p, args);
		} else if (args[0].equals("start")) {
			DungeonMaster dungeonMaster = Main.plugin.getDungeonMaster();
			dungeonMaster.start(p);
		} else if (args[0].equals("reload")) {
			Main.plugin.reload();
			p.sendMessage(ChatColor.GREEN + "Reloaded DID config");
		}
		
		return true;
	}

	private void generate(Player p, String[] args) {
		if (args.length == 1) {
			p.sendMessage(ChatColor.DARK_RED + "Generating...");
			CaveGenerator.generateBlank(p.getWorld());
			p.sendMessage(ChatColor.GREEN + "Done!");
		} else if (args[1].equals("cave")) {
			generateCave(p, args);
		}
	}

	private void generateCave(Player p, String[] args) {
		String styleName = args.length <= 2 ? "default" : args[2];
		int size = args.length <= 3 ? 9 : Integer.parseInt(args[3]);

		CaveStyle style = Main.plugin.getCaveStyles().get(styleName);
		if (style == null) {
			p.sendMessage(ChatColor.DARK_RED + "No such cave style " + styleName);
			return;
		}

		p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
		String s = CaveGenerator.generateCave(p.getWorld(), size, style);
		p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s);
	}
}
