package com.gmail.sharpcastle33.did;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Objects;

public class Util {

    public static BlockState requireDefaultState(BlockType block) {
        return Objects.requireNonNull(block).getDefaultState();
    }

    public static Vector3 rotateAroundY(Vector3 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return Vector3.at(cos * vector.getX() + sin * vector.getZ(), vector.getY(), cos * vector.getZ() - sin * vector.getX());
    }

}
