package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MobSpawnManager implements Runnable, Listener {
	private final Random rand = new Random();
	private boolean spawningMob = false;
	private MobSpawnEntry currentSpawnEntry;
	private final List<MobInstance> allMobs = new ArrayList<>();

	private void spawnMobs(CaveTracker cave) {
		if (cave.getPlayers().isEmpty()) {
			return;
		}

		if (cave.getSpawnCooldown() > 0) {
			cave.setSpawnCooldown(cave.getSpawnCooldown() - 1);
			return;
		}

		// Prevent lack of mob spawning due to idleness
		if (rand.nextFloat() < cave.getStyle().getNaturalPollutionIncrease()) {
			MobSpawnEntry mobType = getRandomSpawnEntry(cave);
			if (mobType != null) {
				Player victim = getRandomPlayer(cave, mobType, Integer.MIN_VALUE, player -> 1);
				if (victim != null) {
					cave.addPlayerMobPollution(victim.getUniqueId(), mobType, victim.isSprinting() ? cave.getStyle().getSprintingPenalty() : 1);
				}
			}
		}

		for (int i = 0; i < cave.getStyle().getSpawnAttemptsPerTick(); i++) {
			if (spawnMob(cave)) {
				break;
			}
		}
	}

	private boolean spawnMob(CaveTracker cave) {
		// pick random type of mob to spawn and check if the cave has enough total pollution to spawn it
		MobSpawnEntry spawnEntry = getRandomSpawnEntry(cave);
		if (spawnEntry == null) {
			return false;
		}
		CaveTracker.MobEntry mobEntry = cave.getMobEntry(spawnEntry);
		if (!mobEntry.isSpawningPack() && mobEntry.getPackSpawnThreshold() > mobEntry.getTotalPollution()) {
			return false;
		}
		boolean spawningPack = mobEntry.getTotalPollution() >= spawnEntry.getSingleMobCost();
		mobEntry.setSpawningPack(spawningPack);
		if (!spawningPack) {
			mobEntry.setPackSpawnThreshold(spawnEntry.getMinPackCost() + rand.nextInt(spawnEntry.getMaxPackCost() - spawnEntry.getMinPackCost() + 1));
		}

		// try to spawn that mob next to a player with enough pollution to afford it
		Player chosenPlayer = getRandomPlayer(cave, spawnEntry, spawnEntry.getSingleMobCost(), player -> cave.getMobEntry(spawnEntry).getPlayerPollution(player));
		if (chosenPlayer == null) {
			chosenPlayer = getRandomPlayer(cave, spawnEntry, 1, player -> cave.getMobEntry(spawnEntry).getPlayerPollution(player));
			if (chosenPlayer == null) {
				return false;
			}
		}


		// Pick a random distance between minDistance and maxDistance, then a random point on the sphere with that radius.
		// Makes distances uniformly likely, as opposed to
		Vector spawnLocation = new Vector(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian())
				.normalize()
				.multiply(spawnEntry.getMinDistance() + rand.nextDouble() * (spawnEntry.getMaxDistance() - spawnEntry.getMinDistance()))
				.add(chosenPlayer.getLocation().toVector());
		spawnLocation.setY(Math.floor(spawnLocation.getY()));
		if (spawnEntry.isCenteredSpawn()) {
			spawnLocation.setX(Math.floor(spawnLocation.getX()) + 0.5);
			spawnLocation.setZ(Math.floor(spawnLocation.getZ()) + 0.5);
		}

		// quick exit for the blocks the mob will definitely intersect, and respect custom hitbox
		if (spawnEntry.getXSize() > 0 && spawnEntry.getYSize() > 0 && spawnEntry.getZSize() > 0) {
			if (!canSpawnMob(cave.getWorld(), spawnEntry,
					spawnLocation.getX() - spawnEntry.getXSize() / 2, spawnLocation.getY(), spawnLocation.getZ() - spawnEntry.getZSize() / 2,
					spawnLocation.getX() + spawnEntry.getXSize() / 2, spawnLocation.getY() + spawnEntry.getYSize(), spawnLocation.getZ() + spawnEntry.getZSize() / 2)) {
				return false;
			}
		} else {
			if (!canSpawnMob(cave.getWorld(), spawnEntry,
					spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(),
					spawnLocation.getX() + 0.001, spawnLocation.getY() + 2, spawnLocation.getZ() + 0.001)) {
				return false;
			}
		}
		Block blockBelow = cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY() - 1, spawnLocation.getBlockZ());
		if (spawnEntry.getCanSpawnOn() == null
				? !blockBelow.getType().isSolid()
				: spawnEntry.getCanSpawnOn().stream().noneMatch(it -> it.equalsFuzzy(blockData2State(blockBelow.getBlockData())))) {
			return false;
		}

		// check if we're within the radius of other players, we don't want to spawn inside them
		if (cave.getPlayers().stream()
				.map(Bukkit::getOfflinePlayer)
				.filter(OfflinePlayer::isOnline)
				.map(OfflinePlayer::getPlayer)
				.filter(player -> player != null && player.getWorld() == cave.getWorld())
				.anyMatch(player -> player.getLocation().toVector().distanceSquared(spawnLocation) < spawnEntry.getMinDistance() * spawnEntry.getMinDistance())) {
			return false;
		}

		if (!doSpawn(cave, spawnEntry, chosenPlayer.getUniqueId(), spawnLocation)) {
			return false;
		}

		// deduct pollution
		cave.addPlayerMobPollution(chosenPlayer.getUniqueId(), spawnEntry, -spawnEntry.getSingleMobCost());
		cave.setSpawnCooldown(cave.getSpawnCooldown() + spawnEntry.getCooldown());

		return true;
	}

	private boolean doSpawn(CaveTracker cave, MobSpawnEntry spawnEntry, UUID spawnedPlayer, Vector spawnLocation) {
		boolean isMythicMob = MythicMobs.inst().getMobManager().getMythicMob(spawnEntry.getMob()) != null;
		Location loc = spawnLocation.toLocation(cave.getWorld());

		// spawn mob and check its hitbox doesn't intersect anything
		// the event cancels the mob spawning in this case
		Entity mob;
		if (isMythicMob) {
			spawningMob = true;
			currentSpawnEntry = spawnEntry;
			ActiveMob activeMob;
			try {
				activeMob = MythicMobs.inst().getMobManager().spawnMob(spawnEntry.getMob(), loc);
			} finally {
				spawningMob = false;
				currentSpawnEntry = null;
			}
			if (activeMob == null) {
				return false;
			}
			mob = BukkitAdapter.adapt(activeMob.getEntity());
			allMobs.add(new MythicMobInstance(cave, spawnEntry, activeMob, spawnedPlayer));
		} else {
			com.sk89q.worldedit.world.entity.EntityType entityType =
					com.sk89q.worldedit.world.entity.EntityType.REGISTRY.get(spawnEntry.getMob().toLowerCase(Locale.ROOT));
			if (entityType == null) {
				Bukkit.getLogger().log(Level.SEVERE, "Could not spawn mob: " + spawnEntry.getMob());
				return false;
			}
			EntityType bukkitEntity = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(entityType);
			mob = cave.getWorld().spawnEntity(loc, bukkitEntity);
			if (!canSpawnMob(cave.getWorld(), spawnEntry, mob)) {
				mob.remove();
				return false;
			}
			if (mob instanceof LivingEntity) {
				((LivingEntity) mob).setRemoveWhenFarAway(false);
			}
			allMobs.add(new VanillaMobInstance(cave, spawnEntry, mob, spawnedPlayer));
		}

		mob.setRotation(rand.nextFloat() * 360, 0);

		return true;
	}

	@Nullable
	private Player getRandomPlayer(CaveTracker cave, MobSpawnEntry spawnEntry, int minPollution, ToIntFunction<UUID> weightFunction) {
		CaveTracker.MobEntry mobEntry = cave.getMobEntry(spawnEntry);
		int totalOnlineWeight = cave.getPlayers().stream()
				.filter(this::isPlayerOnline)
				.filter(player -> cave.getMobEntry(spawnEntry).getPlayerPollution(player) >= minPollution)
				.mapToInt(weightFunction)
				.sum();

		if (totalOnlineWeight <= 0) {
			List<UUID> players = cave.getPlayers().stream().filter(this::isPlayerOnline).filter(player -> mobEntry.getPlayerPollution(player) >= minPollution).collect(Collectors.toList());
			if (players.isEmpty()) {
				return null;
			}
			return Bukkit.getPlayer(players.get(rand.nextInt(players.size())));
		}

		int index = rand.nextInt(totalOnlineWeight);
		Player chosenPlayer = null;
		for (UUID player : cave.getPlayers()) {
			if (!isPlayerOnline(player)) {
				continue;
			}
			int pollution = mobEntry.getPlayerPollution(player);
			if (pollution < minPollution) {
				continue;
			}
			index -= weightFunction.applyAsInt(player);
			if (index < 0) {
				chosenPlayer = Bukkit.getPlayer(player);
				break;
			}
		}
		return chosenPlayer;
	}

	@Nullable
	public MobSpawnEntry getRandomSpawnEntry(CaveTracker cave) {
		List<MobSpawnEntry> spawnEntries = cave.getStyle().getSpawnEntries();
		if (spawnEntries.isEmpty()) {
			return null;
		}

		int totalWeight = spawnEntries.stream().mapToInt(MobSpawnEntry::getWeight).sum();
		int index = rand.nextInt(totalWeight);
		for (MobSpawnEntry entry : spawnEntries) {
			index -= entry.getWeight();
			if (index <= 0) {
				return entry;
			}
		}
		return null;
	}

	private boolean isPlayerOnline(UUID player) {
		OfflinePlayer p = Bukkit.getOfflinePlayer(player);
		//noinspection ConstantConditions - Bukkit derp
		return p != null && p.isOnline();
	}

	@Override
	public void run() {
		if (Bukkit.getWorlds().get(0).getFullTime() % 20 == 0) {
			Iterator<MobInstance> allMobsItr = allMobs.iterator();
			while (allMobsItr.hasNext()) {
				MobInstance mob = allMobsItr.next();
				if (!mob.entity.isDead() && !mob.hasDespawned) {
					checkForDespawn(mob);
				}
				if (mob.entity.isDead() || mob.hasDespawned) {
					allMobsItr.remove();
				}
			}
		}

		for (CaveTracker cave : DescentIntoDarkness.plugin.getCaveTrackerManager().getCaves()) {
			spawnMobs(cave);
		}
	}

	@EventHandler
	public void onMythicMobSpawn(MythicMobSpawnEvent event) {
		if (!spawningMob) {
			return;
		}
		World world = event.getLocation().getWorld();
		if (world == null) {
			return;
		}
		if (!canSpawnMob(world, currentSpawnEntry, event.getEntity())) {
			event.setCancelled();
		}
	}

	private boolean canSpawnMob(World world, MobSpawnEntry spawnEntry, Entity mob) {
		BoundingBox boundingBox = mob.getBoundingBox();
		return canSpawnMob(world, spawnEntry,
				boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
				boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
	}

	private boolean canSpawnMob(World world, MobSpawnEntry spawnEntry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		for (int x = (int)Math.floor(minX + 0.0001); x <= (int)Math.ceil(maxX - 0.0001); x++) {
			for (int y = (int)Math.floor(minY + 0.0001); y <= (int)Math.ceil(maxY - 0.0001); y++) {
				for (int z = (int)Math.floor(minZ + 0.0001); z <= (int)Math.ceil(maxZ - 0.0001); z++) {
					if (!canSpawnIn(spawnEntry, blockData2State(world.getBlockAt(x, y, z).getBlockData()))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean canSpawnIn(MobSpawnEntry spawnEntry, BlockStateHolder<?> block) {
		if (spawnEntry.getCanSpawnIn() == null) {
			BlockMaterial material = block.getBlockType().getMaterial();
			return !material.isMovementBlocker() && !material.isLiquid();
		} else {
			return spawnEntry.getCanSpawnIn().stream().anyMatch(it -> it.equalsFuzzy(block));
		}
	}

	private static BlockStateHolder<?> blockData2State(BlockData data) {
		return com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(data);
	}

	private void checkForDespawn(MobInstance mob) {
		int despawnRange = mob.spawnEntry.getDespawnRange();

		// check if this mob is actually allowed to despawn
		if (despawnRange <= 0) {
			return;
		}

		// quick exit if cave contains no players, mob couldn't despawn anyway
		if (mob.cave.getPlayers().isEmpty()) {
			return;
		}

		// check if there are players within the despawn range of the mob
		if (mob.cave.getPlayers().stream()
				.map(Bukkit::getOfflinePlayer)
				.filter(OfflinePlayer::isOnline)
				.map(OfflinePlayer::getPlayer)
				.filter(player -> player != null && player.getWorld() == mob.cave.getWorld())
				.anyMatch(player -> player.getLocation().distanceSquared(mob.entity.getLocation()) <= despawnRange * despawnRange)) {
			return;
		}

		// find a player to give the pollution back to, preferably the player which spawned the mob
		UUID victimPlayer = null;
		if (mob.cave.getPlayers().contains(mob.spawnedPlayer)) {
			OfflinePlayer player = Bukkit.getOfflinePlayer(mob.spawnedPlayer);
			if (player.isOnline()) {
				assert player.getPlayer() != null;
				if (player.getPlayer().getWorld() == mob.cave.getWorld()) {
					victimPlayer = mob.spawnedPlayer;
				}
			}
		}
		if (victimPlayer == null) {
			Player player = getRandomPlayer(mob.cave, mob.spawnEntry, Integer.MIN_VALUE, p -> 1);
			if (player != null) {
				victimPlayer = player.getUniqueId();
			}
		}
		if (victimPlayer == null) {
			// no player to give pollution to, abort
			return;
		}

		mob.cave.getMobEntry(mob.spawnEntry).addPlayerPollution(victimPlayer, mob.spawnEntry.getSingleMobCost());
		mob.remove();
		mob.hasDespawned = true;
	}

	private static abstract class MobInstance {
		protected final CaveTracker cave;
		protected final MobSpawnEntry spawnEntry;
		protected final Entity entity;
		protected final UUID spawnedPlayer;
		protected boolean hasDespawned = false;

		protected MobInstance(CaveTracker cave, MobSpawnEntry spawnEntry, Entity entity, UUID spawnedPlayer) {
			this.cave = cave;
			this.spawnEntry = spawnEntry;
			this.entity = entity;
			this.spawnedPlayer = spawnedPlayer;
		}

		public abstract void remove();
	}

	private static final class VanillaMobInstance extends MobInstance {
		protected VanillaMobInstance(CaveTracker cave, MobSpawnEntry entry, Entity entity, UUID spawnedPlayer) {
			super(cave, entry, entity, spawnedPlayer);
		}

		@Override
		public void remove() {
			entity.remove();
		}
	}

	private static final class MythicMobInstance extends MobInstance {
		private final ActiveMob mob;

		protected MythicMobInstance(CaveTracker cave, MobSpawnEntry entry, ActiveMob mob, UUID spawnedPlayer) {
			super(cave, entry, BukkitAdapter.adapt(mob.getEntity()), spawnedPlayer);
			this.mob = mob;
		}

		@Override
		public void remove() {
			mob.setDespawned();
		}
	}
}
