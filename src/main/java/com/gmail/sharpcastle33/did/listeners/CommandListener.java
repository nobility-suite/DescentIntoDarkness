package com.gmail.sharpcastle33.did.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.fastasyncworldedit.core.internal.exception.FaweException;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import net.md_5.bungee.api.ChatColor;

public class CommandListener implements TabExecutor {
	
	private final HashMap<UUID,Long> playerSeeds = new HashMap<>();
	private CaveGenContext currentCaveGen;
	private static final WeakHashMap<Player, ConfirmAction> confirmActions = new WeakHashMap<>();

	private static class ConfirmAction {
		private final Runnable action;
		private final long expiry;

		public ConfirmAction(Runnable action, long expiry) {
			this.action = action;
			this.expiry = expiry;
		}
	}
	public static void setConfirmAction(Player p, long timeout, Runnable r) {
		confirmActions.put(p, new ConfirmAction(r, Bukkit.getWorlds().get(0).getFullTime() + timeout));
	}
	
	private static Player requirePlayer(CommandSender sender) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.DARK_RED + "Sender must be a player");
			return null;
		}
		return (Player) sender;
	}

	private static boolean checkElevatedPermission(CommandSender sender) {
		if (!hasElevatedPermission(sender)) {
			sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command");
			return false;
		}
		return true;
	}

	private static boolean hasElevatedPermission(CommandSender sender) {
		return sender.hasPermission("did.command");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player p;

		if (args.length == 0) {
			if (!checkElevatedPermission(sender)) {
				return true;
			}
			if ((p = requirePlayer(sender)) != null) {
				caveMenu(p,args);
			}
			return true;
		}

		// non-elevated-permission commands
		switch (args[0]) {
			case "confirm": {
				p = requirePlayer(sender);
				if (p == null) {
					return true;
				}
				confirm(p);
				return true;
			}
			case "leave": {
				leave(sender, args);
				return true;
			}
		}

		if (!checkElevatedPermission(sender)) {
			return true;
		}

		switch (args[0]) {
			case "delete":
				delete(sender, args);
				break;
			case "generate":
				generate(sender, args);
				break;
			case "cancel":
				cancel(sender, args);
				break;
			case "debug":
				if ((p = requirePlayer(sender)) != null) {
					debug(p,args);
				}
				break;
			case "join":
				join(sender, args);
				break;
			case "list":
				list(sender);
				break;
			case "reload":
				DescentIntoDarkness.instance.reload();
				sender.sendMessage(ChatColor.GREEN + "Reloaded DID config");
				break;
			case "reroll":
				if ((p = requirePlayer(sender)) != null) {
					Random rand = new Random();
					long seed = rand.nextLong();
					playerSeeds.put(p.getUniqueId(), seed);
				}
				break;
		}

		return true;
	}

	private static void confirm(Player p) {
		ConfirmAction action = confirmActions.remove(p);
		if (action == null) {
			return;
		}
		if (action.expiry < Bukkit.getWorlds().get(0).getFullTime()) {
			p.sendMessage(ChatColor.DARK_RED + "Confirmation timed out");
			return;
		}
		action.action.run();
	}
	
	private void debug(Player p, String[] args) {
		CaveTrackerManager ctm = DescentIntoDarkness.instance.getCaveTrackerManager();
		for(CaveTracker ct : ctm.getCaves()) {
			if(ct.getPlayers().contains(p.getUniqueId())) {
				p.sendMessage(ChatColor.GOLD + "You are currently connected to cave: " + ct.getId() + ", " + ct.getStyle().getName());
			}
		}
		
	}
	
	private void caveMenu(Player p, String[] args) {
		Random rand;
		long seed;
		
		if(!playerSeeds.containsKey(p.getUniqueId())) {
			rand = new Random();
			seed = rand.nextLong();
			playerSeeds.put(p.getUniqueId(), seed);
		}else {
			seed = playerSeeds.get(p.getUniqueId());
			rand = new Random(seed);
		}
		
		int options = 5;
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		
		for(int i = 0; i < options; i++) {
			indexes.add(rand.nextInt(DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName().size()));
		}
		
		p.sendMessage(ChatColor.BLUE + "[]===============[] " + ChatColor.BOLD + "Cave Selector" + ChatColor.RESET + ChatColor.BLUE + " []===============[] ");
		p.sendMessage(ChatColor.GRAY +  "" + ChatColor.ITALIC + "Alpha Version 1.0");
		p.sendMessage(ChatColor.GRAY +  "" + ChatColor.ITALIC + "This menu will be replaced with a UI in the future...");
		p.sendMessage(ChatColor.GRAY +  "" + ChatColor.ITALIC + "You can refresh these options with /descent reroll");

		NavigableMap<String,CaveStyle> nm = DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName();
		
		for(int i = 0; i < indexes.size(); i++) {

			ArrayList<CaveStyle> styles = new ArrayList<>(nm.values());
			CaveStyle cs = styles.get(indexes.get(i));
			
			p.sendMessage(ChatColor.BLUE + "Cave: " + ChatColor.WHITE + "[" + i + "], " + cs.getName());
			
		}
	}

	private void delete(CommandSender p, String[] args) {
		if (args.length == 1) {
			return;
		}
		OptionalInt caveId = parseInt(p, args[1]);
		if (caveId.isEmpty()) return;

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();

		CaveTracker cave = caveTrackerManager.getCaveById(caveId.getAsInt());
		if (cave == null) {
			p.sendMessage(ChatColor.RED + "Cave " + caveId.getAsInt() + " not found");
			return;
		}

		caveTrackerManager.deleteCave(cave);
		p.sendMessage(ChatColor.GREEN + "Deleted cave " + caveId.getAsInt());
	}

	private void generate(CommandSender p, String[] args) {
		if (args.length == 1) {
			return;
		}

		Location pos;

		if (args.length >= 5) {
			boolean xRelative = args[2].startsWith("~");
			OptionalInt xOpt = xRelative ? (args[2].equals("~") ? OptionalInt.of(0) : parseInt(p, args[2].substring(1))) : parseInt(p, args[2]);
			if (xOpt.isEmpty()) return;
			boolean yRelative = args[3].startsWith("~");
			OptionalInt yOpt = yRelative ? (args[3].equals("~") ? OptionalInt.of(0) : parseInt(p, args[3].substring(1))) : parseInt(p, args[3]);
			if (yOpt.isEmpty()) return;
			boolean zRelative = args[4].startsWith("~");
			OptionalInt zOpt = zRelative ? (args[4].equals("~") ? OptionalInt.of(0) : parseInt(p, args[4].substring(1))) : parseInt(p, args[4]);
			if (zOpt.isEmpty()) return;
			int x = xOpt.getAsInt(), y = yOpt.getAsInt(), z = zOpt.getAsInt();
			if (xRelative || yRelative || zRelative) {
				Player player = requirePlayer(p);
				if (player == null) return;
				if (xRelative) x += player.getLocation().getBlockX();
				if (yRelative) y += player.getLocation().getBlockY();
				if (zRelative) z += player.getLocation().getBlockZ();
			}
			pos = new Location(p instanceof Player ? ((Player) p).getWorld() : Bukkit.getWorlds().get(0), x, y, z);
		} else {
			Player player = requirePlayer(p);
			if (player == null) return;
			pos = player.getLocation();
		}

		if (args[1].equals("cave")) {
			generateCave(p, pos, args);
		} else if (args[1].equals("blank")) {
			generateBlank(p, pos, args);
		}
	}

	private void cancel(CommandSender p, String[] args) {
		if (currentCaveGen != null) {
			currentCaveGen.cancel();
		} else {
			p.sendMessage(ChatColor.DARK_RED + "No cave generation in progress");
		}
	}

	public static void join(CommandSender p, String[] args) {
		if (args.length == 1) {
			return;
		}

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();

		CaveTracker c;
		try {
			DyeColor color = DyeColor.valueOf(args[1].toUpperCase(Locale.ROOT));
			c = caveTrackerManager.findFreeCave(color);
			if (c == null) {
				p.sendMessage(ChatColor.RED + "No free caves of that color");
				return;
			}
		} catch (IllegalArgumentException e) {
			OptionalInt caveId = parseInt(p, args[1]);
			if (caveId.isEmpty()) return;

			c = caveTrackerManager.getCaveById(caveId.getAsInt());
			if (c == null) {
				p.sendMessage(ChatColor.RED + "Cave " + caveId.getAsInt() + " not found");
				return;
			}
		}

		Player target;
		if (args.length > 2) {
			target = Bukkit.getPlayer(args[2]);
			if (target == null) {
				p.sendMessage(ChatColor.RED + "Player not found");
				return;
			}
		} else {
			target = requirePlayer(p);
			if (target == null) return;
		}

		if (caveTrackerManager.teleportPlayerTo(target, c)) {
			p.sendMessage(ChatColor.GREEN + "Teleported player successfully");
		} else {
			p.sendMessage(ChatColor.RED + "Failed to teleport player");
		}
	}

	private void leave(CommandSender p, String[] args) {
		Player target;
		if (args.length > 1) {
			if (!checkElevatedPermission(p)) return;
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				p.sendMessage(ChatColor.RED + "Player not found");
				return;
			}
		} else {
			target = requirePlayer(p);
			if (target == null) return;
		}

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();
		CaveTracker cave = caveTrackerManager.getCaveForPlayer(target);
		if (cave == null) {
			p.sendMessage(ChatColor.RED + "Player is not in a cave");
			return;
		}
		if (!hasElevatedPermission(p)) {
			OptionalLong lastLeaveTime = cave.getLastLeaveTime(target.getUniqueId());
			if (lastLeaveTime.isPresent()) {
				long time = cave.getWorld().getFullTime() - lastLeaveTime.getAsLong();
				int maxTime = 15 * 60 * 20;
				if (time < maxTime) {
					p.sendMessage(ChatColor.RED + "You can only use this command every 15 minutes. You have " + Util.formatTime(maxTime - time) + " remaining.");
					return;
				}
			}
		}

		if (caveTrackerManager.teleportPlayerTo(target, null)) {
			p.sendMessage(ChatColor.GREEN + "Teleported player successfully");
		} else {
			p.sendMessage(ChatColor.RED + "Failed to teleport player");
		}
	}

	private void list(CommandSender p) {
		List<CaveTracker> caves = DescentIntoDarkness.instance.getCaveTrackerManager().getCaves();
		if (caves.isEmpty()) {
			p.sendMessage(ChatColor.RED + "0 active caves");
		} else {
			p.sendMessage(ChatColor.GREEN + String.format("%d active caves", caves.size()));
			for (CaveTracker cave : caves) {
				p.sendMessage(ChatColor.YELLOW + String.format("%d: %s", cave.getId(), cave.getStyle().getName()));
			}
		}

	}

	private void generateBlank(CommandSender p, Location pos, String[] args) {
		BlockStateHolder<?> base = args.length <= 5 ? Util.requireDefaultState(BlockTypes.STONE) : ConfigUtil.parseBlock(args[5]);
		OptionalInt radius = args.length <= 6 ? OptionalInt.of(200) : parseInt(p, args[6]);
		if (radius.isEmpty()) return;
		OptionalInt yRadius = args.length <= 7 ? OptionalInt.of(120) : parseInt(p, args[7]);
		if (yRadius.isEmpty()) return;

		p.sendMessage(ChatColor.DARK_RED + "Generating...");

		DescentIntoDarkness.instance.runAsync(() -> {
			try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(pos.getWorld()))) {
				CaveGenerator.generateBlank(session, base, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), radius.getAsInt(), yRadius.getAsInt());
			}
		}).whenComplete((v, throwable) -> {
			if (throwable != null) {
				DescentIntoDarkness.instance.runSyncLater(() -> p.sendMessage(ChatColor.DARK_RED + "Failed"));
				Bukkit.getLogger().log(Level.SEVERE, "Failed to generate blank", throwable);
			} else {
				DescentIntoDarkness.instance.runSyncLater(() -> p.sendMessage(ChatColor.GREEN + "Done!"));
			}
		});
	}

	private void generateCave(CommandSender p, Location pos, String[] args) {
		String styleName = args.length <= 5 ? "default" : args[5];
		OptionalInt size = args.length <= 6 ? OptionalInt.of(7) : parseInt(p, args[6]);
		if (size.isEmpty()) return;
		OptionalLong seed = args.length <= 7 ? OptionalLong.of(new Random().nextLong()) : parseLong(p, args[7]);
		if (seed.isEmpty()) return;
		boolean debug = args.length > 8 && Boolean.parseBoolean(args[8]);

		if (currentCaveGen != null) {
			p.sendMessage(ChatColor.DARK_RED + "Already generating cave");
			return;
		}

		CaveStyle style = DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName().get(styleName);
		if (style == null) {
			p.sendMessage(ChatColor.DARK_RED + "No such cave style " + styleName);
			return;
		}
		if (style.isAbstract()) {
			p.sendMessage(ChatColor.DARK_RED + "Cannot create abstract cave style " + styleName);
			return;
		}

		p.sendMessage(ChatColor.DARK_RED + "Generating Cave...");
		
		for(CaveTracker t : DescentIntoDarkness.instance.getCaveTrackerManager().getCaves()) {
			Bukkit.getServer().getLogger().info("CaveTracker found, ID: " + t.getId() + " " + t.getJoinTime());
		}
		DescentIntoDarkness.instance.supplyAsync(() -> {
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(pos.getWorld()), style, seed.getAsLong()).setDebug(debug)) {
				currentCaveGen = ctx;
				return CaveGenerator.generateCave(ctx, BukkitAdapter.asVector(pos), size.getAsInt());
			}
		}).whenComplete((s, throwable) -> {
			currentCaveGen = null;
			if (throwable != null) {
				if (throwable instanceof FaweException && ((FaweException) throwable).getType() == FaweException.Type.MANUAL) {
					p.sendMessage(ChatColor.GREEN + "Canceled cave generation");
					Bukkit.getLogger().log(Level.INFO, "Canceled cave generation");
				} else {
					DescentIntoDarkness.instance.runSyncLater(() -> p.sendMessage(ChatColor.DARK_RED + "Failed"));
					Bukkit.getLogger().log(Level.SEVERE, "Failed to generate cave", throwable);
				}
			} else {
				DescentIntoDarkness.instance.runSyncLater(() -> {
					p.sendMessage(ChatColor.GREEN + "Done! Cave layout: " + s);
					p.sendMessage(ChatColor.GREEN + "Cave seed: " + seed.getAsLong());
					Bukkit.getLogger().log(Level.INFO, "Generated cave with seed " + seed.getAsLong());
				});
			}
		});
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!hasElevatedPermission(sender)) {
			if (args.length == 1) {
				return StringUtil.copyPartialMatches(args[0], List.of("leave"), new ArrayList<>());
			}
			return Collections.emptyList();
		}
		if (args.length == 0) {
			return Collections.emptyList();
		} else if (args.length == 1) {
			return StringUtil.copyPartialMatches(args[0], Arrays.asList("delete", "generate", "cancel", "join", "leave", "list", "reload"), new ArrayList<>());
		} else {
			switch (args[0]) {
				case "generate":
					if (args.length == 2) {
						return StringUtil.copyPartialMatches(args[1], Arrays.asList("cave", "blank"), new ArrayList<>());
					} else {
						if (args.length < 6) {
							return args[args.length - 1].isEmpty() ? Collections.singletonList("~") : Collections.emptyList();
						}
						if (args[1].equals("cave")) {
							if (args.length == 6) {
								return StringUtil.copyPartialMatches(
										args[5],
										DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName().entrySet().stream().filter(entry -> !entry.getValue().isAbstract()).map(Map.Entry::getKey).collect(Collectors.toList()),
										new ArrayList<>()
								);
							} else if (args.length == 9) {
								return StringUtil.copyPartialMatches(args[8], Arrays.asList("false", "true"), new ArrayList<>());
							}
						} else if (args[1].equals("blank")) {
							if (args.length == 6) {
								return StringUtil.copyPartialMatches(args[5], Arrays.stream(Material.values()).map(material -> material.getKey().getKey()).collect(Collectors.toList()), new ArrayList<>());
							}
						}
					}
					break;
				case "join":
					if (args.length == 2) {
						return StringUtil.copyPartialMatches(
								args[1],
								DescentIntoDarkness.instance.getCaveStyles().getGroups().keySet().stream()
									.filter(it -> !DescentIntoDarkness.instance.getCaveStyles().getGroups().get(it).getCaveWeights().isEmpty())
									.map(it -> it.name().toLowerCase(Locale.ROOT))
									.collect(Collectors.toList()),
								new ArrayList<>()
						);
					}
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

	private static OptionalInt parseInt(CommandSender p, String arg) {
		try {
			return OptionalInt.of(Integer.parseInt(arg));
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Invalid integer: " + arg);
			return OptionalInt.empty();
		}
	}

	private static OptionalLong parseLong(CommandSender p, String arg) {
		try {
			return OptionalLong.of(Long.parseLong(arg));
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Invalid long: " + arg);
			return OptionalLong.empty();
		}
	}
}
