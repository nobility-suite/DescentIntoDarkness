package com.gmail.sharpcastle33.did.generator;

import java.util.ArrayList;
import java.util.Random;

public class LayoutGenerator {

	//TODO: Make shelf and subbranches.

	/*
	 * Copyright 2020 (@Sharpcastle33 on GitHub)
	 *
	 * This program generates the outline of a cave system using
	 * a context-free grammar system.
	 *
	 * It is intended for generating caves in instanced zones for voxel games,
	 * where each grammar macro can be used to execute the generation of a section of cave.
	 *
	 * The grammar is applied as follows:
	 *
	 *  --> L L
	 *  --> V V
	 *  --> V L
	 *  --> B L
	 *  --> X L
	 *  --> L
	 *
	 * L ----------
	 * W: Dig forward
	 * A: Dig left
	 * D: Dig right
	 * S: Dig backward
	 *
	 *
	 * V ---------
	 * Q: Dig up
	 * E: Dig down
	 *
	 *
	 * B: Switch biome
	 * X: Branch
	 *
	 *
	 */

	public static ArrayList<String> generateCaveStrings(CaveGenContext ctx, int maxLength, int maxBranches){
		ArrayList<String> ret = new ArrayList<>();

		int totalLength = 0;
		int totalBranches = 0;

		String start = generateCave(ctx, maxLength, maxBranches);

		ret.add(start);

		int newBranches = countBranches(start);
		totalBranches += newBranches;

		for(int i = 0; i < newBranches; i++) {
			ret.add(generateCave(ctx, maxLength, maxBranches-totalBranches));
		}

		return ret;



	}

	public static int countBranches(String s) {
		int count = 0;

		for(char c : s.toCharArray()) {
			if(c == 'X') {
				count++;
			}
		}

		return count;
	}

	public static String generateCave(CaveGenContext ctx, int maxLength, int maxBranches) {
		StringBuilder cave = new StringBuilder();

		int caverns = ctx.rand.nextInt(4)+3;

		for(int i = 0; i < caverns; i++) {
			int length = ctx.rand.nextInt(7)+4;
			cave.append(generateCavern(ctx, length));
			length = ctx.rand.nextInt(5)+5;
			cave.append(generatePassage(ctx, length));
		}

		if(cave.length() < maxLength) {
			int length = maxLength-cave.length();
			for(int i = 0; i < length; i++) {
				cave.append(generateNextCavern(ctx));
			}
		}

		if(cave.length() > maxLength) {
			cave = new StringBuilder(cave.substring(0, maxLength));
		}
		return cave.toString();
	    
	    
	    
	    
	    
	    /*for(int i = 0; i < maxLength; i++) {
	      cave += generateNext();
	    }return cave;*/
	}

	public static String generateCavern(CaveGenContext ctx, int len) {
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < len; i++) {
			ret.append(generateNextCavern(ctx));
		}
		return ret.toString();
	}

	public static String generatePassage(CaveGenContext ctx, int len) {
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < len; i++) {
			ret.append(generateNextPassage(ctx));
		}
		return ret.toString();
	}

	public static char generateNextPassage(CaveGenContext ctx) {
		int n = ctx.rand.nextInt(104);

		if(n < 60) {
			return 'W';
		}else if(n < 75) {
			return 'A';
		}else if(n < 90) {
			return 'D';
		}else if(n < 92) {
			return 'X';
		}else if(n < 94) {
			return 'O';
		}else if(n < 101) {
			return 'R';
		}else if(n < 103) {
			return 'x';
		}
		return 'W';
	}

	public static char generateNextCavern(CaveGenContext ctx) {
		int n = ctx.rand.nextInt(186);

		if(n < 80) {
			return 'W';
		}else if(n < 100) {
			return 'A';
		}else if(n < 120) {
			return 'D';
		}else if(n < 125) {
			return 'X';
		}else if(n < 127) {
			return 'O';
		}else if(n < 129) {
			return 'C';
		}else if(n < 148) {
			return 'R';
		}else if(n < 177) {
			return 'L';
		}else if(n < 185) {
			return 'H';
		}

		return 'F';
	}
}
