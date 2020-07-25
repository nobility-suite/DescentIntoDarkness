package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.gmail.sharpcastle33.did.Main;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.gmail.sharpcastle33.dungeonmaster.DungeonMaster;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandListener implements TabExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + "Sender must be a player");
			return true;
		}

		Player p = (Player) sender;

		switch (args[0]) {
			case "generate":
				generate(p, args);
				break;
			case "start":
				DungeonMaster dungeonMaster = Main.plugin.getDungeonMaster();
				dungeonMaster.start(new Random(), p);
				break;
			case "reload":
				Main.plugin.reload();
				p.sendMessage(ChatColor.GREEN + "Reloaded DID config");
				break;
		}

		return true;
	}

	private void generate(Player p, String[] args) {
		if (args.length == 1) {
			return;
		}

		if (args[1].equals("cave")) {
			generateCave(p, args);
		} else if (args[1].equals("blank")) {
			generateBlank(p, args);
		}
	}

	private void generateBlank(Player p, String[] args) {
		BlockStateHolder<?> base = args.length <= 2 ? Util.requireDefaultState(BlockTypes.STONE) : ConfigUtil.parseBlock(args[2]);
		int radius = args.length <= 3 ? 200 : Integer.parseInt(args[3]);
		int yRadius = args.length <= 4 ? 120 : Integer.parseInt(args[4]);

		p.sendMessage(ChatColor.DARK_RED + "Generating...");

		Location pos = p.getLocation();
		try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(p.getWorld()), -1)) {
			CaveGenerator.generateBlank(session, base, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), radius, yRadius);
		} catch (WorldEditException e) {
			p.sendMessage(ChatColor.DARK_RED + "Failed");
			Bukkit.getLogger().log(Level.SEVERE, "Failed to generate blank", e);
			return;
		}
		p.sendMessage(ChatColor.GREEN + "Done!");
	}

	private void generateCave(Player p, String[] args) {
		String styleName = args.length <= 2 ? "default" : args[2];
		int size = args.length <= 3 ? 9 : Integer.parseInt(args[3]);
		long seed = args.length <= 4 ? new Random().nextLong() : Long.parseLong(args[4]);

		CaveStyle style = Main.plugin.getCaveStyles().get(styleName);
		if (style == null) {
			p.sendMessage(ChatColor.DARK_RED + "No such cave style " + styleName);
			return;
		}

		p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
		String s;
		try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(p.getWorld()), style, new Random(seed))) {
			s = CaveGenerator.generateCave(ctx, BukkitAdapter.asVector(p.getLocation()), size);
		} catch (WorldEditException e) {
			p.sendMessage(ChatColor.DARK_RED + "Failed");
			Bukkit.getLogger().log(Level.SEVERE, "Failed to generate cave", e);
			return;
		}
		p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			return Collections.emptyList();
		} else if (args.length == 1) {
			return StringUtil.copyPartialMatches(args[0], Arrays.asList("generate", "start", "reload"), new ArrayList<>());
		} else {
			if (args[0].equals("generate")) {
				if (args.length == 2) {
					return StringUtil.copyPartialMatches(args[1], Arrays.asList("cave", "blank"), new ArrayList<>());
				} else {
					if (args[1].equals("cave")) {
						if (args.length == 3) {
							return StringUtil.copyPartialMatches(args[2], Main.plugin.getCaveStyles().keySet(), new ArrayList<>());
						}
					} else if (args[1].equals("blank")) {
						if (args.length == 3) {
							return StringUtil.copyPartialMatches(args[2], Main.getAllMaterials().stream().map(material -> material.getKey().getKey()).collect(Collectors.toList()), new ArrayList<>());
						}
					}
				}
			}
		}

		return Collections.emptyList();
	}
}
