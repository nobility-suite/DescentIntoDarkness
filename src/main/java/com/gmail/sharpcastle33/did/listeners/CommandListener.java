package com.gmail.sharpcastle33.did.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.gmail.sharpcastle33.did.instancing.CaveTrackerManager;
import com.gmail.sharpcastle33.did.instancing.CaveTrackerManager.CaveCreationHandle;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.md_5.bungee.api.ChatColor;

public class CommandListener implements TabExecutor {
	
	private HashMap<UUID,Long> playerSeeds;
	
	public CommandListener() {
		this.playerSeeds = new HashMap<UUID,Long>();
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + "Sender must be a player");
			return true;
		}

		Player p = (Player) sender;
		
		if(args.length == 0) {
			caveMenu(p,args);
		}

		switch (args[0]) {
			case "delete":
				delete(p, args);
				break;
			case "generate":
				generate(p, args);
				break;
			case "join":
				join(p, args);
				break;
			case "leave":
				leave(p, args);
				break;
			case "list":
				list(p);
				break;
			case "reload":
				DescentIntoDarkness.plugin.reload();
				p.sendMessage(ChatColor.GREEN + "Reloaded DID config");
				break;
			case "reroll":
				Random rand = new Random();
				long seed = rand.nextLong();
				playerSeeds.put(p.getUniqueId(), seed);
				break;
			case "select":
				select(p,args);
				break;
		}

		return true;
	}
	
	private void select(Player p, String[] args) {
		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.plugin.getCaveTrackerManager();
		
		long seed;
		if(playerSeeds.containsKey(p.getUniqueId())) {
			 seed = playerSeeds.get(p.getUniqueId());
		}else {
			p.sendMessage(ChatColor.RED + "No options to select. Type /descent");
			return;
		}
		
		Random rand = new Random(seed);
		
		
		int selected = Integer.parseInt(args[1]);
		
		int options = 5;
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		
		for(int i = 0; i < options; i++) {
			indexes.add(rand.nextInt(DescentIntoDarkness.plugin.getCaveStyles().size()));
		}
		NavigableMap<String,CaveStyle> nm = DescentIntoDarkness.plugin.getCaveStyles();
		ArrayList<CaveStyle> styles = new ArrayList<>(nm.values());
		
		CaveStyle style =  styles.get(indexes.get(selected));

		int id = -1;
		
		CaveCreationHandle cch = caveTrackerManager.createCave(style);
		p.sendMessage(ChatColor.GREEN + "Creating cave... Cave ID: " + ChatColor.WHITE + cch.caveId);
	}
	
	private void caveMenu(Player p, String[] args) {
		Random rand = new Random();
		long seed;
		
		if(!playerSeeds.containsKey(p.getUniqueId())) {
			seed = rand.nextLong();
			playerSeeds.put(p.getUniqueId(), seed);
		}else seed = playerSeeds.get(p.getUniqueId());
		
		int options = 5;
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		
		for(int i = 0; i < options; i++) {
			indexes.add(rand.nextInt(DescentIntoDarkness.plugin.getCaveStyles().size()));
		}
		
		p.sendMessage(ChatColor.BLUE + "[]===============[] " + ChatColor.BOLD + "Cave Selector" + ChatColor.RESET + ChatColor.BLUE + " []===============[] ");
		p.sendMessage(ChatColor.GRAY +  "" + ChatColor.ITALIC + "Alpha Version 1.0");
		p.sendMessage(ChatColor.GRAY +  "" + ChatColor.ITALIC + "This menu will be replaced with a UI in the future...");
		p.sendMessage(ChatColor.GRAY +  "" + ChatColor.ITALIC + "You can refresh these options with /descent reroll");

		NavigableMap<String,CaveStyle> nm = DescentIntoDarkness.plugin.getCaveStyles();
		
		for(int i = 0; i < indexes.size(); i++) {

			ArrayList<CaveStyle> styles = new ArrayList<>(nm.values());
			CaveStyle cs = styles.get(indexes.get(i));
			
			p.sendMessage(ChatColor.BLUE + "Cave: " + ChatColor.WHITE + "[" + i + "], " + cs.getName());
			
		}
	}

	private void delete(Player p, String[] args) {
		if (args.length == 1) {
			return;
		}
		OptionalInt caveId = parseInt(p, args[1]);
		if (!caveId.isPresent()) return;

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.plugin.getCaveTrackerManager();

		CaveTracker cave = caveTrackerManager.getCaveById(caveId.getAsInt());
		if (cave == null) {
			p.sendMessage(ChatColor.RED + "Cave " + caveId.getAsInt() + " not found");
			return;
		}

		caveTrackerManager.deleteCave(cave);
		p.sendMessage(ChatColor.GREEN + "Deleted cave " + caveId.getAsInt());
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

	private void join(Player p, String[] args) {
		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.plugin.getCaveTrackerManager();
		CompletableFuture<CaveTracker> cave;

		if (args.length > 1) {
			OptionalInt caveId = parseInt(p, args[1]);
			if (!caveId.isPresent()) return;

			CaveTracker c = caveTrackerManager.getCaveById(caveId.getAsInt());
			if (c == null) {
				p.sendMessage(ChatColor.RED + "Cave " + caveId.getAsInt() + " not found");
				return;
			}
			cave = CompletableFuture.completedFuture(c);
		} else {
			cave = caveTrackerManager.findFreeCave().caveFuture;
		}

		Player target;
		if (args.length > 2) {
			target = Bukkit.getPlayer(args[2]);
			if (target == null) {
				p.sendMessage(ChatColor.RED + "Player not found");
				return;
			}
		} else {
			target = p;
		}

		cave.whenComplete((c, throwable) -> {
			if (throwable != null) {
				p.sendMessage(ChatColor.RED + "Could not find free cave");
			} else {
				DescentIntoDarkness.plugin.runSyncLater(() -> {
					if (caveTrackerManager.teleportPlayerTo(target, c)) {
						p.sendMessage(ChatColor.GREEN + "Teleported player successfully");
					} else {
						p.sendMessage(ChatColor.RED + "Failed to teleport player");
					}
				});
			}
		});
	}

	private void leave(Player p, String[] args) {
		Player target;
		if (args.length > 1) {
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				p.sendMessage(ChatColor.RED + "Player not found");
				return;
			}
		} else {
			target = p;
		}

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.plugin.getCaveTrackerManager();
		if (!caveTrackerManager.isInCave(target)) {
			p.sendMessage(ChatColor.RED + "Player is not in a cave");
			return;
		}

		if (caveTrackerManager.teleportPlayerTo(target, null)) {
			p.sendMessage(ChatColor.GREEN + "Teleported player successfully");
		} else {
			p.sendMessage(ChatColor.RED + "Failed to teleport player");
		}
	}

	private void list(Player p) {
		List<CaveTracker> caves = DescentIntoDarkness.plugin.getCaveTrackerManager().getCaves();
		if (caves.isEmpty()) {
			p.sendMessage(ChatColor.RED + "0 active caves");
		} else {
			p.sendMessage(ChatColor.GREEN + String.format("%d active caves", caves.size()));
			for (CaveTracker cave : caves) {
				p.sendMessage(ChatColor.YELLOW + String.format("%d: %s", cave.getId(), cave.getStyle().getName()));
			}
		}

	}

	private void generateBlank(Player p, String[] args) {
		BlockStateHolder<?> base = args.length <= 2 ? Util.requireDefaultState(BlockTypes.STONE) : ConfigUtil.parseBlock(args[2]);
		OptionalInt radius = args.length <= 3 ? OptionalInt.of(200) : parseInt(p, args[3]);
		if (!radius.isPresent()) return;
		OptionalInt yRadius = args.length <= 4 ? OptionalInt.of(120) : parseInt(p, args[4]);
		if (!yRadius.isPresent()) return;

		p.sendMessage(ChatColor.DARK_RED + "Generating...");

		Location pos = p.getLocation();
		DescentIntoDarkness.plugin.runAsync(() -> {
			try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(p.getWorld()), -1)) {
				CaveGenerator.generateBlank(session, base, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), radius.getAsInt(), yRadius.getAsInt());
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
		OptionalInt size = args.length <= 3 ? OptionalInt.of(7) : parseInt(p, args[3]);
		if (!size.isPresent()) return;
		OptionalLong seed = args.length <= 4 ? OptionalLong.of(new Random().nextLong()) : parseLong(p, args[4]);
		if (!seed.isPresent()) return;
		boolean debug = args.length > 5 && Boolean.parseBoolean(args[5]);

		CaveStyle style = DescentIntoDarkness.plugin.getCaveStyles().get(styleName);
		if (style == null) {
			p.sendMessage(ChatColor.DARK_RED + "No such cave style " + styleName);
			return;
		}
		if (style.isAbstract()) {
			p.sendMessage(ChatColor.DARK_RED + "Cannot create abstract cave style " + styleName);
			return;
		}

		p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
		DescentIntoDarkness.plugin.supplyAsync(() -> {
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(p.getWorld()), style, new Random(seed.getAsLong())).setDebug(debug)) {
				return CaveGenerator.generateCave(ctx, BukkitAdapter.asVector(p.getLocation()), size.getAsInt());
			}
		}).whenComplete((s, throwable) -> {
			if (throwable != null) {
				DescentIntoDarkness.plugin.runSyncLater(() -> p.sendMessage(ChatColor.DARK_RED + "Failed"));
				Bukkit.getLogger().log(Level.SEVERE, "Failed to generate cave", throwable);
			} else {
				DescentIntoDarkness.plugin.runSyncLater(() -> {
					p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s);
					p.sendMessage(ChatColor.GREEN + "Cave seed: " + seed.getAsLong());
					Bukkit.getLogger().log(Level.INFO, "Generated cave with seed " + seed.getAsLong());
				});
			}
		});
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			return Collections.emptyList();
		} else if (args.length == 1) {
			return StringUtil.copyPartialMatches(args[0], Arrays.asList("delete", "generate", "join", "leave", "list", "reload"), new ArrayList<>());
		} else {
			switch (args[0]) {
				case "generate":
					if (args.length == 2) {
						return StringUtil.copyPartialMatches(args[1], Arrays.asList("cave", "blank"), new ArrayList<>());
					} else {
						if (args[1].equals("cave")) {
							if (args.length == 3) {
								return StringUtil.copyPartialMatches(
										args[2],
										DescentIntoDarkness.plugin.getCaveStyles().entrySet().stream().filter(entry -> !entry.getValue().isAbstract()).map(Map.Entry::getKey).collect(Collectors.toList()),
										new ArrayList<>()
								);
							} else if (args.length == 6) {
								return StringUtil.copyPartialMatches(args[5], Arrays.asList("false", "true"), new ArrayList<>());
							}
						} else if (args[1].equals("blank")) {
							if (args.length == 3) {
								return StringUtil.copyPartialMatches(args[2], DescentIntoDarkness.getAllMaterials().stream().map(material -> material.getKey().getKey()).collect(Collectors.toList()), new ArrayList<>());
							}
						}
					}
					break;
				case "join":
					if (args.length == 3) {
						return StringUtil.copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), new ArrayList<>());
					}
					break;
				case "leave":
					if (args.length == 2) {
						return StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), new ArrayList<>());
					}
					break;
			}
		}

		return Collections.emptyList();
	}

	private static OptionalInt parseInt(Player p, String arg) {
		try {
			return OptionalInt.of(Integer.parseInt(arg));
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Invalid integer: " + arg);
			return OptionalInt.empty();
		}
	}

	private static OptionalLong parseLong(Player p, String arg) {
		try {
			return OptionalLong.of(Long.parseLong(arg));
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Invalid long: " + arg);
			return OptionalLong.empty();
		}
	}
}
