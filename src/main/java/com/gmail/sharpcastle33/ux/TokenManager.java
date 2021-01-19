package com.gmail.sharpcastle33.ux;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;


public class TokenManager {

	public HashMap<UUID,Integer> gemTokens;
	public HashMap<UUID,Integer> metalTokens;
	public HashMap<UUID,Integer> fuelTokens;
	
	public void addTokens(Player p, int amount, String type) {
		switch(type) {
		case "gem":
			if(gemTokens.containsKey(p.getUniqueId())) {
				gemTokens.put(p.getUniqueId(), gemTokens.get(p.getUniqueId()) + amount);
			}else gemTokens.put(p.getUniqueId(), amount);
			break;
		case "metal":
			if(metalTokens.containsKey(p.getUniqueId())) {
				metalTokens.put(p.getUniqueId(), gemTokens.get(p.getUniqueId()) + amount);
			}else metalTokens.put(p.getUniqueId(), amount);
			break;
		case "fuel":
			if(gemTokens.containsKey(p.getUniqueId())) {
				gemTokens.put(p.getUniqueId(), gemTokens.get(p.getUniqueId()) + amount);
			}else gemTokens.put(p.getUniqueId(), amount);
			break;
		}
	}

}
