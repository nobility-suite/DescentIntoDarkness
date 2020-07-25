package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CaveGenContext implements AutoCloseable {
    private final EditSession session;
    public final CaveStyle style;
    public final Random rand;
    private final Map<BlockVector3, BlockStateHolder<?>> blockCache = new HashMap<>();

    private CaveGenContext(EditSession session, CaveStyle style, Random rand) {
        this.session = session;
        this.style = style;
        this.rand = rand;
    }

    public static CaveGenContext create(World world, CaveStyle style, Random rand) {
        EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
        return new CaveGenContext(session, style, rand);
    }

    public boolean setBlock(BlockVector3 pos, BlockStateHolder<?> block) throws MaxChangedBlocksException {
        if (session.setBlock(pos, block)) {
            blockCache.put(pos, block);
            return true;
        } else {
            return false;
        }
    }

    public BlockStateHolder<?> getBlock(BlockVector3 pos) {
        return blockCache.computeIfAbsent(pos, session::getBlock);
    }

    @Override
    public void close() {
        session.close();
    }
}
