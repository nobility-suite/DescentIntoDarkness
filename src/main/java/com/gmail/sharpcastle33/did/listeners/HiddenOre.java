package com.gmail.sharpcastle33.did.listeners;

import com.github.devotedmc.hiddenore.events.HiddenOreGenerateEvent;
import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HiddenOre implements Listener {
	private static final Map<Location, HiddenOreData> hiddenOreData = new HashMap<>();
	private static final Map<Integer, Location> hiddenOreLocationsByCaveId = new HashMap<>();

	private static final Map<Material, DyeColor> SHULKER_BOX_COLORS = new HashMap<>();
	static {
		// i fucking hate spigot
		SHULKER_BOX_COLORS.put(Material.WHITE_SHULKER_BOX, DyeColor.WHITE);
		SHULKER_BOX_COLORS.put(Material.ORANGE_SHULKER_BOX, DyeColor.ORANGE);
		SHULKER_BOX_COLORS.put(Material.MAGENTA_SHULKER_BOX, DyeColor.MAGENTA);
		SHULKER_BOX_COLORS.put(Material.LIGHT_BLUE_SHULKER_BOX, DyeColor.LIGHT_BLUE);
		SHULKER_BOX_COLORS.put(Material.YELLOW_SHULKER_BOX, DyeColor.YELLOW);
		SHULKER_BOX_COLORS.put(Material.LIME_SHULKER_BOX, DyeColor.LIME);
		SHULKER_BOX_COLORS.put(Material.PINK_SHULKER_BOX, DyeColor.PINK);
		SHULKER_BOX_COLORS.put(Material.GRAY_SHULKER_BOX, DyeColor.GRAY);
		SHULKER_BOX_COLORS.put(Material.LIGHT_GRAY_SHULKER_BOX, DyeColor.LIGHT_GRAY);
		SHULKER_BOX_COLORS.put(Material.CYAN_SHULKER_BOX, DyeColor.CYAN);
		SHULKER_BOX_COLORS.put(Material.PURPLE_SHULKER_BOX, DyeColor.PURPLE);
		SHULKER_BOX_COLORS.put(Material.BLUE_SHULKER_BOX, DyeColor.BLUE);
		SHULKER_BOX_COLORS.put(Material.BROWN_SHULKER_BOX, DyeColor.BROWN);
		SHULKER_BOX_COLORS.put(Material.GREEN_SHULKER_BOX, DyeColor.GREEN);
		SHULKER_BOX_COLORS.put(Material.RED_SHULKER_BOX, DyeColor.RED);
		SHULKER_BOX_COLORS.put(Material.BLACK_SHULKER_BOX, DyeColor.BLACK);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (SHULKER_BOX_COLORS.containsKey(event.getBlock().getType())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onHiddenOreGenerated(HiddenOreGenerateEvent event) {
		Material material = event.getTransform();
		DyeColor color = SHULKER_BOX_COLORS.get(material);
		if (color == null) {
			// not a shulker box
			return;
		}

		Location pos = event.getBlock().getLocation();
		World world = pos.getWorld();
		if (world == null) {
			return;
		}

		HiddenOreData data = new HiddenOreData(null, 0, color);
		hiddenOreData.put(new Location(world, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()), data);
		saveHiddenOreData();
	}

	@EventHandler
	public void onOpenShulkerBox(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null || !(clickedBlock.getState() instanceof ShulkerBox)) {
			return;
		}

		HiddenOreData hiddenOreData = getHiddenOreData(clickedBlock.getLocation());
		if (hiddenOreData == null) {
			return;
		}

		Integer caveId = hiddenOreData.caveId;
		if (caveId == null) {
			CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().findFreeCave(hiddenOreData.color);
			if (cave == null) {
				event.getPlayer().sendMessage(ChatColor.DARK_RED +
						"No " + hiddenOreData.color.name().toLowerCase(Locale.ROOT) +
						" caves are currently available. Please try again later.");
			} else {
				CommandListener.setConfirmAction(
						event.getPlayer(),
						600,
						() -> joinCave(event.getPlayer(), clickedBlock.getLocation()));
				BaseComponent message = new TextComponent(ChatColor.DARK_GREEN + "You have found a " + hiddenOreData.color.name().toLowerCase(Locale.ROOT) + " cave! ");
				TextComponent button = new TextComponent(ChatColor.DARK_GREEN + "Click to enter.");
				button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/did confirm"));
				button.setUnderlined(true);
				message.addExtra(button);
				event.getPlayer().spigot().sendMessage(message);
			}
		} else {
			CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveById(caveId);
			assert cave != null;
			long remainingTime = cave.getJoinTime() + cave.getStyle().getLifetime() - cave.getWorld().getFullTime();
			if (remainingTime <= 0) {
				return;
			}
			String remainingTimeString = Util.formatTime(remainingTime);
			CommandListener.setConfirmAction(
					event.getPlayer(),
					600,
					() -> joinCave(event.getPlayer(), clickedBlock.getLocation())
			);
			BaseComponent message = new TextComponent(ChatColor.DARK_GREEN + "You have found a " + hiddenOreData.color.name().toLowerCase(Locale.ROOT) + " cave! " +
					ChatColor.DARK_GREEN + "It will close in " + remainingTimeString + ". ");
			TextComponent button = new TextComponent(ChatColor.DARK_GREEN + "Click to enter.");
			button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/did confirm"));
			button.setUnderlined(true);
			message.addExtra(button);
			event.getPlayer().spigot().sendMessage(message);
		}
	}

	private static void joinCave(Player player, Location oreLocation) {
		HiddenOreData data = getHiddenOreData(oreLocation);
		if (data == null) {
			player.sendMessage(ChatColor.DARK_RED + "This cave has expired.");
			return;
		}

		CaveTracker cave;

		if (data.caveId == null) {
			cave = DescentIntoDarkness.instance.getCaveTrackerManager().findFreeCave(data.color);
			if (cave == null) {
				player.sendMessage(ChatColor.DARK_RED +
						"No " + data.color.name().toLowerCase(Locale.ROOT) +
						" caves are currently available. Please try again later.");
				return;
			}
			data.caveId = cave.getId();
			hiddenOreLocationsByCaveId.put(data.caveId, oreLocation);
		} else {
			cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveById(data.caveId);
			assert cave != null;
		}

		DescentIntoDarkness.instance.getCaveTrackerManager().teleportPlayerTo(player, cave);
		data.caveJoinTime = cave.getJoinTime();
		saveHiddenOreData();
	}

	@Nullable
	public static HiddenOreData getHiddenOreData(Location location) {
		World world = location.getWorld();
		if (world == null) {
			return null;
		}
		location = new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
		HiddenOreData data = hiddenOreData.get(location);
		if (data == null) {
			return null;
		}
		if (data.caveId != null) {
			CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveById(data.caveId);
			if (cave == null || cave.getJoinTime() != data.caveJoinTime) {
				hiddenOreData.remove(location);
				return null;
			}

			if (cave.getJoinTime() + cave.getStyle().getLifetime() < cave.getWorld().getFullTime()) {
				hiddenOreData.remove(location);
				return null;
			}
		}

		BlockState state = location.getBlock().getState();
		if (!(state instanceof ShulkerBox)) {
			hiddenOreData.remove(location);
			return null;
		}
		ShulkerBox shulkerBox = (ShulkerBox) state;
		if (shulkerBox.getColor() != data.color) {
			hiddenOreData.remove(location);
			return null;
		}

		return data;
	}

	public static void onDeleteCave(int caveId) {
		Location location = hiddenOreLocationsByCaveId.remove(caveId);
		if (location == null) {
			return;
		}
		HiddenOreData data = hiddenOreData.get(location);
		if (data == null) {
			return;
		}

		location.getBlock().setType(Material.AIR);
		saveHiddenOreData();
	}

	public static void saveHiddenOreData() {
		File runtimeFolder = new File(DescentIntoDarkness.instance.getDataFolder(), "runtime");
		if (!runtimeFolder.exists() && !runtimeFolder.mkdirs()) {
			Bukkit.getLogger().warning("Could not create runtime folder");
			return;
		}

		FileConfiguration config = new YamlConfiguration();
		List<Map<?, ?>> datas = new ArrayList<>(hiddenOreData.size());
		hiddenOreData.forEach((location, hiddenOreData) -> {
			Map<String, Object> data = new HashMap<>();
			data.put("world", Objects.requireNonNull(location.getWorld()).getName());
			data.put("x", location.getBlockX());
			data.put("y", location.getBlockY());
			data.put("z", location.getBlockZ());
			if (hiddenOreData.caveId != null) {
				data.put("caveId", hiddenOreData.caveId);
			}
			data.put("caveJoinTime", hiddenOreData.caveJoinTime);
			data.put("color", hiddenOreData.color.name());
			datas.add(data);
		});
		config.set("hiddenOre", datas);

		Util.saveSafely(new File(runtimeFolder, "hiddenOre.yml"), config::save);
	}

	public static void loadHiddenOreData() {
		File runtimeFolder = new File(DescentIntoDarkness.instance.getDataFolder(), "runtime");
		File hiddenOreFile = new File(runtimeFolder, "hiddenOre.yml");
		if (!hiddenOreFile.exists()) {
			return;
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(hiddenOreFile);
		List<Map<?, ?>> datas = config.getMapList("hiddenOre");
		datas.forEach(data -> {
			String worldName = Util.tryCast(data.get("world"), String.class);
			if (worldName == null) {
				return;
			}
			World world = Bukkit.getWorld(worldName);
			if (world == null) {
				return;
			}
			Integer x = Util.tryCastInt(data.get("x"));
			Integer y = Util.tryCastInt(data.get("y"));
			Integer z = Util.tryCastInt(data.get("z"));
			if (x == null || y == null || z == null) {
				return;
			}
			Location location = new Location(world, x, y, z);
			@Nullable Integer caveId = Util.tryCastInt(data.get("caveId"));
			@Nullable Long caveJoinTime = Util.tryCastLong(data.get("caveJoinTime"));
			String colorName = Util.tryCast(data.get("color"), String.class);
			if (colorName == null) {
				return;
			}
			DyeColor color = ConfigUtil.tryParseEnum(DyeColor.class, colorName);
			if (color == null) {
				return;
			}
			if (caveId != null) {
				hiddenOreLocationsByCaveId.put(caveId, location);
			}
			hiddenOreData.put(location, new HiddenOreData(caveId, caveJoinTime == null ? 0 : caveJoinTime, color));
		});
	}

	public static class HiddenOreData {
		@Nullable
		private Integer caveId;
		private long caveJoinTime;
		private final DyeColor color;

		private HiddenOreData(@Nullable Integer caveId, long caveJoinTime, DyeColor color) {
			this.caveId = caveId;
			this.caveJoinTime = caveJoinTime;
			this.color = color;
		}
	}
}
