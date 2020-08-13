package com.gmail.sharpcastle33.did.generator;

import com.sk89q.worldedit.math.Vector3;

import java.util.List;

public class Centroid {
	public final Vector3 pos;
	public final int size;
	public final List<String> tags;

	public Centroid(Vector3 pos, int size, List<String> tags) {
		this.pos = pos;
		this.size = size;
		this.tags = tags;
	}
}
