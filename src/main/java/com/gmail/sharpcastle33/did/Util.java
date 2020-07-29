package com.gmail.sharpcastle33.did;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Util {

    public static BlockState requireDefaultState(BlockType block) {
        return Objects.requireNonNull(block).getDefaultState();
    }

    public static Vector3 rotateAroundY(Vector3 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return Vector3.at(cos * vector.getX() + sin * vector.getZ(), vector.getY(), cos * vector.getZ() - sin * vector.getX());
    }

    public static Direction getOpposite(Direction dir) {
        switch (dir) {
            case NORTH: return Direction.SOUTH;
            case EAST: return Direction.WEST;
            case SOUTH: return Direction.NORTH;
            case WEST: return Direction.EAST;
            case UP: return Direction.DOWN;
            case DOWN: return Direction.UP;
            case NORTHEAST: return Direction.SOUTHWEST;
            case NORTHWEST: return Direction.SOUTHEAST;
            case SOUTHEAST: return Direction.NORTHWEST;
            case SOUTHWEST: return Direction.NORTHEAST;
            case WEST_NORTHWEST: return Direction.EAST_SOUTHEAST;
            case WEST_SOUTHWEST: return Direction.EAST_NORTHEAST;
            case NORTH_NORTHWEST: return Direction.SOUTH_SOUTHEAST;
            case NORTH_NORTHEAST: return Direction.SOUTH_SOUTHWEST;
            case EAST_NORTHEAST: return Direction.WEST_SOUTHWEST;
            case EAST_SOUTHEAST: return Direction.WEST_NORTHWEST;
            case SOUTH_SOUTHEAST: return Direction.NORTH_NORTHWEST;
            case SOUTH_SOUTHWEST: return Direction.NORTH_NORTHEAST;
            default: throw new AssertionError("Stop adding new directions, sk89q!");
        }
    }

    public static <T> CompletableFuture<T> completeExceptionally(Throwable t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
    }

}
