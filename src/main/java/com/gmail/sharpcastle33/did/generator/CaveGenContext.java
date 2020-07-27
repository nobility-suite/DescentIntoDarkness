package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CaveGenContext implements AutoCloseable {
    private final EditSession session;
    public final CaveStyle style;
    public final Random rand;
    private boolean debug;
    private final Map<BlockVector3, BlockState> blockCache = new HashMap<>();

    private CaveGenContext(EditSession session, CaveStyle style, Random rand) {
        this.session = session;
        this.style = style;
        this.rand = rand;
    }

    public CaveGenContext setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public static CaveGenContext create(World world, CaveStyle style, Random rand) {
        EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
        return new CaveGenContext(session, style, rand);
    }

    public boolean setBlock(BlockVector3 pos, BlockStateHolder<?> block) throws MaxChangedBlocksException {
        if (session.setBlock(pos, block)) {
            blockCache.put(pos, block.toImmutableState());
            return true;
        } else {
            return false;
        }
    }

    public BlockState getBlock(BlockVector3 pos) {
        return blockCache.computeIfAbsent(pos, session::getBlock);
    }

    public Extent asExtent() {
        return new AbstractDelegateExtent(session) {
            @Override
            public BlockState getBlock(BlockVector3 position) {
                return CaveGenContext.this.getBlock(position);
            }

            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
                return CaveGenContext.this.setBlock(location, block);
            }
        };
    }

    @Override
    public void close() {
        session.close();
    }
}
