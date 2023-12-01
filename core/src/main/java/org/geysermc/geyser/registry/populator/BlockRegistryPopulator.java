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
import it.unimi.dsi.fastutil.objects.*;
import org.cloudburstmc.blockstateupdater.BlockStateUpdater;
import org.cloudburstmc.blockstateupdater.BlockStateUpdater_1_20_10;
import org.cloudburstmc.blockstateupdater.BlockStateUpdater_1_20_30;
import org.cloudburstmc.blockstateupdater.BlockStateUpdater_1_20_40;
import org.cloudburstmc.blockstateupdater.BlockStateUpdater_1_20_50;
import org.cloudburstmc.blockstateupdater.util.tagupdater.CompoundTagUpdaterContext;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.physics.PistonBehavior;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.util.BlockUtils;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
        POST_INIT;
    }

    @FunctionalInterface
    private interface Remapper {

        NbtMap remap(NbtMap tag);

        static Remapper of(BlockStateUpdater... updaters) {
            CompoundTagUpdaterContext context = new CompoundTagUpdaterContext();
            for (BlockStateUpdater updater : updaters) {
                updater.registerUpdaters(context);
            }

            return tag -> {
                NbtMapBuilder updated = context.update(tag, 0).toBuilder();
                updated.remove("version"); // we already removed this, but the context adds it. remove it again.
                return updated.build();
            };
        }
    }

    public static void populate(Stage stage) {
        switch (stage) {
            case PRE_INIT, POST_INIT -> { nullifyBlocksNode(); }
            case INIT_JAVA -> { registerJavaBlocks(); }
            case INIT_BEDROCK -> { registerBedrockBlocks(); }
            default -> { throw new IllegalArgumentException("Unknown stage: " + stage); }
        }
    }

    /**
     * Stores the raw blocks JSON until it is no longer needed.
     */
    private static JsonNode BLOCKS_JSON;
    private static int MIN_CUSTOM_RUNTIME_ID = -1;
    private static int JAVA_BLOCKS_SIZE = -1;

    private static void nullifyBlocksNode() {
        BLOCKS_JSON = null;
    }

    private static void registerBedrockBlocks() {
        Remapper mapper594 = Remapper.of(BlockStateUpdater_1_20_10.INSTANCE);
        Remapper mapper618 = Remapper.of(BlockStateUpdater_1_20_10.INSTANCE, BlockStateUpdater_1_20_30.INSTANCE);
        Remapper mapper622 = Remapper.of(BlockStateUpdater_1_20_10.INSTANCE, BlockStateUpdater_1_20_30.INSTANCE, BlockStateUpdater_1_20_40.INSTANCE);
        Remapper mapper630 = Remapper.of(BlockStateUpdater_1_20_10.INSTANCE, BlockStateUpdater_1_20_30.INSTANCE, BlockStateUpdater_1_20_40.INSTANCE, BlockStateUpdater_1_20_50.INSTANCE);

        var blockMappers = ImmutableMap.<ObjectIntPair<String>, Remapper>builder()
                .put(ObjectIntPair.of("1_20_0", Bedrock_v589.CODEC.getProtocolVersion()), tag -> tag)
                .put(ObjectIntPair.of("1_20_10", Bedrock_v594.CODEC.getProtocolVersion()), mapper594)
                .put(ObjectIntPair.of("1_20_30", Bedrock_v618.CODEC.getProtocolVersion()), mapper618)
                .put(ObjectIntPair.of("1_20_40", Bedrock_v622.CODEC.getProtocolVersion()), mapper622)
                .put(ObjectIntPair.of("1_20_50", Bedrock_v630.CODEC.getProtocolVersion()), mapper630)
                .build();

        // We can keep this strong as nothing should be garbage collected
        // Safe to intern since Cloudburst NBT is immutable
        Interner<NbtMap> statesInterner = Interners.newStrongInterner();

        for (ObjectIntPair<String> palette : blockMappers.keySet()) {
            int protocolVersion = palette.valueInt();
            List<NbtMap> vanillaBlockStates;
            List<NbtMap> blockStates;
            try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResource(String.format("bedrock/block_palette.%s.nbt", palette.key()));
                NBTInputStream nbtInputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(stream)), true, true)) {
                NbtMap blockPalette = (NbtMap) nbtInputStream.readTag();

                vanillaBlockStates = new ArrayList<>(blockPalette.getList("blocks", NbtType.COMPOUND));
                for (int i = 0; i < vanillaBlockStates.size(); i++) {
                    NbtMapBuilder builder = vanillaBlockStates.get(i).toBuilder();
                    builder.remove("version"); // Remove all nbt tags which are not needed for differentiating states
                    builder.remove("name_hash"); // Quick workaround - was added in 1.19.20
                    builder.remove("network_id"); // Added in 1.19.80 - ????
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
                if (blockStateOrderedMap.containsKey(tag)) {
                    throw new AssertionError("Duplicate block states in Bedrock palette: " + tag);
                }
                GeyserBedrockBlock block = new GeyserBedrockBlock(i, tag);
                blockStateOrderedMap.put(tag, block);
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

            GeyserBedrockBlock airDefinition = null;
            BlockDefinition commandBlockDefinition = null;
            BlockDefinition mobSpawnerBlockDefinition = null;
            BlockDefinition waterDefinition = null;
            BlockDefinition movingBlockDefinition = null;
            Iterator<Map.Entry<String, JsonNode>> blocksIterator = BLOCKS_JSON.fields();

            Remapper stateMapper = blockMappers.get(palette);

            GeyserBedrockBlock[] javaToBedrockBlocks = new GeyserBedrockBlock[JAVA_BLOCKS_SIZE];
            GeyserBedrockBlock[] javaToVanillaBedrockBlocks = new GeyserBedrockBlock[JAVA_BLOCKS_SIZE];

            Map<String, NbtMap> flowerPotBlocks = new Object2ObjectOpenHashMap<>();
            Map<NbtMap, BlockDefinition> itemFrames = new Object2ObjectOpenHashMap<>();

            Set<BlockDefinition> jigsawDefinitions = new ObjectOpenHashSet<>();

            BlockMappings.BlockMappingsBuilder builder = BlockMappings.builder();
            while (blocksIterator.hasNext()) {
                javaRuntimeId++;
                Map.Entry<String, JsonNode> entry = blocksIterator.next();
                String javaId = entry.getKey();

                NbtMap originalBedrockTag = buildBedrockState(entry.getValue());
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

                if (javaId.contains("jigsaw")) {
                    jigsawDefinitions.add(bedrockDefinition);
                }

                boolean waterlogged = entry.getKey().contains("waterlogged=true")
                        || javaId.contains("minecraft:bubble_column") || javaId.contains("minecraft:kelp") || javaId.contains("seagrass");

                if (waterlogged) {
                    int finalJavaRuntimeId = javaRuntimeId;
                    BlockRegistries.WATERLOGGED.register(set -> set.set(finalJavaRuntimeId));
                }

                String cleanJavaIdentifier = BlockUtils.getCleanIdentifier(entry.getKey());

                // Get the tag needed for non-empty flower pots
                if (entry.getValue().get("pottable") != null) {
                    flowerPotBlocks.put(cleanJavaIdentifier.intern(), blockStates.get(bedrockDefinition.getRuntimeId()));
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
            if (nonVanillaStateOverrides.size() > 0) {
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
                }
            }

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
                    .stateDefinitionMap(blockStateOrderedMap)
                    .itemFrames(itemFrames)
                    .flowerPotBlocks(flowerPotBlocks)
                    .jigsawStates(jigsawDefinitions)
                    .remappedVanillaIds(remappedVanillaIds)
                    .blockProperties(customBlockProperties)
                    .customBlockStateDefinitions(customBlockStateDefinitions)
                    .extendedCollisionBoxes(extendedCollisionBoxes)
                    .build());
        }
    }

    private static void registerJavaBlocks() {
        JsonNode blocksJson;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResource("mappings/blocks.json")) {
            blocksJson = GeyserImpl.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block mappings", e);
        }

        JAVA_BLOCKS_SIZE = blocksJson.size();

        if (BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().size() > 0) {
            MIN_CUSTOM_RUNTIME_ID = BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().keySet().stream().min(Comparator.comparing(JavaBlockState::javaId)).get().javaId();
            int maxCustomRuntimeID = BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().keySet().stream().max(Comparator.comparing(JavaBlockState::javaId)).get().javaId();

            if (MIN_CUSTOM_RUNTIME_ID < blocksJson.size()) {
                throw new RuntimeException("Non vanilla custom block state overrides runtime ID must start after the last vanilla block state (" + JAVA_BLOCKS_SIZE + ")");
            }

            JAVA_BLOCKS_SIZE = maxCustomRuntimeID + 1; // Runtime ids start at 0, so we need to add 1
        }

        BlockRegistries.JAVA_BLOCKS.set(new BlockMapping[JAVA_BLOCKS_SIZE]); // Set array size to number of blockstates

        Deque<String> cleanIdentifiers = new ArrayDeque<>();

        int javaRuntimeId = -1;
        int cobwebBlockId = -1;
        int furnaceRuntimeId = -1;
        int furnaceLitRuntimeId = -1;
        int honeyBlockRuntimeId = -1;
        int slimeBlockRuntimeId = -1;
        int spawnerRuntimeId = -1;
        int uniqueJavaId = -1;
        int waterRuntimeId = -1;
        Iterator<Map.Entry<String, JsonNode>> blocksIterator = blocksJson.fields();
        while (blocksIterator.hasNext()) {
            javaRuntimeId++;
            Map.Entry<String, JsonNode> entry = blocksIterator.next();
            String javaId = entry.getKey();

            // TODO fix this, (no block should have a null hardness)
            BlockMapping.BlockMappingBuilder builder = BlockMapping.builder();
            JsonNode hardnessNode = entry.getValue().get("block_hardness");
            if (hardnessNode != null) {
                builder.hardness(hardnessNode.floatValue());
            }

            JsonNode canBreakWithHandNode = entry.getValue().get("can_break_with_hand");
            if (canBreakWithHandNode != null) {
                builder.canBreakWithHand(canBreakWithHandNode.booleanValue());
            } else {
                builder.canBreakWithHand(false);
            }

            JsonNode collisionIndexNode = entry.getValue().get("collision_index");
            if (hardnessNode != null) {
                builder.collisionIndex(collisionIndexNode.intValue());
            }

            JsonNode pickItemNode = entry.getValue().get("pick_item");
            if (pickItemNode != null) {
                builder.pickItem(pickItemNode.textValue().intern());
            }

            if (javaId.equals("minecraft:obsidian") || javaId.equals("minecraft:crying_obsidian") || javaId.startsWith("minecraft:respawn_anchor") || javaId.startsWith("minecraft:reinforced_deepslate")) {
                builder.pistonBehavior(PistonBehavior.BLOCK);
            } else {
                JsonNode pistonBehaviorNode = entry.getValue().get("piston_behavior");
                if (pistonBehaviorNode != null) {
                    builder.pistonBehavior(PistonBehavior.getByName(pistonBehaviorNode.textValue()));
                } else {
                    builder.pistonBehavior(PistonBehavior.NORMAL);
                }
            }

            JsonNode hasBlockEntityNode = entry.getValue().get("has_block_entity");
            if (hasBlockEntityNode != null) {
                builder.isBlockEntity(hasBlockEntityNode.booleanValue());
            } else {
                builder.isBlockEntity(false);
            }

            BlockStateValues.storeBlockStateValues(entry.getKey(), javaRuntimeId, entry.getValue());

            String cleanJavaIdentifier = BlockUtils.getCleanIdentifier(entry.getKey());
            String bedrockIdentifier = entry.getValue().get("bedrock_identifier").asText();

            if (!cleanJavaIdentifier.equals(cleanIdentifiers.peekLast())) {
                uniqueJavaId++;
                cleanIdentifiers.add(cleanJavaIdentifier.intern());
            }

            builder.javaIdentifier(javaId);
            builder.javaBlockId(uniqueJavaId);

            BlockRegistries.JAVA_IDENTIFIER_TO_ID.register(javaId, javaRuntimeId);
            BlockRegistries.JAVA_BLOCKS.register(javaRuntimeId, builder.build());

            // Keeping this here since this is currently unchanged between versions
            // It's possible to only have this store differences in names, but the key set of all Java names is used in sending command suggestions
            BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.register(cleanJavaIdentifier.intern(), bedrockIdentifier.intern());

            if (javaId.contains("cobweb")) {
                cobwebBlockId = uniqueJavaId;

            } else if (javaId.startsWith("minecraft:furnace[facing=north")) {
                if (javaId.contains("lit=true")) {
                    furnaceLitRuntimeId = javaRuntimeId;
                } else {
                    furnaceRuntimeId = javaRuntimeId;
                }

            } else if (javaId.startsWith("minecraft:spawner")) {
                spawnerRuntimeId = javaRuntimeId;

            } else if ("minecraft:water[level=0]".equals(javaId)) {
                waterRuntimeId = javaRuntimeId;
            } else if (javaId.equals("minecraft:honey_block")) {
                honeyBlockRuntimeId = javaRuntimeId;
            } else if (javaId.equals("minecraft:slime_block")) {
                slimeBlockRuntimeId = javaRuntimeId;
            }
        }

        if (cobwebBlockId == -1) {
            throw new AssertionError("Unable to find cobwebs in palette");
        }
        BlockStateValues.JAVA_COBWEB_ID = cobwebBlockId;

        if (furnaceRuntimeId == -1) {
            throw new AssertionError("Unable to find furnace in palette");
        }
        BlockStateValues.JAVA_FURNACE_ID = furnaceRuntimeId;

        if (furnaceLitRuntimeId == -1) {
            throw new AssertionError("Unable to find lit furnace in palette");
        }
        BlockStateValues.JAVA_FURNACE_LIT_ID = furnaceLitRuntimeId;

        if (honeyBlockRuntimeId == -1) {
            throw new AssertionError("Unable to find honey block in palette");
        }
        BlockStateValues.JAVA_HONEY_BLOCK_ID = honeyBlockRuntimeId;

        if (slimeBlockRuntimeId == -1) {
            throw new AssertionError("Unable to find slime block in palette");
        }
        BlockStateValues.JAVA_SLIME_BLOCK_ID = slimeBlockRuntimeId;

        if (spawnerRuntimeId == -1) {
            throw new AssertionError("Unable to find spawner in palette");
        }
        BlockStateValues.JAVA_SPAWNER_ID = spawnerRuntimeId;

        if (waterRuntimeId == -1) {
            throw new AssertionError("Unable to find Java water in palette");
        }
        BlockStateValues.JAVA_WATER_ID = waterRuntimeId;

        if (BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().size() > 0) {
            Set<Integer> usedNonVanillaRuntimeIDs = new HashSet<>();

            for (JavaBlockState javaBlockState : BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().keySet()) {
                if (!usedNonVanillaRuntimeIDs.add(javaBlockState.javaId())) {
                    throw new RuntimeException("Duplicate runtime ID " + javaBlockState.javaId() + " for non vanilla Java block state " + javaBlockState.identifier());
                }

                CustomBlockState customBlockState = BlockRegistries.NON_VANILLA_BLOCK_STATE_OVERRIDES.get().get(javaBlockState);

                String javaId = javaBlockState.identifier();
                int stateRuntimeId = javaBlockState.javaId();
                BlockMapping blockMapping = BlockMapping.builder()
                    .canBreakWithHand(javaBlockState.canBreakWithHand())
                    .pickItem(javaBlockState.pickItem())
                    .isNonVanilla(true)
                    .javaIdentifier(javaId)
                    .javaBlockId(javaBlockState.stateGroupId())
                    .hardness(javaBlockState.blockHardness())
                    .pistonBehavior(javaBlockState.pistonBehavior() == null ? PistonBehavior.NORMAL : PistonBehavior.getByName(javaBlockState.pistonBehavior()))
                    .isBlockEntity(javaBlockState.hasBlockEntity())
                    .build();

                String cleanJavaIdentifier = BlockUtils.getCleanIdentifier(javaBlockState.identifier());
                String bedrockIdentifier = customBlockState.block().identifier();

                if (!cleanJavaIdentifier.equals(cleanIdentifiers.peekLast())) {
                    uniqueJavaId++;
                    cleanIdentifiers.add(cleanJavaIdentifier.intern());
                }

                BlockRegistries.JAVA_IDENTIFIER_TO_ID.register(javaId, stateRuntimeId);
                BlockRegistries.JAVA_BLOCKS.register(stateRuntimeId, blockMapping);

                // Keeping this here since this is currently unchanged between versions
                // It's possible to only have this store differences in names, but the key set of all Java names is used in sending command suggestions
                BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.register(cleanJavaIdentifier.intern(), bedrockIdentifier.intern());
            }
        }

        BlockRegistries.CLEAN_JAVA_IDENTIFIERS.set(cleanIdentifiers.toArray(new String[0]));

        BLOCKS_JSON = blocksJson;

        JsonNode blockInteractionsJson;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResource("mappings/interactions.json")) {
            blockInteractionsJson = GeyserImpl.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java block interaction mappings", e);
        }

        BlockRegistries.INTERACTIVE.set(toBlockStateSet((ArrayNode) blockInteractionsJson.get("always_consumes")));
        BlockRegistries.INTERACTIVE_MAY_BUILD.set(toBlockStateSet((ArrayNode) blockInteractionsJson.get("requires_may_build")));
    }

    private static BitSet toBlockStateSet(ArrayNode node) {
        BitSet blockStateSet = new BitSet(node.size());
        for (JsonNode javaIdentifier : node) {
            blockStateSet.set(BlockRegistries.JAVA_IDENTIFIER_TO_ID.get().getInt(javaIdentifier.textValue()));
        }
        return blockStateSet;
    }

    private static NbtMap buildBedrockState(JsonNode node) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        String bedrockIdentifier = node.get("bedrock_identifier").textValue();
        tagBuilder.putString("name", bedrockIdentifier);

        NbtMapBuilder statesBuilder = NbtMap.builder();

        // check for states
        JsonNode states = node.get("bedrock_states");
        if (states != null) {
            Iterator<Map.Entry<String, JsonNode>> statesIterator = states.fields();

            while (statesIterator.hasNext()) {
                Map.Entry<String, JsonNode> stateEntry = statesIterator.next();
                JsonNode stateValue = stateEntry.getValue();
                switch (stateValue.getNodeType()) {
                    case BOOLEAN -> statesBuilder.putBoolean(stateEntry.getKey(), stateValue.booleanValue());
                    case STRING -> statesBuilder.putString(stateEntry.getKey(), stateValue.textValue());
                    case NUMBER -> statesBuilder.putInt(stateEntry.getKey(), stateValue.intValue());
                }
            }
        }
        tagBuilder.put("states", statesBuilder.build());
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
