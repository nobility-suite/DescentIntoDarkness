package com.gmail.sharpcastle33.did.listeners;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.fastasyncworldedit.core.internal.exception.FaweException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
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
	
	private final HashMap<UUID,Long> playerSeeds;
	private CaveGenContext currentCaveGen;
	
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
			return true;
		}

		switch (args[0]) {
			case "delete":
				delete(p, args);
				break;
			case "generate":
				generate(p, args);
				break;
			case "cancel":
				cancel(p, args);
				break;
			case "debug":
				debug(p,args);
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
				DescentIntoDarkness.instance.reload();
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
	
	private void debug(Player p, String[] args) {
		// TODO Auto-generated method stub
		CaveTrackerManager ctm = DescentIntoDarkness.instance.getCaveTrackerManager();
		for(CaveTracker ct : ctm.getCaves()) {
			if(ct.getPlayers().contains(p.getUniqueId())) {
				p.sendMessage(ChatColor.GOLD + "You are currently connected to cave: " + ct.getId() + ", " + ct.getStyle().getName());
			}
		}
		
	}

	private void select(Player p, String[] args) {
		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();
		
		long seed;
		if(playerSeeds.containsKey(p.getUniqueId())) {
			 seed = playerSeeds.get(p.getUniqueId());
		}else {
			p.sendMessage(ChatColor.RED + "No options to select. Type /descent");
			return;
		}
		
		Random rand = new Random(seed);
		
		int selected = -1;
		try {
			selected = Integer.parseInt(args[1]);
		}catch(Exception e) {
			p.sendMessage(ChatColor.DARK_RED + "Selected cave must be a number!");
			return;
		}
		
		int options = 5;
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		
		for(int i = 0; i < options; i++) {
			indexes.add(rand.nextInt(DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName().size()));
		}
		NavigableMap<String,CaveStyle> nm = DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName();
		ArrayList<CaveStyle> styles = new ArrayList<>(nm.values());
		
		CaveStyle style =  styles.get(indexes.get(selected));

		int id = -1;
		
		CaveCreationHandle cch = caveTrackerManager.createCave(style);
		p.sendMessage(ChatColor.GREEN + "Creating cave... Cave ID: " + ChatColor.WHITE + cch.caveId);
		
		World world = p.getWorld();
		Villager v = (Villager) world.spawnEntity(p.getLocation(), EntityType.VILLAGER);
		v.setAdult();
		v.setAI(false);
		v.setPersistent(true);
		v.setCustomName(ChatColor.BLUE + "Cave: " + ChatColor.WHITE + style.getName() + " [" + cch.caveId + "]");
		
		//TODO despawn villager
		
		//TODO implement portal
		
		//TODO subtitle message
		
		//TODO prevent villager trading
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

	private void delete(Player p, String[] args) {
		if (args.length == 1) {
			return;
		}
		OptionalInt caveId = parseInt(p, args[1]);
		if (!caveId.isPresent()) return;

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();

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

	private void cancel(Player p, String[] args) {
		if (currentCaveGen != null) {
			currentCaveGen.cancel();
		}
	}

	public static void join(Player p, String[] args) {
		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();
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
				DescentIntoDarkness.instance.runSyncLater(() -> {
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

		CaveTrackerManager caveTrackerManager = DescentIntoDarkness.instance.getCaveTrackerManager();
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

	private void generateBlank(Player p, String[] args) {
		BlockStateHolder<?> base = args.length <= 2 ? Util.requireDefaultState(BlockTypes.STONE) : ConfigUtil.parseBlock(args[2]);
		OptionalInt radius = args.length <= 3 ? OptionalInt.of(200) : parseInt(p, args[3]);
		if (!radius.isPresent()) return;
		OptionalInt yRadius = args.length <= 4 ? OptionalInt.of(120) : parseInt(p, args[4]);
		if (!yRadius.isPresent()) return;

		p.sendMessage(ChatColor.DARK_RED + "Generating...");

		Location pos = p.getLocation();
		DescentIntoDarkness.instance.runAsync(() -> {
			try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(p.getWorld()))) {
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

	private void generateCave(Player p, String[] args) {
		String styleName = args.length <= 2 ? "default" : args[2];
		OptionalInt size = args.length <= 3 ? OptionalInt.of(7) : parseInt(p, args[3]);
		if (!size.isPresent()) return;
		OptionalLong seed = args.length <= 4 ? OptionalLong.of(new Random().nextLong()) : parseLong(p, args[4]);
		if (!seed.isPresent()) return;
		boolean debug = args.length > 5 && Boolean.parseBoolean(args[5]);

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
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(p.getWorld()), style, new Random(seed.getAsLong())).setDebug(debug)) {
				currentCaveGen = ctx;
				return CaveGenerator.generateCave(ctx, BukkitAdapter.asVector(p.getLocation()), size.getAsInt());
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
						if (args[1].equals("cave")) {
							if (args.length == 3) {
								return StringUtil.copyPartialMatches(
										args[2],
										DescentIntoDarkness.instance.getCaveStyles().getCaveStylesByName().entrySet().stream().filter(entry -> !entry.getValue().isAbstract()).map(Map.Entry::getKey).collect(Collectors.toList()),
										new ArrayList<>()
								);
							} else if (args.length == 6) {
								return StringUtil.copyPartialMatches(args[5], Arrays.asList("false", "true"), new ArrayList<>());
							}
						} else if (args[1].equals("blank")) {
							if (args.length == 3) {
								return StringUtil.copyPartialMatches(args[2], Arrays.stream(Material.values()).map(material -> material.getKey().getKey()).collect(Collectors.toList()), new ArrayList<>());
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
