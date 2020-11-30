package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.generator.room.Room;
import com.gmail.sharpcastle33.did.generator.room.SimpleRoom;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;

public class CaveGenerator {

	public static void generateBlank(EditSession session, BlockStateHolder<?> base, int x, int y, int z, int radius, int yRadius) throws WorldEditException {
		for (BlockVector3 pos : new CuboidRegion(
				BlockVector3.at(x - radius, Math.max(0, y - yRadius), z - radius),
				BlockVector3.at(x + radius, Math.min(255, y + yRadius), z + radius)
		)) {
			session.setBlock(pos, base);
		}
	}

	public static String generateCave(CaveGenContext ctx, Vector3 pos) {
		int size = ctx.style.getMinSize() + ctx.rand.nextInt(ctx.style.getMaxSize() - ctx.style.getMinSize());
		return generateCave(ctx, pos, size);
	}

	public static String generateCave(CaveGenContext ctx, Vector3 pos, int size) throws WorldEditException {
		Bukkit.getLogger().log(Level.INFO, "Generating cave of size " + size);
		List<Centroid> centroids = new ArrayList<>();
		List<Integer> roomStarts = new ArrayList<>();
		int length = ctx.style.getMinLength() + ctx.rand.nextInt(ctx.style.getMaxLength() - ctx.style.getMinLength() + 1);
		Vector3 startingDir = Vector3.UNIT_X;
		if (ctx.style.usesRandomRotation()) {
			startingDir = Util.rotateAroundY(startingDir, ctx.rand.nextDouble() * 2 * Math.PI);
		}
		String caveString = generateBranch(ctx, size, pos, length, true, startingDir, centroids, roomStarts);
		PostProcessor.postProcess(ctx, centroids, roomStarts);
		return caveString;
	}

	public static String generateBranch(CaveGenContext ctx, int size, Vector3 pos, int length, boolean moreBranches, Vector3 dir, List<Centroid> centroids, List<Integer> roomStarts) throws WorldEditException {
		LayoutGenerator.Layout layout = LayoutGenerator.generateCave(ctx, length);

		if(!moreBranches) {
			Room simpleRoom = ctx.style.getRooms().stream().filter(room -> room instanceof SimpleRoom).findFirst().orElse(null);
			String branchReplacement = simpleRoom == null ? "" : String.valueOf(simpleRoom.getSymbol());
			for (Room room : ctx.style.getRooms()) {
				if (room.isBranch()) {
					layout.setValue(layout.getValue().replace(String.valueOf(room.getSymbol()), branchReplacement));
				}
			}
			Bukkit.getServer().getLogger().log(Level.WARNING, "New Branch: " + layout);
		}

		ModuleGenerator.read(ctx, layout, pos, dir, size, centroids, roomStarts);
		return layout.getValue();
	}



}
