package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.google.common.collect.Iterators;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.gmail.sharpcastle33.did.generator.CaveGenerator;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
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
			case "teleport":
				teleport(p, args);
				break;
			case "reload":
				DescentIntoDarkness.plugin.reload();
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

	private void teleport(Player p, String[] args) {
		NavigableMap<String, CaveStyle> caveStyles = DescentIntoDarkness.plugin.getCaveStyles();
		CaveStyle style = args.length < 2 ? Iterators.get(caveStyles.values().iterator(), new Random().nextInt(caveStyles.size())) : caveStyles.get(args[1]);
		if (style == null) {
			p.sendMessage(ChatColor.DARK_RED + "No such cave style " + args[1]);
			return;
		}
		p.sendMessage(ChatColor.DARK_RED + "Creating instance...");
		DescentIntoDarkness.plugin.getCaveTrackerManager().createCave(style).whenComplete((instance, throwable) -> {
			if (throwable != null) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed to create instance", throwable);
				DescentIntoDarkness.plugin.runSyncLater(() -> p.sendMessage(ChatColor.DARK_RED + "Failed to create instance"));
			} else {
				DescentIntoDarkness.plugin.runSyncLater(() -> {
					if (!DescentIntoDarkness.plugin.getCaveTrackerManager().teleportPlayerTo(p, instance)) {
						p.sendMessage(ChatColor.DARK_RED + "Failed to teleport you to the cave");
					} else {
						p.sendMessage(ChatColor.GREEN + "Done!");
					}
				});
			}
		});
	}

	private void generateBlank(Player p, String[] args) {
		BlockStateHolder<?> base = args.length <= 2 ? Util.requireDefaultState(BlockTypes.STONE) : ConfigUtil.parseBlock(args[2]);
		int radius = args.length <= 3 ? 200 : Integer.parseInt(args[3]);
		int yRadius = args.length <= 4 ? 120 : Integer.parseInt(args[4]);

		p.sendMessage(ChatColor.DARK_RED + "Generating...");

		Location pos = p.getLocation();
		DescentIntoDarkness.plugin.runAsync(() -> {
			try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(p.getWorld()), -1)) {
				CaveGenerator.generateBlank(session, base, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), radius, yRadius);
			}
		}).whenComplete((v, throwable) -> {
			if (throwable != null) {
				DescentIntoDarkness.plugin.runSyncLater(() -> p.sendMessage(ChatColor.DARK_RED + "Failed"));
				Bukkit.getLogger().log(Level.SEVERE, "Failed to generate blank", throwable);
			} else {
				DescentIntoDarkness.plugin.runSyncLater(() -> p.sendMessage(ChatColor.GREEN + "Done!"));
			}
		});
	}

	private void generateCave(Player p, String[] args) {
		String styleName = args.length <= 2 ? "default" : args[2];
		int size = args.length <= 3 ? 9 : Integer.parseInt(args[3]);
		long seed = args.length <= 4 ? new Random().nextLong() : Long.parseLong(args[4]);
		boolean debug = args.length > 5 && Boolean.parseBoolean(args[5]);

		CaveStyle style = DescentIntoDarkness.plugin.getCaveStyles().get(styleName);
		if (style == null) {
			p.sendMessage(ChatColor.DARK_RED + "No such cave style " + styleName);
			return;
		}

		p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
		DescentIntoDarkness.plugin.supplyAsync(() -> {
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(p.getWorld()), style, new Random(seed)).setDebug(debug)) {
				return CaveGenerator.generateCave(ctx, BukkitAdapter.asVector(p.getLocation()), size);
			}
		}).whenComplete((s, throwable) -> {
			if (throwable != null) {
				DescentIntoDarkness.plugin.runSyncLater(() -> p.sendMessage(ChatColor.DARK_RED + "Failed"));
				Bukkit.getLogger().log(Level.SEVERE, "Failed to generate cave", throwable);
			} else {
				DescentIntoDarkness.plugin.runSyncLater(() -> p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s));
			}
		});
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			return Collections.emptyList();
		} else if (args.length == 1) {
			return StringUtil.copyPartialMatches(args[0], Arrays.asList("generate", "teleport", "reload"), new ArrayList<>());
		} else {
			if (args[0].equals("generate")) {
				if (args.length == 2) {
					return StringUtil.copyPartialMatches(args[1], Arrays.asList("cave", "blank"), new ArrayList<>());
				} else {
					if (args[1].equals("cave")) {
						if (args.length == 3) {
							return StringUtil.copyPartialMatches(args[2], DescentIntoDarkness.plugin.getCaveStyles().keySet(), new ArrayList<>());
						} else if (args.length == 6) {
							return StringUtil.copyPartialMatches(args[5], Arrays.asList("false", "true"), new ArrayList<>());
						}
					} else if (args[1].equals("blank")) {
						if (args.length == 3) {
							return StringUtil.copyPartialMatches(args[2], DescentIntoDarkness.getAllMaterials().stream().map(material -> material.getKey().getKey()).collect(Collectors.toList()), new ArrayList<>());
						}
					}
				}
			} else if (args[0].equals("teleport")) {
				if (args.length == 2) {
					return StringUtil.copyPartialMatches(args[1], DescentIntoDarkness.plugin.getCaveStyles().keySet(), new ArrayList<>());
				}
			}
		}

		return Collections.emptyList();
	}
}
