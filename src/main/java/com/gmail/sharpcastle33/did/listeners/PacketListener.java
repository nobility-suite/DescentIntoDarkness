package com.gmail.sharpcastle33.did.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.Biomes;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PacketListener {
	private static final Class<?> DYNAMIC_REGISTRY_MANAGER_IMPL;
	private static final Object REGISTRY_MANAGER_NETWORK_CODEC;
	private static final Object JSON_OPS;
	private static final MethodAccessor CODEC_PARSE;
	private static final MethodAccessor CODEC_ENCODE_START;
	private static final MethodAccessor DATA_RESULT_RESULT;
	private static final MethodAccessor DATA_RESULT_ERROR;
	private static final MethodAccessor PARTIAL_RESULT_MESSAGE;

	private static final Class<?> DIMENSION_TYPE;
	private static final Object NETHER_DIMENSION;
	private static final Object END_DIMENSION;

	static {
		DYNAMIC_REGISTRY_MANAGER_IMPL = MinecraftReflection.getMinecraftClass("IRegistryCustom$Dimension");
		Field registryManagerNetworkCodecField = FuzzyReflection.fromClass(DYNAMIC_REGISTRY_MANAGER_IMPL).getFieldByType("[.\\w$]*com\\.mojang\\.serialization\\.Codec");
		Class<?> codecClass = registryManagerNetworkCodecField.getType();
		REGISTRY_MANAGER_NETWORK_CODEC = Accessors.getFieldAccessor(registryManagerNetworkCodecField).get(null);
		Class<?> decoderClass;
		Class<?> encoderClass;
		Class<?> dynamicOpsClass;
		Class<?> jsonOpsClass;
		Class<?> dataResultClass;
		Class<?> partialResultClass;
		try {
			String serializationPackage = codecClass.getPackage().getName();
			decoderClass = Class.forName(serializationPackage + ".Decoder");
			encoderClass = Class.forName(serializationPackage + ".Encoder");
			dynamicOpsClass = Class.forName(serializationPackage + ".DynamicOps");
			jsonOpsClass = Class.forName(serializationPackage + ".JsonOps");
			dataResultClass = Class.forName(serializationPackage + ".DataResult");
			partialResultClass = Class.forName(serializationPackage + ".DataResult$PartialResult");
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
		JSON_OPS = Accessors.getFieldAccessor(FuzzyReflection.fromClass(jsonOpsClass).getFieldByName("INSTANCE")).get(null);
		CODEC_PARSE = Accessors.getMethodAccessor(FuzzyReflection.fromClass(decoderClass).getMethod(FuzzyMethodContract.newBuilder().nameExact("parse").returnTypeExact(dataResultClass).parameterExactArray(dynamicOpsClass, Object.class).build()));
		CODEC_ENCODE_START = Accessors.getMethodAccessor(FuzzyReflection.fromClass(encoderClass).getMethod(FuzzyMethodContract.newBuilder().nameExact("encodeStart").returnTypeExact(dataResultClass).parameterExactArray(dynamicOpsClass, Object.class).build()));
		DATA_RESULT_RESULT = Accessors.getMethodAccessor(FuzzyReflection.fromClass(dataResultClass).getMethod(FuzzyMethodContract.newBuilder().nameExact("result").returnTypeExact(Optional.class).parameterCount(0).build()));
		DATA_RESULT_ERROR = Accessors.getMethodAccessor(FuzzyReflection.fromClass(dataResultClass).getMethod(FuzzyMethodContract.newBuilder().nameExact("error").returnTypeExact(Optional.class).parameterCount(0).build()));
		PARTIAL_RESULT_MESSAGE = Accessors.getMethodAccessor(FuzzyReflection.fromClass(partialResultClass).getMethod(FuzzyMethodContract.newBuilder().nameExact("message").returnTypeExact(String.class).parameterCount(0).build()));

		DIMENSION_TYPE = MinecraftReflection.getMinecraftClass("DimensionManager");
		NETHER_DIMENSION = Accessors.getFieldAccessor(FuzzyReflection.fromClass(DIMENSION_TYPE, true).getFieldByName("THE_NETHER_IMPL"), true).get(null);
		END_DIMENSION = Accessors.getFieldAccessor(FuzzyReflection.fromClass(DIMENSION_TYPE, true).getFieldByName("THE_END_IMPL"), true).get(null);
	}

	private static JsonObject registryManagerToJson(Object registryManager) {
		Object dataResult = CODEC_ENCODE_START.invoke(REGISTRY_MANAGER_NETWORK_CODEC, JSON_OPS, registryManager);
		Optional<?> error = (Optional<?>) DATA_RESULT_ERROR.invoke(dataResult);
		if (error.isPresent()) {
			throw new IllegalArgumentException("Unable to convert registryManager to json: " + PARTIAL_RESULT_MESSAGE.invoke(error.get()));
		}
		Optional<?> result = (Optional<?>) DATA_RESULT_RESULT.invoke(dataResult);
		assert result.isPresent();
		return ((JsonElement) result.get()).getAsJsonObject();
	}

	private static Object jsonToRegistryManager(JsonObject json) {
		Object dataResult = CODEC_PARSE.invoke(REGISTRY_MANAGER_NETWORK_CODEC, JSON_OPS, json);
		Optional<?> error = (Optional<?>) DATA_RESULT_ERROR.invoke(dataResult);
		if (error.isPresent()) {
			throw new IllegalArgumentException("Unable to convert json to registryManager: " + PARTIAL_RESULT_MESSAGE.invoke(error.get()));
		}
		Optional<?> result = (Optional<?>) DATA_RESULT_RESULT.invoke(dataResult);
		assert result.isPresent();
		return result.get();
	}

	public static void register() {
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

		protocolManager.addPacketListener(new PacketAdapter(PacketAdapter.params(DescentIntoDarkness.instance, PacketType.Play.Server.LOGIN)) {
			@Override
			public void onPacketSending(PacketEvent event) {
				Biomes.addNotifiedPlayer(event.getPlayer().getUniqueId());
				event.getPacket().getModifier().withType(DYNAMIC_REGISTRY_MANAGER_IMPL).modify(0, registryManager -> {
					JsonObject json = registryManagerToJson(registryManager);
					JsonArray biomes = json.getAsJsonObject("minecraft:worldgen/biome").getAsJsonArray("value");
					Set<String> vanillaBiomes = new HashSet<>();
					for (JsonElement vanillaBiomeElem : biomes) {
						JsonObject vanillaBiome = vanillaBiomeElem.getAsJsonObject();
						String vanillaBiomeName = vanillaBiome.getAsJsonPrimitive("name").getAsString();
						vanillaBiomes.add(Biomes.normalize(vanillaBiomeName));
						if (Biomes.isCustomBiome(vanillaBiomeName)) {
							vanillaBiome.add("element", Biomes.getBiomeJson(vanillaBiomeName));
						}
					}

					for (String customBiome : Biomes.getCustomBiomes()) {
						customBiome = Biomes.normalize(customBiome);
						if (!vanillaBiomes.contains(customBiome)) {
							JsonObject biomeJson = new JsonObject();
							biomeJson.addProperty("name", customBiome);
							biomeJson.addProperty("id", Biomes.getRawId(customBiome));
							biomeJson.add("element", Biomes.getBiomeJson(customBiome));
							biomes.add(biomeJson);
						}
					}
					return jsonToRegistryManager(json);
				});
			}
		});

		protocolManager.addPacketListener(new PacketAdapter(PacketAdapter.params(DescentIntoDarkness.instance, PacketType.Play.Server.MAP_CHUNK)) {
			@Override
			public void onPacketSending(PacketEvent event) {
				event.getPacket().getIntegerArrays().modify(0, biomes -> {
					if (biomes == null) {
						return null;
					}
					if (!Biomes.isPlayerNotified(event.getPlayer().getUniqueId())) {
						return biomes;
					}
					CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveForPlayer(event.getPlayer());
					if (cave == null) {
						return biomes;
					}
					Arrays.fill(biomes, Biomes.getRawId(cave.getStyle().getBiome()));
					return biomes;
				});
			}
		});

		protocolManager.addPacketListener(new PacketAdapter(PacketAdapter.params(DescentIntoDarkness.instance, PacketType.Play.Server.RESPAWN, PacketType.Play.Server.LOGIN)) {
			@Override
			public void onPacketSending(PacketEvent event) {
				event.getPacket().getModifier().withType(DIMENSION_TYPE).modify(0, dim -> {
					CaveTracker cave = DescentIntoDarkness.instance.getCaveTrackerManager().getCaveForPlayer(event.getPlayer());
					if (cave != null) {
						return cave.getStyle().isNether() ? NETHER_DIMENSION : END_DIMENSION;
					} else {
						return dim;
					}
				});
			}
		});
	}
}
