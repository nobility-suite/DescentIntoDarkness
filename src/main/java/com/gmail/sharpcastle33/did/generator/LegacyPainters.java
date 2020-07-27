package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Util;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class LegacyPainters {
    public static void paintOcean(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        r = r+1;
        //Material.PRISMARINE;
        //Material.PRISMARINE_BRICKS;
        //Material.DARK_PRISMARINE;

        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.PRISMARINE));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.PRISMARINE), Util.requireDefaultState(BlockTypes.DARK_PRISMARINE),0.1);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.PRISMARINE), Util.requireDefaultState(BlockTypes.PRISMARINE_BRICKS), 0.1);
    }

    public static void paintCoral(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.BUBBLE_CORAL_BLOCK),0.1);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.TUBE_CORAL_BLOCK),0.1);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.HORN_CORAL_BLOCK),0.1);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.FIRE_CORAL_BLOCK),0.1);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.DEAD_HORN_CORAL_BLOCK),0.1);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BRAIN_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.WET_SPONGE),0.05);

    }

    public static void paintGeneric(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAVEL));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.ANDESITE),0.2);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.COBBLESTONE),0.2);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.MOSSY_COBBLESTONE),0.05);
    }

    public static void paintMarble(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.DIORITE));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.DIORITE), Util.requireDefaultState(BlockTypes.POLISHED_DIORITE),0.2);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.DIORITE), Util.requireDefaultState(BlockTypes.QUARTZ_BLOCK),0.1);
    }

    public static void paintGlacial(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.BLUE_ICE));

        PostProcessor.replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.BLUE_ICE), Util.requireDefaultState(BlockTypes.SNOW_BLOCK));

        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.BLUE_ICE), Util.requireDefaultState(BlockTypes.PACKED_ICE),0.2);
    }

    public static void paintTest(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.SNOW_BLOCK));
        PostProcessor.replaceCeiling(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.OBSIDIAN));
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.RED_WOOL));
    }

    public static void paintMesa(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRANITE));
        PostProcessor.replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.GRANITE), Util.requireDefaultState(BlockTypes.RED_SAND));
        PostProcessor.replaceCeiling(ctx,loc,r, Util.requireDefaultState(BlockTypes.GRANITE), Util.requireDefaultState(BlockTypes.RED_TERRACOTTA));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.GRANITE), Util.requireDefaultState(BlockTypes.POLISHED_GRANITE),0.2);
    }

    public static void paintDesert(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.SANDSTONE));
        PostProcessor.replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.SAND));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.CHISELED_SANDSTONE),0.2);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.GRANITE),0.2);
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.SANDSTONE), Util.requireDefaultState(BlockTypes.POLISHED_GRANITE),0.1);
    }

    public static void paintMagma(CaveGenContext ctx, BlockVector3 loc, int r) throws MaxChangedBlocksException {
        //Material.OBSIDIAN
        //Material.BLACK_CONCRETE_POWDER;
        //Material.BLACK_CONCRETE;
        //Material.MAGMA_BLOCK;
        //Material.
        PostProcessor.replaceFloor(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.BLACK_CONCRETE_POWDER));
        PostProcessor.replaceCeiling(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.DEAD_TUBE_CORAL_BLOCK));
        PostProcessor.radiusReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.STONE), Util.requireDefaultState(BlockTypes.GRAY_CONCRETE));
        PostProcessor.chanceReplace(ctx,loc,r, Util.requireDefaultState(BlockTypes.DEAD_TUBE_CORAL_BLOCK), Util.requireDefaultState(BlockTypes.DEAD_FIRE_CORAL_BLOCK),0.5);

    }

    public static void generateBlob(CaveGenContext ctx, BlockVector3 loc, int r, int rx, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {

        int tx = ctx.rand.nextInt(r*2)-r;
        int tz = ctx.rand.nextInt(r*2)-r;
        int ty = ctx.rand.nextInt(r*2)-r;


        BlockVector3 next = loc.add(tx,ty,tz);
        PostProcessor.radiusReplace(ctx, next,rx,old,m);
    }

    public static void generateBlobs(CaveGenContext ctx, BlockVector3 loc, int r, int rx, int amt, BlockStateHolder<?> old, BlockStateHolder<?> m) throws MaxChangedBlocksException {
        for(int i = 0; i < amt; i++) {
            generateBlob(ctx,loc,r,rx,old,m);
        }
    }
}
