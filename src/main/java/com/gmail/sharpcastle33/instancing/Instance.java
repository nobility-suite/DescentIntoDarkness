package com.gmail.sharpcastle33.instancing;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Instance {
  
  private World world;
  private Location start;
  private ArrayList<Player> members;
  private int danger;
  
  public Instance(World world, Location start) {
    this.world = world;
    this.start = start;
    this.danger = 0;
    members = new ArrayList<Player>();
  }

  public World getWorld() {
    return world;
  }

  public void setWorld(World world) {
    this.world = world;
  }

  public Location getStart() {
    return start;
  }

  public void setStart(Location start) {
    this.start = start;
  }

  public ArrayList<Player> getMembers() {
    return members;
  }

  public void addMember(Player p) {
    this.members.add(p);
  }
  
  public void removeMember(Player p) {
    this.members.remove(p);
  }

  public int getDanger() {
    return danger;
  }

  public void setDanger(int danger) {
    this.danger = danger;
  }

}
