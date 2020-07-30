package com.gmail.sharpcastle33.instancing;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class InstanceManager {

	private final ArrayList<Instance> instances = new ArrayList<>();
	private int nextInstanceId;

	public CompletableFuture<Instance> createInstance(CaveStyle style) {
		int id = nextInstanceId++;
		String name = getWorldName(id);
		World world = createFlatWorld(id, style, World.Environment.NORMAL);
		if (world == null) {
			return Util.completeExceptionally(new RuntimeException("Could not create world"));
		}
		return DescentIntoDarkness.plugin.supplyAsync(() -> {
			Random rand = new Random();
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(world), style, rand)) {
				CaveGenerator.generateCave(ctx, Vector3.at(0, 210, 0), rand.nextInt(5) + 7);
			} catch (WorldEditException e) {
				DescentIntoDarkness.plugin.runSyncLater(() -> DescentIntoDarkness.multiverseCore.getMVWorldManager().deleteWorld(name));
				throw new RuntimeException("Could not generate cave", e);
			}
			Instance instance = new Instance(id, world, new Location(world, 0, 210, 0));
			DescentIntoDarkness.plugin.runSyncNow(() -> instances.add(instance));
			return instance;
		});
	}

	public void deleteInstance(Instance instance) {
		// TODO: teleport players out

		instances.remove(instance);
		DescentIntoDarkness.multiverseCore.getMVWorldManager().deleteWorld(getWorldName(instance.getId()));
	}

	public void destroy() {
		while (!instances.isEmpty()) {
			deleteInstance(instances.get(0));
		}
	}

	public boolean inInstance(Player p) {
		for(Instance i : instances) {
			ArrayList<Player> members = i.getMembers();
			if(members.contains(p)) {
				return true;
			}
		}
		return false;
	}

	public boolean teleportPlayerTo(Player p, @Nullable Instance instance) {
		Instance existingInstance = getInstance(p);
		if (existingInstance == instance) {
			return true;
		}
		if (existingInstance != null) {
			existingInstance.removeMember(p);
		}

		if (instance == null) {
			// TODO
		} else {
			Location start = instance.getStart();
			String destStr = String.format("e:%s:%f,%f,%f", getWorldName(instance.getId()), start.getX(), start.getY(), start.getZ());
			MVDestination dest = DescentIntoDarkness.multiverseCore.getDestFactory().getDestination(destStr);
			if (DescentIntoDarkness.multiverseCore.getSafeTTeleporter().teleport(Bukkit.getServer().getConsoleSender(), p, dest) != TeleportResult.SUCCESS) {
				return false;
			}
			instance.addMember(p);
		}

		return true;
	}

	@Nullable
	public Instance getInstance(Player p) {
		for(Instance i : instances) {
			ArrayList<Player> members = i.getMembers();
			if(members.contains(p)) {
				return i;
			}
		}
		return null;
	}

	private String getWorldName(int id) {
		return "did_cave_" + id;
	}

	private World createFlatWorld(int id, CaveStyle style, World.Environment environment) {
		MVWorldManager worldManager = DescentIntoDarkness.multiverseCore.getMVWorldManager();
		String worldName = getWorldName(id);
		String generator = "DescentIntoDarkness:full_" + style.getBaseBlock().getAsString();
		if (!worldManager.addWorld(worldName, environment, "0", WorldType.FLAT, Boolean.FALSE, generator, false)) {
			return null;
		}
		return worldManager.getMVWorld(worldName).getCBWorld();
	}

}
