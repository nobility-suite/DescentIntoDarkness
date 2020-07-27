package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Main;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class Structure {
    private final String name;
    private final Type type;
    protected final List<Edge> edges;
    private final double chance;
    protected final List<BlockStateHolder<?>> canPlaceOn;
    protected final List<BlockStateHolder<?>> canReplace;
    private final List<Direction> validDirections = new ArrayList<>();

    protected Structure(String name, Type type, ConfigurationSection map) {
        this.name = name;
        this.type = type;
        this.edges = ConfigUtil.deserializeSingleableList(map.get("edges"), val -> ConfigUtil.parseEnum(Edge.class, val), () -> Lists.newArrayList(Edge.values()));
        this.chance = map.getDouble("chance", 1);
        this.canPlaceOn = deserializePlacementRule(map.get("canPlaceOn"));
        this.canReplace = deserializePlacementRule(map.get("canReplace"));
        computeValidDirections();
    }

    protected Structure(String name, Type type, List<Edge> edges, double chance, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace) {
        this.name = name;
        this.type = type;
        this.edges = edges;
        this.chance = chance;
        this.canPlaceOn = canPlaceOn;
        this.canReplace = canReplace;
        computeValidDirections();
    }

    private void computeValidDirections() {
        if (edges.isEmpty()) {
            throw new InvalidConfigException("No edges to choose from");
        }
        for (Edge edge : edges) {
            Collections.addAll(validDirections, edge.directions);
        }
    }

    public final String getName() {
        return name;
    }

    public final Type getType() {
        return type;
    }

    public Direction getRandomDirection(Random rand) {
        return validDirections.get(rand.nextInt(validDirections.size()));
    }

    public double getChance() {
        return chance;
    }

    public boolean canPlaceOn(CaveGenContext ctx, BlockStateHolder<?> block) {
        if (canPlaceOn == null) {
            return !ctx.style.isTransparentBlock(block);
        } else {
            return canPlaceOn.stream().anyMatch(it -> it.equalsFuzzy(block));
        }
    }

    public void serialize(ConfigurationSection map) {
        map.set("type", ConfigUtil.enumToString(type));
        map.set("edges", ConfigUtil.serializeSingleableList(edges, ConfigUtil::enumToString));
        map.set("chance", chance);
        if (canPlaceOn != null) {
            map.set("canPlaceOn", ConfigUtil.serializeSingleableList(canPlaceOn, BlockStateHolder::getAsString));
        }
        if (canReplace != null) {
            map.set("canReplace", ConfigUtil.serializeSingleableList(canReplace, BlockStateHolder::getAsString));
        }
        serialize0(map);
    }

    protected abstract void serialize0(ConfigurationSection map);

    public static Structure deserialize(String name, ConfigurationSection map) {
        if (!map.contains("type")) {
            throw new InvalidConfigException("Structure missing type");
        }
        Type type = ConfigUtil.parseEnum(Type.class, map.getString("type"));
        return type.deserialize(name, map);
    }

    protected static List<Edge> deserializeEdges(Object edges) {
        List<Edge> ret = ConfigUtil.deserializeSingleableList(edges, val -> ConfigUtil.parseEnum(Edge.class, val), () -> Lists.newArrayList(Edge.values()));
        if (ret.isEmpty()) {
            throw new InvalidConfigException("No edges to choose from");
        }
        return ret;
    }

    protected static List<BlockStateHolder<?>> deserializePlacementRule(Object rule) {
        return ConfigUtil.deserializeSingleableList(rule, ConfigUtil::parseBlock, () -> null);
    }

    public abstract void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException;

    protected boolean canReplace(CaveGenContext ctx, BlockStateHolder<?> block) {
        if (canReplace == null) {
            return ctx.style.isTransparentBlock(block);
        } else {
            return canReplace.stream().anyMatch(it -> it.equalsFuzzy(block));
        }
    }

    public static class SchematicStructure extends Structure {
        private final List<Schematic> schematics;
        private final Direction originSide;

        public SchematicStructure(String name, ConfigurationSection map) {
            super(name, Type.SCHEMATIC, map);
            this.schematics = ConfigUtil.deserializeSingleableList(map.get("schematics"), schematicName -> {
                Clipboard data = Main.plugin.getSchematic(schematicName);
                if (data == null) {
                    throw new InvalidConfigException("Unknown schematic: " + schematicName);
                }
                return new SchematicStructure.Schematic(schematicName, data);
            }, () -> null);
            if (schematics == null) {
                throw new InvalidConfigException("Missing \"schematics\"");
            }
            String originSideVal = map.getString("originSide");
            if (originSideVal == null) {
                this.originSide = Direction.DOWN;
            } else {
                this.originSide = ConfigUtil.parseEnum(Direction.class, originSideVal);
                if (!originSide.isCardinal() && !originSide.isUpright()) {
                    throw new InvalidConfigException("Invalid Direction: " + originSideVal);
                }
            }
        }

        public SchematicStructure(String name, List<Edge> edges, double chance, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, List<Schematic> schematics, Direction originSide) {
            super(name, Type.SCHEMATIC, edges, chance, canPlaceOn, canReplace);
            this.schematics = schematics;
            this.originSide = originSide;
        }

        @Override
        protected void serialize0(ConfigurationSection map) {
            map.set("schematics", ConfigUtil.serializeSingleableList(schematics, schematic -> schematic.name));
            map.set("originSide", ConfigUtil.enumToString(originSide));
        }

        @Override
        public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
            Schematic chosenSchematic = schematics.get(ctx.rand.nextInt(schematics.size()));
            ClipboardHolder clipboardHolder = new ClipboardHolder(chosenSchematic.data);
            if (side != originSide) {
                AffineTransform transform = new AffineTransform();
                if (originSide == Direction.DOWN) {
                    switch (side) {
                        case UP: transform = transform.scale(1, -1, 1); break;
                        case NORTH: transform = transform.rotateX(-90); break;
                        case SOUTH: transform = transform.rotateX(90); break;
                        case WEST: transform = transform.rotateZ(90); break;
                        case EAST: transform = transform.rotateZ(-90); break;
                        default: throw new AssertionError("There are too many directions!");
                    }
                } else if (originSide == Direction.UP) {
                    switch (side) {
                        case DOWN: transform = transform.scale(1, -1, 1); break;
                        case NORTH: transform = transform.rotateX(90); break;
                        case SOUTH: transform = transform.rotateX(-90); break;
                        case WEST: transform = transform.rotateZ(-90); break;
                        case EAST: transform = transform.rotateZ(90); break;
                        default: throw new AssertionError("There are too many directions!");
                    }
                } else {
                    if (side.isCardinal()) {
                        transform = transform.rotateY(originSide.toBlockVector().toYaw() - side.toBlockVector().toYaw());
                    } else if (side == Direction.DOWN) {
                        switch (originSide) {
                            case NORTH: transform = transform.rotateX(90); break;
                            case SOUTH: transform = transform.rotateX(-90); break;
                            case WEST: transform = transform.rotateZ(-90); break;
                            case EAST: transform = transform.rotateZ(90); break;
                            default: throw new AssertionError("There are too many directions!");
                        }
                    } else {
                        switch (originSide) {
                            case NORTH: transform = transform.rotateX(-90); break;
                            case SOUTH: transform = transform.rotateX(90); break;
                            case WEST: transform = transform.rotateZ(90); break;
                            case EAST: transform = transform.rotateZ(-90); break;
                            default: throw new AssertionError("There are too many directions!");
                        }
                    }
                }
                clipboardHolder.setTransform(transform);
            }
            BlockVector3 to = pos.subtract(side.toBlockVector());
            if (canPlace(ctx, to, chosenSchematic.data, clipboardHolder.getTransform())) {
                Operation paste = clipboardHolder.createPaste(ctx.asExtent()).to(to).ignoreAirBlocks(true).build();
                Operations.complete(paste);
                if (ctx.isDebug()) {
                    ctx.setBlock(to, Util.requireDefaultState(BlockTypes.DIAMOND_BLOCK));
                }
            }
        }

        private boolean canPlace(CaveGenContext ctx, BlockVector3 to, Clipboard schematic, Transform transform) {
            for (BlockVector3 pos : schematic.getRegion()) {
                if (schematic.getBlock(pos).getBlockType() == BlockTypes.AIR) {
                    continue;
                }
                BlockVector3 destPos = transform.apply(pos.subtract(schematic.getOrigin()).toVector3()).toBlockPoint().add(to);
                BlockStateHolder<?> block = ctx.getBlock(destPos);
                if (!canReplace(ctx, block)) {
                    return false;
                }
            }
            return true;
        }

        public static class Schematic {
            private final String name;
            private final Clipboard data;

            private Schematic(String name, Clipboard data) {
                this.name = name;
                this.data = data;
            }
        }
    }

    public static class VeinStructure extends Structure {
        private final BlockStateHolder<?> ore;
        private final int radius;

        public VeinStructure(String name, ConfigurationSection map) {
            super(name, Type.VEIN, map);
            String oreVal = map.getString("ore");
            if (oreVal == null) {
                throw new InvalidConfigException("Vein missing \"ore\"");
            }
            this.ore = ConfigUtil.parseBlock(oreVal);
            this.radius = map.getInt("radius", 4);
        }

        public VeinStructure(String name, List<Edge> edges, double chance, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, BlockStateHolder<?> ore, int radius) {
            super(name, Type.VEIN, edges, chance, canPlaceOn, canReplace);
            this.ore = ore;
            this.radius = radius;
        }

        @Override
        protected void serialize0(ConfigurationSection map) {
            map.set("ore", ore.getAsString());
            map.set("radius", radius);
        }

        @Override
        public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
            ModuleGenerator.generateOreCluster(ctx, pos, radius, canReplace, ore);
        }
    }

    public static class PatchStructure extends Structure {
        private final BlockStateHolder<?> block;
        private final int spreadX;
        private final int spreadY;
        private final int spreadZ;
        private final int tries;

        protected PatchStructure(String name, ConfigurationSection map) {
            super(name, Type.PATCH, map);
            this.block = ConfigUtil.parseBlock(map.getString("block"));
            this.spreadX = map.getInt("spreadX", 8);
            this.spreadY = map.getInt("spreadY", 4);
            this.spreadZ = map.getInt("spreadZ", 8);
            if (spreadX < 0 || spreadY < 0 || spreadZ < 0) {
                throw new InvalidConfigException("Spread cannot be negative");
            }
            this.tries = map.getInt("tries", 64);
        }

        protected PatchStructure(String name, Type type, List<Edge> edges, double chance, List<BlockStateHolder<?>> canPlaceOn, List<BlockStateHolder<?>> canReplace, BlockStateHolder<?> block, int spreadX, int spreadY, int spreadZ, int tries) {
            super(name, type, edges, chance, canPlaceOn, canReplace);
            this.block = block;
            this.spreadX = spreadX;
            this.spreadY = spreadY;
            this.spreadZ = spreadZ;
            this.tries = tries;
        }

        @Override
        protected void serialize0(ConfigurationSection map) {
            map.set("block", block.getAsString());
            map.set("spreadX", spreadX);
            map.set("spreadY", spreadY);
            map.set("spreadZ", spreadZ);
            map.set("tries", tries);
        }

        @Override
        public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
            BlockVector3 origin = pos.subtract(side.toBlockVector());
            for (int i = 0; i < tries; i++) {
                BlockVector3 offsetPos = origin.add(
                        ctx.rand.nextInt(spreadX + 1) - ctx.rand.nextInt(spreadX + 1),
                        ctx.rand.nextInt(spreadY + 1) - ctx.rand.nextInt(spreadY + 1),
                        ctx.rand.nextInt(spreadZ + 1) - ctx.rand.nextInt(spreadZ + 1)
                );
                if (canReplace(ctx, ctx.getBlock(offsetPos))) {
                    BlockStateHolder<?> blockBelow = ctx.getBlock(offsetPos.add(side.toBlockVector()));
                    boolean allowPlacement;
                    if (canPlaceOn == null) {
                        allowPlacement = !ctx.style.isTransparentBlock(blockBelow);
                    } else {
                        allowPlacement = canPlaceOn.stream().anyMatch(it -> it.equalsFuzzy(blockBelow));
                    }
                    if (allowPlacement) {
                        ctx.setBlock(offsetPos, this.block);
                    }
                }
            }
        }
    }

    public enum Type {
        SCHEMATIC {
            @Override
            public Structure deserialize(String name, ConfigurationSection map) {
                return new SchematicStructure(name, map);
            }
        },
        VEIN {
            @Override
            public Structure deserialize(String name, ConfigurationSection map) {
                return new VeinStructure(name, map);
            }
        },
        PATCH {
            @Override
            public Structure deserialize(String name, ConfigurationSection map) {
                return new PatchStructure(name, map);
            }
        },
        ;

        public abstract Structure deserialize(String name, ConfigurationSection map);
    }

    public enum Edge {
        FLOOR(Direction.DOWN), CEILING(Direction.UP), WALL(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
        private final Direction[] directions;
        Edge(Direction... directions) {
            this.directions = directions;
        }
    }
}
