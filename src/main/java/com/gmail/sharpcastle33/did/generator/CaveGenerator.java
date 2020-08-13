package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.Util;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;

public class CaveGenerator {

	public static void generateBlank(EditSession session, BlockStateHolder<?> base, int x, int y, int z, int radius, int yRadius) throws WorldEditException {
		session.setBlocks((Region)new CuboidRegion(
				BlockVector3.at(x - radius, Math.max(0, y - yRadius), z - radius),
				BlockVector3.at(x + radius, Math.min(255, y + yRadius), z + radius)),
			base);
	}

	public static String generateCave(CaveGenContext ctx, Vector3 pos, int size) throws WorldEditException {
		Bukkit.getLogger().log(Level.INFO, "Generating cave of size " + size);
		ArrayList<Centroid> centroids = new ArrayList<>();
		Vector3 startingDir = Util.rotateAroundY(Vector3.UNIT_X, ctx.rand.nextDouble() * 2 * Math.PI);
		String caveString = generateBranch(ctx, size, pos, 90, true, startingDir, centroids);
		PostProcessor.postProcess(ctx, centroids);
		return caveString;
	}

	public static String generateBranch(CaveGenContext ctx, int size, Vector3 pos, int length, boolean moreBranches, Vector3 dir, List<Centroid> centroids) throws WorldEditException {
		String s = LayoutGenerator.generateCave(ctx, length);

		if(!moreBranches) {
			Room simpleRoom = ctx.style.getRooms().stream().filter(room -> room instanceof Room.SimpleRoom).findFirst().orElse(null);
			String branchReplacement = simpleRoom == null ? "" : String.valueOf(simpleRoom.getSymbol());
			for (Room room : ctx.style.getRooms()) {
				if (room.isBranch()) {
					s = s.replace(String.valueOf(room.getSymbol()), branchReplacement);
				}
			}
			Bukkit.getServer().getLogger().log(Level.WARNING, "New Branch: " + s);
		}

		ModuleGenerator.read(ctx, s, pos, dir, size, centroids);
		return s;
	}



}
