package com.gmail.sharpcastle33.did.generator.structure;

import com.sk89q.worldedit.util.Direction;

public enum StructurePlacementEdge {
	FLOOR(Direction.DOWN), CEILING(Direction.UP), WALL(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
	private final Direction[] directions;

	StructurePlacementEdge(Direction... directions) {
		this.directions = directions;
	}

	public Direction[] getDirections() {
		return directions;
	}
}
