package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;

public class CaveGenerator {

	public static void generateBlank(EditSession session, BlockStateHolder<?> base, int x, int y, int z, int radius, int yRadius) throws WorldEditException {
		session.setBlocks(new CuboidRegion(
				BlockVector3.at(x - radius, Math.max(0, y - yRadius), z - radius),
				BlockVector3.at(x + radius, Math.min(255, y + yRadius), z + radius)),
			base);
	}

	public static String generateCave(CaveGenContext ctx, Vector3 pos, int size) throws WorldEditException {
		ArrayList<Centroid> centroids = new ArrayList<>();
		String caveString = generateBranch(ctx, size, pos, 90, true, Vector3.UNIT_X, centroids);
		PostProcessor.postProcess(ctx, centroids);
		return caveString;
	}

	public static String generateBranch(CaveGenContext ctx, int size, Vector3 pos, int length, boolean moreBranches, Vector3 dir, List<Centroid> centroids) throws WorldEditException {
		String s = LayoutGenerator.generateCave(ctx, length, 0);

		if(!moreBranches) {
			s = s.replace("X", "W");
			s = s.replace("x", "W");
			Bukkit.getServer().getLogger().log(Level.WARNING, "New Branch: " + s);
		}

		ModuleGenerator gen = new ModuleGenerator(centroids, size);
		gen.read(ctx, s, pos ,dir);
		return s;
	}



}
