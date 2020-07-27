package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.Main;
import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class Structure {
    private final String name;
    private final Type type;
    protected final List<Edge> edges;
    private final double chance;
    private final List<Direction> validDirections;

    protected Structure(String name, Type type, List<Edge> edges, double chance) {
        this.name = name;
        this.type = type;
        this.edges = edges;
        this.chance = chance;
        this.validDirections = new ArrayList<>();
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

    public void serialize(ConfigurationSection map) {
        map.set("type", ConfigUtil.enumToString(type));
        if (edges.size() == 1) {
            map.set("edges", ConfigUtil.enumToString(edges.get(0)));
        } else {
            map.set("edges", edges.stream().map(ConfigUtil::enumToString).collect(Collectors.toCollection(ArrayList::new)));
        }
        map.set("chance", chance);
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
        if (edges == null) {
            return Lists.newArrayList(Edge.values());
        }
        if (edges instanceof List) {
            ArrayList<Edge> ret = ((List<?>) edges).stream().map(val -> ConfigUtil.parseEnum(Edge.class, val.toString())).collect(Collectors.toCollection(ArrayList::new));
            if (ret.isEmpty()) {
                throw new InvalidConfigException("No edges to choose from");
            }
            return ret;
        }
        return Lists.newArrayList(ConfigUtil.parseEnum(Edge.class, edges.toString()));
    }

    public abstract void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException;

    public static class SchematicStructure extends Structure {
        private final String schematicName;
        private final Clipboard schematic;
        private final Direction originSide;

        public SchematicStructure(String name, List<Edge> edges, double chance, String schematicName, Clipboard schematic, Direction originSide) {
            super(name, Type.SCHEMATIC, edges, chance);
            this.schematicName = schematicName;
            this.schematic = schematic;
            this.originSide = originSide;
        }

        @Override
        protected void serialize0(ConfigurationSection map) {
            map.set("schematic", schematicName);
            map.set("originSide", ConfigUtil.enumToString(originSide));
        }

        @Override
        public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
            ClipboardHolder clipboardHolder = new ClipboardHolder(schematic);
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
            Operation paste = clipboardHolder.createPaste(ctx.asExtent()).to(pos.subtract(side.toBlockVector())).ignoreAirBlocks(true).build();
            Operations.complete(paste);
        }
    }

    public static class Vein extends Structure {
        private final List<BlockStateHolder<?>> oldBlocks;
        private final BlockStateHolder<?> newBlock;
        private final int radius;

        public Vein(String name, List<Edge> edges, double chance, List<BlockStateHolder<?>> oldBlocks, BlockStateHolder<?> newBlock, int radius) {
            super(name, Type.VEIN, edges, chance);
            this.oldBlocks = oldBlocks;
            this.newBlock = newBlock;
            this.radius = radius;
        }

        @Override
        protected void serialize0(ConfigurationSection map) {
            if (oldBlocks != null) {
                if (oldBlocks.size() == 1) {
                    map.set("oldBlocks", oldBlocks.get(0).getAsString());
                } else {
                    map.set("oldBlocks", oldBlocks.stream().map(BlockStateHolder::getAsString).collect(Collectors.toCollection(ArrayList::new)));
                }
            }
            map.set("newBlock", newBlock.getAsString());
            map.set("radius", radius);
        }

        @Override
        public void place(CaveGenContext ctx, BlockVector3 pos, Direction side) throws WorldEditException {
            ModuleGenerator.generateOreCluster(ctx, pos, radius, oldBlocks, newBlock);
        }
    }

    public enum Type {
        SCHEMATIC {
            @Override
            public Structure deserialize(String name, ConfigurationSection map) {
                List<Edge> edges = deserializeEdges(map.get("edges"));
                double chance = map.getDouble("chance", 1);
                String schematicName = map.getString("schematic");
                if (schematicName == null) {
                    throw new InvalidConfigException("Missing schematic");
                }
                Clipboard schematic = Main.plugin.getSchematic(schematicName);
                if (schematic == null) {
                    throw new InvalidConfigException("Unknown schematic " + schematicName);
                }
                String originSideVal = map.getString("originSide");
                Direction originSide;
                if (originSideVal == null) {
                    originSide = Direction.DOWN;
                } else {
                    originSide = ConfigUtil.parseEnum(Direction.class, originSideVal);
                    if (!originSide.isCardinal() && !originSide.isUpright()) {
                        throw new InvalidConfigException("Invalid Direction: " + originSideVal);
                    }
                }
                return new SchematicStructure(name, edges, chance, schematicName, schematic, originSide);
            }
        },
        VEIN {
            @Override
            public Structure deserialize(String name, ConfigurationSection map) {
                List<Edge> edges = deserializeEdges(map.get("edges"));
                double chance = map.getDouble("chance", 1);
                Object oldBlocksVal = map.get("oldBlocks");
                List<BlockStateHolder<?>> oldBlocks;
                if (oldBlocksVal == null) {
                    oldBlocks = null;
                } else if (oldBlocksVal instanceof List) {
                    oldBlocks = ((List<?>) oldBlocksVal).stream().map(val -> ConfigUtil.parseBlock(val.toString())).collect(Collectors.toCollection(ArrayList::new));
                } else {
                    oldBlocks = Lists.newArrayList(ConfigUtil.parseBlock(oldBlocksVal.toString()));
                }
                String newBlockVal = map.getString("newBlock");
                if (newBlockVal == null) {
                    throw new InvalidConfigException("Vein missing newBlock");
                }
                BlockStateHolder<?> newBlock = ConfigUtil.parseBlock(newBlockVal);
                int radius = map.getInt("radius", 4);
                return new Vein(name, edges, chance, oldBlocks, newBlock, radius);
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
