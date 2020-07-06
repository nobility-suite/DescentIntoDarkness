package com.gmail.sharpcastle33.did.generator;

import java.util.logging.Level;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class CaveGenerator {
	
	public static void generateBlank(World world) {
		int x = 0;
		int y = 121;
		int z = 0;
		
		int ylen = 120;
		int len = 200;
		
		for(int i = -len; i < x+len; i++) {
			for(int j = -ylen; j < y+ylen; j++) {
				for(int k = -len; k < z+len; k++) {
					world.getBlockAt(i, j, k).setType(Material.STONE);
				}
			}
		}
	}
	
	public static String generateCave(World world, CaveStyle style) {
		return generateCave(world,5, style);
		
	}
	
	public static String generateCave(World world, int size, CaveStyle style) {
		Vector dir = new Vector(1,0,0);
		return generateCave(world,size,style,0,210,0,90,true,dir);
	}
	
	public static String generateCave(World world, int size, CaveStyle style, int x, int y, int z, int length, boolean branches, Vector dir) {
		
		int len = 100;
		String s = LayoutGenerator.generateCave(length, 0);
		
		if(branches == false) {
			s.replaceAll("X", "W");
			s.replaceAll("x", "W");
			Bukkit.getServer().getLogger().log(Level.WARNING, "New Branch: " + s);
		}
		
		Location start = world.getBlockAt(x,y,z).getLocation();
		
		ModuleGenerator gen = new ModuleGenerator();
		gen.read(s, start, size, style,dir);
		return s;
	}
	
	

}
