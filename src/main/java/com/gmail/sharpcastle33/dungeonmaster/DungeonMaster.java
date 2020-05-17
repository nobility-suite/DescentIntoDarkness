package com.gmail.sharpcastle33.dungeonmaster;

import java.util.Random;

public class DungeonMaster {
	
	public String tempo(int len) {
		String ret = "";
		
		Random rand = new Random();
		int lucky = 0; //Tempers randomness
		
		for(int i = 0; i < len; i++) {
			int rng = rand.nextInt(100-lucky)+lucky;
			
			if(rng <= 5) {
				ret += "...";
				lucky +=10;
			}else if(rng <= 10) {
				ret += "..";
				lucky+=5;
			}else if(rng <= 30) {
				ret += ".";
				lucky+=3;
			}else if(rng <= 70) {
				ret += ".";
				lucky+=1;
			}else if(rng <= 80) {
				ret+="x";
				lucky-=2;
			}else if(rng <= 92) {
				ret+="X";
				lucky-=10;
			}else if(rng <= 98) {
				ret+="!";
				lucky-=25;
			}else {
				ret+="x";
			}		
		}
		return ret;
	}

}
