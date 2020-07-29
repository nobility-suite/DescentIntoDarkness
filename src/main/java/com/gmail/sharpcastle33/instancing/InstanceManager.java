package com.gmail.sharpcastle33.instancing;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.boydti.fawe.util.TaskManager;
import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.gmail.sharpcastle33.did.generator.CaveGenerator;
import com.onarandombox.MultiverseCore.api.MVDestination;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.enums.TeleportResult;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
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
		return createFlatWorld(id, style, World.Environment.NORMAL).thenApply(v -> {
			MultiverseWorld mvWorld = TaskManager.IMP.sync(() -> DescentIntoDarkness.multiverseCore.getMVWorldManager().getMVWorld(name));
			Random rand = new Random();
			try (CaveGenContext ctx = CaveGenContext.create(BukkitAdapter.adapt(mvWorld.getCBWorld()), style, rand)) {
				CaveGenerator.generateCave(ctx, Vector3.at(0, 210, 0), rand.nextInt(5) + 7);
			} catch (WorldEditException e) {
				TaskManager.IMP.sync(() -> DescentIntoDarkness.multiverseCore.getMVWorldManager().deleteWorld(name));
				throw new RuntimeException("Could not generate cave", e);
			}
			Instance instance = new Instance(id, mvWorld.getCBWorld(), new Location(mvWorld.getCBWorld(), 0, 210, 0));
			TaskManager.IMP.sync(() -> instances.add(instance));
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

	private CompletableFuture<Void> createFlatWorld(int id, CaveStyle style, World.Environment environment) {
		String flatName = String.format("did_flat_%s_%s", environment, style.getBaseBlock().getAsString());
		MVWorldManager worldManager = DescentIntoDarkness.multiverseCore.getMVWorldManager();
		if (!worldManager.isMVWorld(flatName)) {
			if (!worldManager.addWorld(flatName, environment, "0", WorldType.FLAT, Boolean.FALSE, "3;1*minecraft:bedrock;2;", false)) {
				return Util.completeExceptionally(new RuntimeException("Could not create world"));
			}
			World world = worldManager.getMVWorld(flatName).getCBWorld();
			return DescentIntoDarkness.plugin.runAsync(() -> {
				try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
					session.setBlocks((Region)new CuboidRegion(BlockVector3.at(-250, 0, -250), BlockVector3.at(250, 255, 250)), style.getBaseBlock());
				} catch (MaxChangedBlocksException e) {
					throw new RuntimeException(e);
				}
			}).thenRun(() -> {
				if (!TaskManager.IMP.sync(() -> worldManager.cloneWorld(flatName, getWorldName(id)))) {
					throw new RuntimeException("Could not clone world");
				}
			});
		}
		if (!worldManager.cloneWorld(flatName, getWorldName(id))) {
			return Util.completeExceptionally(new RuntimeException("Could not clone world"));
		} else {
			return CompletableFuture.completedFuture(null);
		}
	}

}
