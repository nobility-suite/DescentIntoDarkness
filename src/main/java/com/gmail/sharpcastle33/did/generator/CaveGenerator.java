package com.gmail.sharpcastle33.did.generator;

import java.util.logging.Level;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
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

	public static String generateCave(CaveGenContext ctx, Vector3 pos) throws MaxChangedBlocksException {
		return generateCave(ctx, pos, 5);

	}

	public static String generateCave(CaveGenContext ctx, Vector3 pos, int size) throws MaxChangedBlocksException {
		return generateCave(ctx,size,pos,90,true,Vector3.UNIT_X);
	}

	public static String generateCave(CaveGenContext ctx, int size, Vector3 pos, int length, boolean branches, Vector3 dir) throws MaxChangedBlocksException {

		int len = 100;
		String s = LayoutGenerator.generateCave(ctx, length, 0);

		if(!branches) {
			s = s.replace("X", "W");
			s = s.replace("x", "W");
			Bukkit.getServer().getLogger().log(Level.WARNING, "New Branch: " + s);
		}

		ModuleGenerator gen = new ModuleGenerator();
		gen.read(ctx, s, pos, size ,dir);
		return s;
	}



}
