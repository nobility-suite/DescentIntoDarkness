package com.gmail.sharpcastle33.did.generator.structure;

import com.gmail.sharpcastle33.did.config.ConfigUtil;
import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import com.gmail.sharpcastle33.did.generator.CaveGenContext;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class StalagmiteStructure extends Structure {
	private final BlockStateHolder<?> block;
	private final int floorToCeilingSearchRange;
	private final float maxColumnRadiusToCaveHeightRatio;
	private final int minColumnRadius;
	private final int maxColumnRadius;
	private final float minStalagmiteBluntness;
	private final float maxStalagmiteBluntness;
	private final float minStalactiteBluntness;
	private final float maxStalactiteBluntness;
	private final float minHeightScale;
	private final float maxHeightScale;
	private final float minWindSpeed;
	private final float maxWindSpeed;
	private final int minRadiusForWind;
	private final float minBluntnessForWind;
	private final boolean hasStalactite;

	protected StalagmiteStructure(String name, ConfigurationSection map) {
		super(name, StructureType.STALAGMITE, map);
		this.block = ConfigUtil.parseBlock(ConfigUtil.requireString(map, "block"));
		this.floorToCeilingSearchRange = map.getInt("floorToCeilingSearchRange", 30);
		if (floorToCeilingSearchRange < 1 || floorToCeilingSearchRange > 512) {
			throw new InvalidConfigException("floorToCeilingSearchRange must be 1-512");
		}
		this.maxColumnRadiusToCaveHeightRatio = (float) map.getDouble("maxColumnRadiusToCaveHeightRatio", 0.33);
		if (maxColumnRadiusToCaveHeightRatio < 0.1 || maxColumnRadiusToCaveHeightRatio > 1) {
			throw new InvalidConfigException("maxColumnRadiusToCaveHeightRatio must be 0.1-1");
		}
		this.minColumnRadius = map.getInt("minColumnRadius", 3);
		this.maxColumnRadius = map.getInt("maxColumnRadius", 16);
		if (minColumnRadius < 1 || maxColumnRadius < minColumnRadius || minColumnRadius > 30 || maxColumnRadius > 60) {
			throw new InvalidConfigException("Invalid columnRadius range");
		}
		this.minStalagmiteBluntness = (float) map.getDouble("minStalagmiteBluntness", 0.4);
		this.maxStalagmiteBluntness = (float) map.getDouble("maxStalagmiteBluntness", 0.6);
		if (minStalagmiteBluntness < 0.1 || maxStalagmiteBluntness < minStalagmiteBluntness || minStalagmiteBluntness > 5 || maxStalagmiteBluntness > 10) {
			throw new InvalidConfigException("Invalid stalagmiteBluntness range");
		}
		this.minStalactiteBluntness = (float) map.getDouble("minStalactiteBluntness", 0.3);
		this.maxStalactiteBluntness = (float) map.getDouble("maxStalactiteBluntness", 0.6);
		if (minStalactiteBluntness < 0.1 || maxStalactiteBluntness < minStalactiteBluntness || minStalactiteBluntness > 5 || maxStalactiteBluntness > 10) {
			throw new InvalidConfigException("Invalid stalactiteBluntness range");
		}
		this.minHeightScale = (float) map.getDouble("minHeightScale", 0.4);
		this.maxHeightScale = (float) map.getDouble("maxHeightScale", 1.6);
		if (minHeightScale < 0 || maxHeightScale < minHeightScale || minHeightScale > 10 || maxHeightScale > 20) {
			throw new InvalidConfigException("Invalid heightScale range");
		}
		this.minWindSpeed = (float) map.getDouble("minWindSpeed", 0);
		this.maxWindSpeed = (float) map.getDouble("maxWindSpeed", 0.2);
		if (minWindSpeed < 0 || maxWindSpeed < minWindSpeed || maxWindSpeed > 1) {
			throw new InvalidConfigException("Invalid windSpeed range");
		}
		this.minRadiusForWind = map.getInt("minRadiusForWind", 5);
		if (minRadiusForWind < 0 || minRadiusForWind > 100) {
			throw new InvalidConfigException("minRadiusForWind must be 0-100");
		}
		this.minBluntnessForWind = (float) map.getDouble("minBluntnessForWind", 0.7);
		if (minBluntnessForWind < 0 || minBluntnessForWind > 5) {
			throw new InvalidConfigException("minBluntnessForWind must be 0-5");
		}
		this.hasStalactite = map.getBoolean("hasStalactite", false);
	}

	@Override
	protected boolean shouldTransformBlocksByDefault() {
		return true;
	}

	@Override
	protected Direction getDefaultOriginSide(List<StructurePlacementEdge> edges) {
		return Direction.DOWN;
	}

	@Override
	protected void serialize0(ConfigurationSection map) {
		map.set("block", block);
		map.set("floorToCeilingSearchRange", floorToCeilingSearchRange);
		map.set("maxColumnRadiusToCaveHeightRatio", maxColumnRadiusToCaveHeightRatio);
		map.set("minColumnRadius", minColumnRadius);
		map.set("maxColumnRadius", maxColumnRadius);
		map.set("minStalagmiteBluntness", minStalagmiteBluntness);
		map.set("maxStalagmiteBluntness", maxStalagmiteBluntness);
		map.set("minStalactiteBluntness", minStalactiteBluntness);
		map.set("maxStalactiteBluntness", maxStalactiteBluntness);
		map.set("minHeightScale", minHeightScale);
		map.set("maxHeightScale", maxHeightScale);
		map.set("minWindSpeed", minWindSpeed);
		map.set("maxWindSpeed", maxWindSpeed);
		map.set("minRadiusForWind", minRadiusForWind);
		map.set("minBluntnessForWind", minBluntnessForWind);
		map.set("hasStalactite", hasStalactite);
	}

	@Override
	public void place(CaveGenContext ctx, BlockVector3 pos, boolean force) throws WorldEditException {
		pos = pos.add(0, 1, 0);

		if (!force && !canReplace(ctx, ctx.getBlock(pos))) {
			return;
		}

		BlockVector3 ceilingPos = pos;
		for (int dy = 1; dy < floorToCeilingSearchRange * 2 && canReplace(ctx, ctx.getBlock(ceilingPos)); dy++) {
			ceilingPos = ceilingPos.add(0, 1, 0);
		}
		boolean hasCeiling = canPlaceOn(ctx, ctx.getBlock(ceilingPos));
		BlockVector3 floorPos = pos;
		for (int dy = 1; dy < floorToCeilingSearchRange * 2 && canReplace(ctx, ctx.getBlock(floorPos)); dy++) {
			floorPos = floorPos.add(0, -1, 0);
		}
		boolean hasFloor = canPlaceOn(ctx, ctx.getBlock(ceilingPos));

		if (!force && !hasFloor || !hasCeiling) {
			return;
		}

		int height = ceilingPos.getY() - floorPos.getY() - 1;
		if (height < 4) {
			if (force) {
				height = 4;
			} else {
				return;
			}
		}

		pos = BlockVector3.at(pos.getX(), floorPos.getY() + 1 + ctx.rand.nextInt(ceilingPos.getY() - floorPos.getY() + 1), pos.getZ());

		int r = (int)(height * maxColumnRadiusToCaveHeightRatio);
		int maxRadius;
		if (r < minColumnRadius) {
			maxRadius = minColumnRadius;
		} else if (r > maxColumnRadius) {
			maxRadius = maxColumnRadius;
		} else {
			maxRadius = r;
		}
		int radius = minColumnRadius + ctx.rand.nextInt(maxRadius - minColumnRadius + 1);

		DripstoneGenerator stalagmiteGenerator = createGenerator(ctx, pos.withY(ceilingPos.getY() - 1), false, radius);
		DripstoneGenerator stalactiteGenerator = createGenerator(ctx, pos.withY(floorPos.getY() + 1), true, radius);
		WindModifier windModifier;
		if (stalagmiteGenerator.shouldGenerateWind() && stalactiteGenerator.shouldGenerateWind()) {
			windModifier = new WindModifier(pos.getY(), ctx);
		} else {
			windModifier = new WindModifier();
		}
		boolean roomForStalagmite = stalagmiteGenerator.adjustScale(ctx, windModifier);
		boolean roomForStalactite = stalactiteGenerator.adjustScale(ctx, windModifier);

		if ((force || roomForStalagmite) && hasStalactite) {
			stalagmiteGenerator.generate(ctx, windModifier);
		}
		if (force || roomForStalactite) {
			stalactiteGenerator.generate(ctx, windModifier);
		}
	}

	private DripstoneGenerator createGenerator(CaveGenContext ctx, BlockVector3 pos, boolean isStalagmite, int scale) {
		float bluntness;
		if (isStalagmite) {
			bluntness = minStalagmiteBluntness + ctx.rand.nextFloat() * (maxStalagmiteBluntness - minStalagmiteBluntness);
		} else {
			bluntness = minStalactiteBluntness + ctx.rand.nextFloat() * (maxStalactiteBluntness - minStalactiteBluntness);
		}
		float heightScale = minHeightScale + ctx.rand.nextFloat() * (maxHeightScale - minHeightScale);
		return new DripstoneGenerator(pos, isStalagmite, scale, bluntness, heightScale);
	}

	private boolean canGenerateBase(CaveGenContext ctx, BlockVector3 pos, int radius) {
		if (canReplace(ctx, ctx.getBlock(pos))) {
			return false;
		}
		final float arcLength = 6;
		float deltaAngle = arcLength / radius;
		for (float angle = 0; angle < Math.PI * 2; angle += deltaAngle) {
			int x = (int)(Math.cos(angle) * radius);
			int z = (int)(Math.sin(angle) * radius);
			if (canReplace(ctx, ctx.getBlock(pos.add(x, 0, z)))) {
				return false;
			}
		}
		return true;
	}

	protected static double getHeightFromHDistance(double hDistance, double scale, double heightScale, double bluntness) {
		if (hDistance < bluntness) {
			hDistance = bluntness;
		}
		final double inputScale = 0.384;
		double scaledHDistance = hDistance / scale * inputScale;
		double temp1 = 0.75 * Math.pow(scaledHDistance, 4.0 / 3);
		double temp2 = Math.pow(scaledHDistance, 2.0 / 3);
		double temp3 = 1.0 / 3 * Math.log(scaledHDistance);
		double height = heightScale * (temp1 - temp2 - temp3);
		height = Math.max(height, 0);
		return height / inputScale * scale;
	}

	private final class DripstoneGenerator {
		private BlockVector3 pos;
		private final boolean isStalagmite;
		private int radius;
		private final double bluntness;
		private final double heightScale;

		private DripstoneGenerator(BlockVector3 pos, boolean isStalagmite, int radius, double bluntness, double heightScale) {
			this.pos = pos;
			this.isStalagmite = isStalagmite;
			this.radius = radius;
			this.bluntness = bluntness;
			this.heightScale = heightScale;
		}

		private int getTotalHeight() {
			return this.getHeight(0.0f);
		}

		private int getStalactiteBottomY() {
			if (this.isStalagmite) {
				return this.pos.getY();
			}
			return this.pos.getY() - this.getTotalHeight();
		}

		private int getStalagmiteTopY() {
			if (!this.isStalagmite) {
				return this.pos.getY();
			}
			return this.pos.getY() + this.getTotalHeight();
		}

		private boolean adjustScale(CaveGenContext ctx, WindModifier wind) {
			int originalScale = this.radius;
			while (this.radius > 1) {
				BlockVector3 pos = this.pos;
				int heightToCheck = Math.min(10, this.getTotalHeight());
				for (int i = 0; i < heightToCheck; i++) {
					if (canGenerateBase(ctx, wind.modify(pos), this.radius)) {
						this.pos = pos;
						return true;
					}
					pos = pos.add(0, this.isStalagmite ? -1 : 1, 0);
				}
				this.radius /= 2;
			}
			this.radius = originalScale;
			return false;
		}

		private int getHeight(float hDistance) {
			return (int) getHeightFromHDistance(hDistance, this.radius, this.heightScale, this.bluntness);
		}

		private void generate(CaveGenContext ctx, WindModifier wind) {
			for (int x = -this.radius; x <= this.radius; x++) {
				for (int z = -this.radius; z <= this.radius; z++) {
					float hDistance = (float)Math.sqrt(x * x + z * z);
					if (hDistance <= this.radius) {
						int localHeight = this.getHeight(hDistance);
						if (localHeight > 0) {
							if (ctx.rand.nextFloat() < 0.2) {
								localHeight *= 0.8f + ctx.rand.nextFloat() * 0.2f;
							}
							BlockVector3 pos = this.pos.add(x, 0, z);
							boolean placedBlock = false;
							for (int y = 0; y < localHeight; ++y) {
								BlockVector3 windModifiedPos = wind.modify(pos);
								if (canReplace(ctx, ctx.getBlock(windModifiedPos))) {
									placedBlock = true;
									ctx.setBlock(windModifiedPos, block);
								} else if (placedBlock && !canReplace(ctx, ctx.getBlock(windModifiedPos))) {
									break;
								}
								pos = pos.add(0, this.isStalagmite ? 1 : -1, 0);
							}
						}
					}
				}
			}
		}

		private boolean shouldGenerateWind() {
			return this.radius >= minRadiusForWind && this.bluntness >= minBluntnessForWind;
		}
	}

	private final class WindModifier {
		private final int y;
		private final Vector3 wind;

		private WindModifier(int y, CaveGenContext ctx) {
			this.y = y;
			float speedX = minWindSpeed + ctx.rand.nextFloat() * (maxWindSpeed - minWindSpeed);
			float speedZ = minWindSpeed + ctx.rand.nextFloat() * (maxWindSpeed - minWindSpeed);
			this.wind = Vector3.at(ctx.rand.nextBoolean() ? -speedX : speedX, 0.0, ctx.rand.nextBoolean() ? -speedZ : speedZ);
		}

		private WindModifier() {
			this.y = 0;
			this.wind = null;
		}

		private BlockVector3 modify(BlockVector3 pos) {
			if (this.wind == null) {
				return pos;
			}
			int dy = this.y - pos.getY();
			Vector3 delta = this.wind.multiply(dy);
			return pos.add((int)Math.round(delta.getX()), 0, (int)Math.round(delta.getZ()));
		}
	}
}
