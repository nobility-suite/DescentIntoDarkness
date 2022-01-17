package com.gmail.sharpcastle33.did.listeners;

import com.github.devotedmc.hiddenore.events.HiddenOreGenerateEvent;
import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HiddenOre implements Listener {
	private static final Map<Location, HiddenOreData> hiddenOreData = new HashMap<>();

	@EventHandler
	public void onHiddenOreGenerated(HiddenOreGenerateEvent event) {
		Block block = event.getBlock();
		if (!(block.getBlockData() instanceof ShulkerBox)) {
			return;
		}

		Location pos = block.getLocation();
		World world = pos.getWorld();
		if (world == null) {
			return;
		}

		ShulkerBox shulkerBox = (ShulkerBox) block.getBlockData();
		DyeColor color = shulkerBox.getColor();
		HiddenOreData data = new HiddenOreData(null, color);
		hiddenOreData.put(new Location(world, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()), data);
		saveHiddenOreData();
	}

	@EventHandler
	public void onOpenShulkerBox(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null || !(clickedBlock.getBlockData() instanceof ShulkerBox)) {
			return;
		}

		HiddenOreData hiddenOreData = getHiddenOreData(clickedBlock.getLocation());
		if (hiddenOreData == null) {
			return;
		}


	}

	public static void checkExpiries() {

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
		if (data.expiry != null && world.getFullTime() > data.expiry) {
			hiddenOreData.remove(location);
			return null;
		}

		BlockData blockData = location.getBlock().getBlockData();
		if (!(blockData instanceof ShulkerBox)) {
			hiddenOreData.remove(location);
			return null;
		}
		ShulkerBox shulkerBox = (ShulkerBox) blockData;
		if (shulkerBox.getColor() != data.color) {
			hiddenOreData.remove(location);
			return null;
		}

		return data;
	}

	public static void saveHiddenOreData() {
		File runtimeFolder = new File(DescentIntoDarkness.instance.getDataFolder(), "runtime");
		if (!runtimeFolder.mkdirs()) {
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
			if (hiddenOreData.expiry != null) {
				data.put("expiry", hiddenOreData.expiry);
			}
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
			@Nullable Long expiry = Util.tryCastLong(data.get("expiry"));
			String colorName = Util.tryCast(data.get("color"), String.class);
			if (colorName == null) {
				return;
			}
			DyeColor color = ConfigUtil.tryParseEnum(DyeColor.class, colorName);
			if (color == null) {
				return;
			}
			hiddenOreData.put(location, new HiddenOreData(expiry, color));
		});

		checkExpiries();
	}

	public static class HiddenOreData {
		@Nullable
		private Long expiry;
		private final DyeColor color;

		private HiddenOreData(@Nullable Long expiry, DyeColor color) {
			this.expiry = expiry;
			this.color = color;
		}
	}
}
