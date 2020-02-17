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
	  
	  public static ArrayList<String> generateCaveStrings(int maxLength, int maxBranches){
	    ArrayList<String> ret = new ArrayList<String>();
	    
	    int totalLength = 0;
	    int totalBranches = 0;
	    
	    String start = generateCave(maxLength, maxBranches);
	    
	    ret.add(start);
	    
	    int newBranches = countBranches(start);
	    totalBranches += newBranches;
	    
	    for(int i = 0; i < newBranches; i++) {
	      ret.add(generateCave(maxLength, maxBranches-totalBranches));
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
	  
	  public static String generateCave(int maxLength, int maxBranches) {
	    String cave = "";
	    
	    Random rand = new Random();
	    int caverns = rand.nextInt(4)+3;
	  
	    for(int i = 0; i < caverns; i++) {
	    	int length = rand.nextInt(7)+4;
	    	cave+=generateCavern(length);
	    	length = rand.nextInt(5)+5;
	    	cave+=generatePassage(length);
	    }
	    
	    if(cave.length() < maxLength) {
	    	int length = maxLength-cave.length();
	    	for(int i = 0; i < length; i++) {
	  	      cave += generateNextCavern();
	  	    }
	    }
	    
	    if(cave.length() > maxLength) {
	    	cave = cave.substring(0, maxLength);
	    }
	    return cave;
	    
	    
	    
	    
	    
	    /*for(int i = 0; i < maxLength; i++) {
	      cave += generateNext();
	    }return cave;*/
	  }
	  
	  public static String generateCavern(int len) {
		  String ret = "";
		  for(int i = 0; i < len; i++) {
			  ret = ret + generateNextCavern();
		  }
		  return ret;
	  }
	  
	  public static String generatePassage(int len) {
		  String ret = "";
		  for(int i = 0; i < len; i++) {
			  ret = ret + generateNextPassage();
		  }
		  return ret;
	  }
	  
	  public static char generateNextPassage() {
		    Random rand = new Random();
		    int n = rand.nextInt(100);
		    
		    if(n < 60) {
		      return 'W';
		    }else if(n < 75) {
		      return 'A';
		    }else if(n < 90) {
		      return 'D';
		    }else if(n < 92) {
		      return 'X';
		    }else if(n < 95) {
		      return 'O';
		    }else if(n < 101) {
		      return 'R';
		    }
		    return 'W';
	  }
	  
	  public static char generateNextCavern() {
	    Random rand = new Random();
	    int n = rand.nextInt(167);
	    
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
	    }else if(n < 150) {
	      return 'R';
	    }else if(n < 177) {
	      return 'L';
	    }else if(n < 167) {
		      return 'W';
		}
	    
	    return 'F';
	  }
}
