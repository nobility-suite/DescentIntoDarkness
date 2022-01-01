package com.gmail.sharpcastle33.did.provider;

import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.function.Predicate;

@FunctionalInterface
public interface BlockPredicate extends Predicate<BlockStateHolder<?>> {
}
