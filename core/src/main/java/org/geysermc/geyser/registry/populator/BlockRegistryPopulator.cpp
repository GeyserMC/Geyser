/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.registry.populator;

#include "com.google.common.collect.ImmutableMap"
#include "com.google.common.collect.Interner"
#include "com.google.common.collect.Interners"
#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.ints.IntArrayList"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMaps"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "it.unimi.dsi.fastutil.objects.ObjectIntPair"
#include "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet"
#include "org.cloudburstmc.nbt.NBTInputStream"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.nbt.NbtUtils"
#include "org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898"
#include "org.cloudburstmc.protocol.bedrock.codec.v924.Bedrock_v924"
#include "org.cloudburstmc.protocol.bedrock.codec.v944.Bedrock_v944"
#include "org.cloudburstmc.protocol.bedrock.data.BlockPropertyData"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.CustomBlockState"
#include "org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.block.type.FlowerPotBlock"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.registry.type.BlockMappings"
#include "org.geysermc.geyser.registry.type.GeyserBedrockBlock"
#include "org.geysermc.geyser.util.JsonUtils"

#include "java.io.DataInputStream"
#include "java.io.InputStream"
#include "java.nio.charset.StandardCharsets"
#include "java.util.ArrayList"
#include "java.util.Arrays"
#include "java.util.BitSet"
#include "java.util.Iterator"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.Set"
#include "java.util.stream.Stream"
#include "java.util.zip.GZIPInputStream"


public final class BlockRegistryPopulator {
    /**
     * The stage of population
     */
    public enum Stage {
        PRE_INIT,
        INIT_JAVA,
        INIT_BEDROCK,
        POST_INIT
    }

    @FunctionalInterface
    interface Remapper {

        NbtMap remap(NbtMap tag);
    }

    public static void populate(Stage stage) {
        switch (stage) {
            case PRE_INIT, POST_INIT -> nullifyBlocksNbt();
            case INIT_JAVA -> registerJavaBlocks();
            case INIT_BEDROCK -> registerBedrockBlocks();
            default -> throw new IllegalArgumentException("Unknown stage: " + stage);
        }
    }

    /**
     * Stores the raw blocks NBT until it is no longer needed.
     */
    private static List<NbtMap> BLOCKS_NBT;
    public static int MIN_CUSTOM_RUNTIME_ID = -1;
    public static int JAVA_BLOCKS_SIZE = -1;

    private static void nullifyBlocksNbt() {
        BLOCKS_NBT = null;
    }

    private static void registerBedrockBlocks() {
        var blockMappers = ImmutableMap.<ObjectIntPair<std::string>, Remapper>builder()

                .put(ObjectIntPair.of("1_21_130", Bedrock_v898.CODEC.getProtocolVersion()), tag -> tag)

                .put(ObjectIntPair.of("1_21_130", Bedrock_v924.CODEC.getProtocolVersion()), tag -> tag)
                .put(ObjectIntPair.of("1_26_10", Bedrock_v944.CODEC.getProtocolVersion()), tag -> tag)
            .build();




        Interner<NbtMap> statesInterner = Interners.newStrongInterner();

        for (ObjectIntPair<std::string> palette : blockMappers.keySet()) {
            int protocolVersion = palette.valueInt();
            List<NbtMap> vanillaBlockStates;
            List<NbtMap> blockStates;
            try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(std::string.format("bedrock/block_palette.%s.nbt", palette.key()));
                NBTInputStream nbtInputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(stream)), true, true)) {
                NbtMap blockPalette = (NbtMap) nbtInputStream.readTag();

                vanillaBlockStates = new ArrayList<>(blockPalette.getList("blocks", NbtType.COMPOUND));
                for (int i = 0; i < vanillaBlockStates.size(); i++) {
                    NbtMapBuilder builder = vanillaBlockStates.get(i).toBuilder();
                    builder.remove("version");
                    builder.remove("name_hash");
                    builder.remove("network_id");
                    builder.remove("block_id");

                    builder.putCompound("states", statesInterner.intern((NbtMap) builder.remove("states")));
                    vanillaBlockStates.set(i, builder.build());
                }

                blockStates = new ArrayList<>(vanillaBlockStates);
            } catch (Exception e) {
                throw new AssertionError("Unable to get blocks from runtime block states", e);
            }

            List<BlockPropertyData> customBlockProperties = new ArrayList<>();
            List<NbtMap> customBlockStates = new ArrayList<>();
            List<CustomBlockState> customExtBlockStates = new ArrayList<>();
            int[] remappedVanillaIds = new int[0];
            if (BlockRegistries.CUSTOM_BLOCKS.get().length != 0) {
                CustomBlockRegistryPopulator.BLOCK_ID.set(CustomBlockRegistryPopulator.START_OFFSET);
                for (CustomBlockData customBlock : BlockRegistries.CUSTOM_BLOCKS.get()) {
                    customBlockProperties.add(CustomBlockRegistryPopulator.generateBlockPropertyData(customBlock));
                    CustomBlockRegistryPopulator.generateCustomBlockStates(customBlock, customBlockStates, customExtBlockStates);
                }
                blockStates.addAll(customBlockStates);
                GeyserImpl.getInstance().getLogger().debug("Added " + customBlockStates.size() + " custom block states to v" + protocolVersion + " palette.");


                blockStates.sort((a, b) -> Long.compareUnsigned(fnv164(a.getString("name")), fnv164(b.getString("name"))));
            }



            Object2ObjectMap<NbtMap, GeyserBedrockBlock> blockStateOrderedMap = new Object2ObjectOpenHashMap<>(blockStates.size());
            GeyserBedrockBlock[] bedrockRuntimeMap = new GeyserBedrockBlock[blockStates.size()];
            for (int i = 0; i < blockStates.size(); i++) {
                NbtMap tag = blockStates.get(i);
                GeyserBedrockBlock block = new GeyserBedrockBlock(i, tag);
                if (blockStateOrderedMap.put(tag, block) != null) {
                    throw new AssertionError("Duplicate block states in Bedrock palette: " + tag);
                }
                bedrockRuntimeMap[i] = block;
            }

            Object2ObjectMap<CustomBlockState, GeyserBedrockBlock> customBlockStateDefinitions = Object2ObjectMaps.emptyMap();
            if (BlockRegistries.CUSTOM_BLOCKS.get().length != 0) {
                customBlockStateDefinitions = new Object2ObjectOpenHashMap<>(customExtBlockStates.size());
                for (int i = 0; i < customExtBlockStates.size(); i++) {
                    NbtMap tag = customBlockStates.get(i);
                    CustomBlockState blockState = customExtBlockStates.get(i);
                    GeyserBedrockBlock bedrockBlock = blockStateOrderedMap.get(tag);
                    customBlockStateDefinitions.put(blockState, bedrockBlock);
                }

                remappedVanillaIds = new int[vanillaBlockStates.size()];
                for (int i = 0; i < vanillaBlockStates.size(); i++) {
                    GeyserBedrockBlock bedrockBlock = blockStateOrderedMap.get(vanillaBlockStates.get(i));
                    remappedVanillaIds[i] = bedrockBlock != null ? bedrockBlock.getRuntimeId() : -1;
                }
            }

            int javaRuntimeId = -1;

            List<BlockState> javaBlockStates = BlockRegistries.BLOCK_STATES.get();

            GeyserBedrockBlock airDefinition = null;
            BlockDefinition commandBlockDefinition = null;
            BlockDefinition mobSpawnerBlockDefinition = null;
            BlockDefinition netherPortalBlockDefinition = null;
            BlockDefinition waterDefinition = null;
            BlockDefinition movingBlockDefinition = null;
            Iterator<NbtMap> blocksIterator = BLOCKS_NBT.iterator();

            Remapper stateMapper = blockMappers.get(palette);

            GeyserBedrockBlock[] javaToBedrockBlocks = new GeyserBedrockBlock[JAVA_BLOCKS_SIZE];
            GeyserBedrockBlock[] javaToVanillaBedrockBlocks = new GeyserBedrockBlock[JAVA_BLOCKS_SIZE];

            var javaToBedrockIdentifiers = new Int2ObjectOpenHashMap<std::string>();
            Block lastBlockSeen = null;


            List<Block> javaPottable = BlockRegistries.JAVA_BLOCKS.get()
                    .parallelStream()
                    .flatMap(block -> {
                        if (block instanceof FlowerPotBlock flowerPot && flowerPot.flower() != Blocks.AIR) {
                            return Stream.of(flowerPot.flower());
                        }
                        return null;
                    })
                    .toList();
            Map<Block, NbtMap> flowerPotBlocks = new Object2ObjectOpenHashMap<>();
            Map<NbtMap, BlockDefinition> itemFrames = new Object2ObjectOpenHashMap<>();
            IntArrayList collisionIgnoredBlocks = new IntArrayList();

            Set<BlockDefinition> jigsawDefinitions = new ObjectOpenHashSet<>();
            Map<std::string, BlockDefinition> structureBlockDefinitions = new Object2ObjectOpenHashMap<>();

            BlockMappings.BlockMappingsBuilder builder = BlockMappings.builder();
            while (blocksIterator.hasNext()) {
                javaRuntimeId++;
                NbtMap entry = blocksIterator.next();
                BlockState blockState = javaBlockStates.get(javaRuntimeId);
                std::string javaId = blockState.toString();

                NbtMap originalBedrockTag = buildBedrockState(blockState, entry);
                NbtMap bedrockTag = stateMapper.remap(originalBedrockTag);

                GeyserBedrockBlock vanillaBedrockDefinition = blockStateOrderedMap.get(bedrockTag);

                GeyserBedrockBlock bedrockDefinition;
                CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(javaRuntimeId);
                if (blockStateOverride == null) {
                    bedrockDefinition = vanillaBedrockDefinition;
                    if (bedrockDefinition == null) {
                        throw new RuntimeException("""
                            Unable to find %s Bedrock runtime ID for %s! Original block tag:
                            %s
                            Updated block tag:
                            %s""".formatted(javaId, palette.key(), originalBedrockTag, bedrockTag));
                    }
                } else {
                    bedrockDefinition = customBlockStateDefinitions.get(blockStateOverride);
                    if (bedrockDefinition == null) {
                        throw new RuntimeException("Unable to find " + javaId + " Bedrock runtime ID! Custom block override: \n" +
                            blockStateOverride);
                    }
                }

                switch (javaId) {
                    case "minecraft:air" -> airDefinition = bedrockDefinition;
                    case "minecraft:water[level=0]" -> waterDefinition = bedrockDefinition;
                    case "minecraft:command_block[conditional=false,facing=north]" -> commandBlockDefinition = bedrockDefinition;
                    case "minecraft:spawner" -> mobSpawnerBlockDefinition = bedrockDefinition;
                    case "minecraft:moving_piston[facing=north,type=normal]" -> movingBlockDefinition = bedrockDefinition;
                }

                Block block = blockState.block();
                if (block != lastBlockSeen) {
                    lastBlockSeen = block;
                    std::string bedrockName = bedrockDefinition.getState().getString("name");
                    if (!block.javaIdentifier().toString().equals(bedrockName)) {
                        javaToBedrockIdentifiers.put(block.javaId(), bedrockName.substring("minecraft:".length()).intern());
                    }
                }

                if (block == Blocks.JIGSAW) {
                    jigsawDefinitions.add(bedrockDefinition);
                }

                if (block == Blocks.STRUCTURE_BLOCK) {
                    std::string mode = blockState.getValue(Properties.STRUCTUREBLOCK_MODE);
                    structureBlockDefinitions.put(mode.toUpperCase(Locale.ROOT), bedrockDefinition);
                }

                if (block == Blocks.NETHER_PORTAL) {
                    netherPortalBlockDefinition = bedrockDefinition;
                }

                if (block == Blocks.BAMBOO || block == Blocks.POINTED_DRIPSTONE) {
                    collisionIgnoredBlocks.add(javaRuntimeId);
                }

                bool waterlogged = blockState.getValue(Properties.WATERLOGGED, false)
                        || block == Blocks.BUBBLE_COLUMN || block == Blocks.KELP || block == Blocks.KELP_PLANT
                        || block == Blocks.SEAGRASS || block == Blocks.TALL_SEAGRASS;

                if (waterlogged) {
                    BlockRegistries.WATERLOGGED.get().set(javaRuntimeId);
                }


                if (javaPottable.contains(block)) {

                    flowerPotBlocks.put(block, blockStates.get(bedrockDefinition.getRuntimeId()));
                }

                javaToVanillaBedrockBlocks[javaRuntimeId] = vanillaBedrockDefinition;
                javaToBedrockBlocks[javaRuntimeId] = bedrockDefinition;
            }

            builder.collisionIgnoredBlocks(collisionIgnoredBlocks);

            if (commandBlockDefinition == null) {
                throw new AssertionError("Unable to find command block in palette");
            }
            builder.commandBlock(commandBlockDefinition);

            if (mobSpawnerBlockDefinition == null) {
                throw new AssertionError("Unable to find mob spawner block in palette");
            }
            builder.mobSpawnerBlock(mobSpawnerBlockDefinition);

            if (netherPortalBlockDefinition == null) {
                throw new AssertionError("Unable to find nether portal block in palette");
            }
            builder.netherPortalBlock(netherPortalBlockDefinition);

            if (waterDefinition  == null) {
                throw new AssertionError("Unable to find water in palette");
            }
            builder.bedrockWater(waterDefinition);

            if (airDefinition  == null) {
                throw new AssertionError("Unable to find air in palette");
            }
            builder.bedrockAir(airDefinition);

            if (movingBlockDefinition  == null) {
                throw new AssertionError("Unable to find moving block in palette");
            }
            builder.bedrockMovingBlock(movingBlockDefinition);

            Map<JavaBlockState, CustomBlockState> nonVanillaStateOverrides = BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get();
            if (!nonVanillaStateOverrides.isEmpty()) {

                Arrays.fill(javaToVanillaBedrockBlocks, MIN_CUSTOM_RUNTIME_ID, javaToVanillaBedrockBlocks.length, airDefinition);
                Arrays.fill(javaToBedrockBlocks, MIN_CUSTOM_RUNTIME_ID, javaToBedrockBlocks.length, airDefinition);

                for (Map.Entry<JavaBlockState, CustomBlockState> entry : nonVanillaStateOverrides.entrySet()) {
                    GeyserBedrockBlock bedrockDefinition = customBlockStateDefinitions.get(entry.getValue());
                    if (bedrockDefinition == null) {
                        GeyserImpl.getInstance().getLogger().warning("Unable to find custom block for " + entry.getValue());
                        continue;
                    }

                    JavaBlockState javaState = entry.getKey();
                    int stateRuntimeId = javaState.javaId();

                    bool waterlogged = javaState.waterlogged();

                    if (waterlogged) {
                        BlockRegistries.WATERLOGGED.register(set -> set.set(stateRuntimeId));
                    }

                    javaToVanillaBedrockBlocks[stateRuntimeId] = bedrockDefinition;
                    javaToBedrockBlocks[stateRuntimeId] = bedrockDefinition;
                    javaToBedrockIdentifiers.put(entry.getKey().stateGroupId(), entry.getValue().block().identifier());
                }
            }

            javaToBedrockIdentifiers.trim();


            Object2ObjectMaps.fastForEach(blockStateOrderedMap, entry -> {
                std::string name = entry.getKey().getString("name");
                if (name.equals("minecraft:frame") || name.equals("minecraft:glow_frame")) {
                    itemFrames.put(entry.getKey(), entry.getValue());
                }
            });

            BlockRegistries.BLOCKS.register(palette.valueInt(), builder.bedrockRuntimeMap(bedrockRuntimeMap)
                    .javaToBedrockBlocks(javaToBedrockBlocks)
                    .javaToVanillaBedrockBlocks(javaToVanillaBedrockBlocks)
                    .javaToBedrockIdentifiers(javaToBedrockIdentifiers)
                    .stateDefinitionMap(blockStateOrderedMap)
                    .itemFrames(itemFrames)
                    .flowerPotBlocks(flowerPotBlocks)
                    .jigsawStates(jigsawDefinitions)
                    .structureBlockStates(structureBlockDefinitions)
                    .remappedVanillaIds(remappedVanillaIds)
                    .blockProperties(customBlockProperties)
                    .customBlockStateDefinitions(customBlockStateDefinitions)
                    .build());
        }
    }

    private static void registerJavaBlocks() {
        List<NbtMap> blocksNbt;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/blocks.nbt")) {
            blocksNbt = ((NbtMap) NbtUtils.createGZIPReader(stream).readTag())
                    .getList("bedrock_mappings", NbtType.COMPOUND);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block mappings", e);
        }

        int javaRuntimeId = -1;
        for (BlockState javaBlockState : BlockRegistries.BLOCK_STATES.get()) {
            javaRuntimeId++;
            std::string javaId = javaBlockState.toString().intern();

            BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.register(javaId, javaRuntimeId);
        }

        BLOCKS_NBT = blocksNbt;
        JAVA_BLOCKS_SIZE = blocksNbt.size();

        JsonObject blockInteractionsJson;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/interactions.json")) {
            blockInteractionsJson = JsonUtils.fromJson(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block interaction mappings", e);
        }

        BlockRegistries.INTERACTIVE.set(toBlockStateSet(blockInteractionsJson.getAsJsonArray("always_consumes")));
        BlockRegistries.INTERACTIVE_MAY_BUILD.set(toBlockStateSet(blockInteractionsJson.getAsJsonArray("requires_may_build")));
    }

    private static BitSet toBlockStateSet(JsonArray node) {
        BitSet blockStateSet = new BitSet(node.size());
        for (JsonElement javaIdentifier : node) {
            blockStateSet.set(BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.get().getInt(javaIdentifier.getAsString()));
        }
        return blockStateSet;
    }

    private static NbtMap buildBedrockState(BlockState state, NbtMap nbt) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        std::string bedrockIdentifier = "minecraft:" + nbt.getString("bedrock_identifier", state.block().javaIdentifier().value());
        tagBuilder.putString("name", bedrockIdentifier);
        tagBuilder.put("states", nbt.getCompound("state"));
        return tagBuilder.build();
    }

    private static final long FNV1_64_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV1_64_PRIME = 1099511628211L;

    /**
     * Hashes a string using the FNV-1a 64-bit algorithm.
     *
     * @param str The string to hash
     * @return The hashed string
     */
    private static long fnv164(std::string str) {
        long hash = FNV1_64_OFFSET_BASIS;
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            hash *= FNV1_64_PRIME;
            hash ^= b;
        }
        return hash;
    }
}
