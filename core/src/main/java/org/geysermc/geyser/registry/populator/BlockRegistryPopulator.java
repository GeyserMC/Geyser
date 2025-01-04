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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766;
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.FlowerPotBlock;
import org.geysermc.geyser.level.physics.PistonBehavior;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Populates the block registries.
 */
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
    private static int MIN_CUSTOM_RUNTIME_ID = -1;
    private static int JAVA_BLOCKS_SIZE = -1;

    private static void nullifyBlocksNbt() {
        BLOCKS_NBT = null;
    }

    private static void registerBedrockBlocks() {
        var blockMappers = ImmutableMap.<ObjectIntPair<String>, Remapper>builder()
                .put(ObjectIntPair.of("1_21_40", Bedrock_v748.CODEC.getProtocolVersion()), Conversion766_748::remapBlock)
                .put(ObjectIntPair.of("1_21_50", Bedrock_v766.CODEC.getProtocolVersion()), tag -> tag)
                .build();

        // We can keep this strong as nothing should be garbage collected
        // Safe to intern since Cloudburst NBT is immutable
        //noinspection UnstableApiUsage
        Interner<NbtMap> statesInterner = Interners.newStrongInterner();

        for (ObjectIntPair<String> palette : blockMappers.keySet()) {
            int protocolVersion = palette.valueInt();
            List<NbtMap> vanillaBlockStates;
            List<NbtMap> blockStates;
            try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(String.format("bedrock/block_palette.%s.nbt", palette.key()));
                NBTInputStream nbtInputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(stream)), true, true)) {
                NbtMap blockPalette = (NbtMap) nbtInputStream.readTag();

                vanillaBlockStates = new ArrayList<>(blockPalette.getList("blocks", NbtType.COMPOUND));
                for (int i = 0; i < vanillaBlockStates.size(); i++) {
                    NbtMapBuilder builder = vanillaBlockStates.get(i).toBuilder();
                    builder.remove("version"); // Remove all nbt tags which are not needed for differentiating states
                    builder.remove("name_hash"); // Quick workaround - was added in 1.19.20
                    builder.remove("network_id"); // Added in 1.19.80
                    builder.remove("block_id"); // Added in 1.20.60
                    //noinspection UnstableApiUsage
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
                    customBlockProperties.add(CustomBlockRegistryPopulator.generateBlockPropertyData(customBlock, protocolVersion));
                    CustomBlockRegistryPopulator.generateCustomBlockStates(customBlock, customBlockStates, customExtBlockStates);
                }
                blockStates.addAll(customBlockStates);
                GeyserImpl.getInstance().getLogger().debug("Added " + customBlockStates.size() + " custom block states to v" + protocolVersion + " palette.");

                // The palette is sorted by the FNV1 64-bit hash of the name
                blockStates.sort((a, b) -> Long.compareUnsigned(fnv164(a.getString("name")), fnv164(b.getString("name"))));
            }

            // New since 1.16.100 - find the block runtime ID by the order given to us in the block palette,
            // as we no longer send a block palette
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
            Int2ObjectMap<GeyserBedrockBlock> extendedCollisionBoxes = new Int2ObjectOpenHashMap<>();
            if (BlockRegistries.CUSTOM_BLOCKS.get().length != 0) {
                customBlockStateDefinitions = new Object2ObjectOpenHashMap<>(customExtBlockStates.size());
                for (int i = 0; i < customExtBlockStates.size(); i++) {
                    NbtMap tag = customBlockStates.get(i);
                    CustomBlockState blockState = customExtBlockStates.get(i);
                    GeyserBedrockBlock bedrockBlock = blockStateOrderedMap.get(tag);
                    customBlockStateDefinitions.put(blockState, bedrockBlock);

                    Set<Integer> extendedCollisionjavaIds = BlockRegistries.EXTENDED_COLLISION_BOXES.getOrDefault(blockState.block(), null);
                    if (extendedCollisionjavaIds != null) {
                        for (int javaId : extendedCollisionjavaIds) {
                            extendedCollisionBoxes.put(javaId, bedrockBlock);
                        }
                    }
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
            BlockDefinition waterDefinition = null;
            BlockDefinition movingBlockDefinition = null;
            Iterator<NbtMap> blocksIterator = BLOCKS_NBT.iterator();

            Remapper stateMapper = blockMappers.get(palette);

            GeyserBedrockBlock[] javaToBedrockBlocks = new GeyserBedrockBlock[JAVA_BLOCKS_SIZE];
            GeyserBedrockBlock[] javaToVanillaBedrockBlocks = new GeyserBedrockBlock[JAVA_BLOCKS_SIZE];

            var javaToBedrockIdentifiers = new Int2ObjectOpenHashMap<String>();
            Block lastBlockSeen = null;

            // Stream isn't ideal.
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

            Set<BlockDefinition> jigsawDefinitions = new ObjectOpenHashSet<>();
            Map<String, BlockDefinition> structureBlockDefinitions = new Object2ObjectOpenHashMap<>();

            BlockMappings.BlockMappingsBuilder builder = BlockMappings.builder();
            while (blocksIterator.hasNext()) {
                javaRuntimeId++;
                NbtMap entry = blocksIterator.next();
                BlockState blockState = javaBlockStates.get(javaRuntimeId);
                String javaId = blockState.toString();

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
                    String bedrockName = bedrockDefinition.getState().getString("name");
                    if (!block.javaIdentifier().toString().equals(bedrockName)) {
                        javaToBedrockIdentifiers.put(block.javaId(), bedrockName.substring("minecraft:".length()).intern());
                    }
                }

                if (block == Blocks.JIGSAW) {
                    jigsawDefinitions.add(bedrockDefinition);
                }

                if (block == Blocks.STRUCTURE_BLOCK) {
                    String mode = blockState.getValue(Properties.STRUCTUREBLOCK_MODE);
                    structureBlockDefinitions.put(mode.toUpperCase(Locale.ROOT), bedrockDefinition);
                }

                boolean waterlogged = blockState.getValue(Properties.WATERLOGGED, false)
                        || block == Blocks.BUBBLE_COLUMN || block == Blocks.KELP || block == Blocks.KELP_PLANT
                        || block == Blocks.SEAGRASS || block == Blocks.TALL_SEAGRASS;

                if (waterlogged) {
                    BlockRegistries.WATERLOGGED.get().set(javaRuntimeId);
                }

                // Get the tag needed for non-empty flower pots
                if (javaPottable.contains(block)) {
                    // Specifically NOT putIfAbsent - mangrove propagule breaks otherwise
                    flowerPotBlocks.put(block, blockStates.get(bedrockDefinition.getRuntimeId()));
                }

                javaToVanillaBedrockBlocks[javaRuntimeId] = vanillaBedrockDefinition;
                javaToBedrockBlocks[javaRuntimeId] = bedrockDefinition;
            }

            if (commandBlockDefinition == null) {
                throw new AssertionError("Unable to find command block in palette");
            }
            builder.commandBlock(commandBlockDefinition);

            if (mobSpawnerBlockDefinition == null) {
                throw new AssertionError("Unable to find mob spawner block in palette");
            }
            builder.mobSpawnerBlock(mobSpawnerBlockDefinition);

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
                // First ensure all non vanilla runtime IDs at minimum are air in case they aren't consecutive
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

                    boolean waterlogged = javaState.waterlogged();

                    if (waterlogged) {
                        BlockRegistries.WATERLOGGED.register(set -> set.set(stateRuntimeId));
                    }

                    javaToVanillaBedrockBlocks[stateRuntimeId] = bedrockDefinition; // TODO: Check this?
                    javaToBedrockBlocks[stateRuntimeId] = bedrockDefinition;
                    javaToBedrockIdentifiers.put(entry.getKey().stateGroupId(), entry.getValue().block().identifier());
                }
            }

            javaToBedrockIdentifiers.trim();

            // Loop around again to find all item frame runtime IDs
            Object2ObjectMaps.fastForEach(blockStateOrderedMap, entry -> {
                String name = entry.getKey().getString("name");
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
                    .extendedCollisionBoxes(extendedCollisionBoxes)
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

        JAVA_BLOCKS_SIZE = BlockRegistries.BLOCK_STATES.get().size();

        if (!BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().isEmpty()) {
            MIN_CUSTOM_RUNTIME_ID = BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().keySet().stream().min(Comparator.comparing(JavaBlockState::javaId)).orElseThrow().javaId();
            int maxCustomRuntimeID = BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().keySet().stream().max(Comparator.comparing(JavaBlockState::javaId)).orElseThrow().javaId();

            if (MIN_CUSTOM_RUNTIME_ID < blocksNbt.size()) {
                throw new RuntimeException("Non vanilla custom block state overrides runtime ID must start after the last vanilla block state (" + JAVA_BLOCKS_SIZE + ")");
            }

            JAVA_BLOCKS_SIZE = maxCustomRuntimeID + 1; // Runtime ids start at 0, so we need to add 1
        }

        int javaRuntimeId = -1;
        for (BlockState javaBlockState : BlockRegistries.BLOCK_STATES.get()) {
            javaRuntimeId++;
            String javaId = javaBlockState.toString().intern();

            BlockRegistries.JAVA_IDENTIFIER_TO_ID.register(javaId, javaRuntimeId);
        }

        if (!BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().isEmpty()) {
            IntSet usedNonVanillaRuntimeIDs = new IntOpenHashSet();

            for (JavaBlockState javaBlockState : BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().keySet()) {
                if (!usedNonVanillaRuntimeIDs.add(javaBlockState.javaId())) {
                    throw new RuntimeException("Duplicate runtime ID " + javaBlockState.javaId() + " for non vanilla Java block state " + javaBlockState.identifier());
                }

                String javaId = javaBlockState.identifier();
                int stateRuntimeId = javaBlockState.javaId();
                String pistonBehavior = javaBlockState.pistonBehavior();

                Block.Builder builder = Block.builder()
                        .destroyTime(javaBlockState.blockHardness())
                        .pushReaction(pistonBehavior == null ? PistonBehavior.NORMAL : PistonBehavior.getByName(pistonBehavior));
                if (!javaBlockState.canBreakWithHand()) {
                    builder.requiresCorrectToolForDrops();
                }
                String cleanJavaIdentifier = BlockUtils.getCleanIdentifier(javaBlockState.identifier());
                String pickItem = javaBlockState.pickItem();
                Block block = new Block(cleanJavaIdentifier, builder) {
                    @Override
                    public ItemStack pickItem(BlockState state) {
                        if (this.item == null) {
                            this.item = Registries.JAVA_ITEM_IDENTIFIERS.get(pickItem);
                            if (this.item == null) {
                                GeyserImpl.getInstance().getLogger().warning("We could not find item " + pickItem
                                        + " for getting the item for block " + javaBlockState.identifier());
                                this.item = Items.AIR;
                            }
                        }
                        return new ItemStack(this.item.javaId());
                    }
                };
                block.setJavaId(javaBlockState.stateGroupId());

                BlockRegistries.JAVA_BLOCKS.registerWithAnyIndex(javaBlockState.stateGroupId(), block, Blocks.AIR);
                BlockRegistries.JAVA_IDENTIFIER_TO_ID.register(javaId, stateRuntimeId);
                BlockRegistries.BLOCK_STATES.register(stateRuntimeId, new BlockState(block, stateRuntimeId));
            }
        }

        BLOCKS_NBT = blocksNbt;

        JsonNode blockInteractionsJson;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow("mappings/interactions.json")) {
            blockInteractionsJson = GeyserImpl.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block interaction mappings", e);
        }

        BlockRegistries.INTERACTIVE.set(toBlockStateSet((ArrayNode) blockInteractionsJson.get("always_consumes")));
        BlockRegistries.INTERACTIVE_MAY_BUILD.set(toBlockStateSet((ArrayNode) blockInteractionsJson.get("requires_may_build")));

        BlockRegistries.BLOCK_STATES.freeze();
    }

    private static BitSet toBlockStateSet(ArrayNode node) {
        BitSet blockStateSet = new BitSet(node.size());
        for (JsonNode javaIdentifier : node) {
            blockStateSet.set(BlockRegistries.JAVA_IDENTIFIER_TO_ID.get().getInt(javaIdentifier.textValue()));
        }
        return blockStateSet;
    }

    private static NbtMap buildBedrockState(BlockState state, NbtMap nbt) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        String bedrockIdentifier = "minecraft:" + nbt.getString("bedrock_identifier", state.block().javaIdentifier().value());
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
    private static long fnv164(String str) {
        long hash = FNV1_64_OFFSET_BASIS;
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            hash *= FNV1_64_PRIME;
            hash ^= b;
        }
        return hash;
    }
}
