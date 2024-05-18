/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.type;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Property;
import org.geysermc.geyser.level.physics.PistonBehavior;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.Identifier;

import java.util.*;
import java.util.stream.Stream;

public class Block {
    public static final int JAVA_AIR_ID = 0;

    private final String javaIdentifier;
    private final boolean requiresCorrectToolForDrops;
    private final boolean hasBlockEntity;
    private final float destroyTime;
    private final @NonNull PistonBehavior pushReaction;
    private int javaId = -1;

    public Block(String javaIdentifier, Builder builder) {
        this.javaIdentifier = Identifier.formalize(javaIdentifier).intern();
        this.requiresCorrectToolForDrops = builder.requiresCorrectToolForDrops;
        this.hasBlockEntity = builder.hasBlockEntity;
        this.destroyTime = builder.destroyTime;
        this.pushReaction = builder.pushReaction;
        builder.build(this);
    }

    public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {
        BlockDefinition definition = session.getBlockMappings().getBedrockBlock(state);
        sendBlockUpdatePacket(session, state, definition, position);

        {
            // Extended collision boxes for custom blocks
            if (!session.getBlockMappings().getExtendedCollisionBoxes().isEmpty()) {
                int aboveBlock = session.getGeyser().getWorldManager().getBlockAt(session, position.getX(), position.getY() + 1, position.getZ());
                BlockDefinition aboveBedrockExtendedCollisionDefinition = session.getBlockMappings().getExtendedCollisionBoxes().get(state.javaId());
                int belowBlock = session.getGeyser().getWorldManager().getBlockAt(session, position.getX(), position.getY() - 1, position.getZ());
                BlockDefinition belowBedrockExtendedCollisionDefinition = session.getBlockMappings().getExtendedCollisionBoxes().get(belowBlock);
                if (belowBedrockExtendedCollisionDefinition != null && state.is(Blocks.AIR)) {
                    UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
                    updateBlockPacket.setDataLayer(0);
                    updateBlockPacket.setBlockPosition(position);
                    updateBlockPacket.setDefinition(belowBedrockExtendedCollisionDefinition);
                    updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
                    session.sendUpstreamPacket(updateBlockPacket);
                } else if (aboveBedrockExtendedCollisionDefinition != null && aboveBlock == Block.JAVA_AIR_ID) {
                    UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
                    updateBlockPacket.setDataLayer(0);
                    updateBlockPacket.setBlockPosition(position.add(0, 1, 0));
                    updateBlockPacket.setDefinition(aboveBedrockExtendedCollisionDefinition);
                    updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
                    session.sendUpstreamPacket(updateBlockPacket);
                } else if (aboveBlock == Block.JAVA_AIR_ID) {
                    UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
                    updateBlockPacket.setDataLayer(0);
                    updateBlockPacket.setBlockPosition(position.add(0, 1, 0));
                    updateBlockPacket.setDefinition(session.getBlockMappings().getBedrockAir());
                    updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
                    session.sendUpstreamPacket(updateBlockPacket);
                }
            }
        }

        handleLecternBlockUpdate(session, state, position);
    }

    protected void sendBlockUpdatePacket(GeyserSession session, BlockState state, BlockDefinition definition, Vector3i position) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.setDefinition(definition);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        session.sendUpstreamPacket(updateBlockPacket);

        UpdateBlockPacket waterPacket = new UpdateBlockPacket();
        waterPacket.setDataLayer(1);
        waterPacket.setBlockPosition(position);
        if (BlockRegistries.WATERLOGGED.get().get(state.javaId())) {
            waterPacket.setDefinition(session.getBlockMappings().getBedrockWater());
        } else {
            waterPacket.setDefinition(session.getBlockMappings().getBedrockAir());
        }
        session.sendUpstreamPacket(waterPacket);
    }

    protected void handleLecternBlockUpdate(GeyserSession session, BlockState state, Vector3i position) {
        // Block state is out of bounds of this map - lectern has been destroyed, if it existed
        if (!session.getGeyser().getWorldManager().shouldExpectLecternHandled(session)) {
            session.getLecternCache().remove(position);
        }
    }

    public String javaIdentifier() {
        return javaIdentifier;
    }

    public boolean requiresCorrectToolForDrops() {
        return requiresCorrectToolForDrops;
    }

    public boolean hasBlockEntity() {
        return hasBlockEntity;
    }

    public float destroyTime() {
        return destroyTime;
    }

    @NonNull
    public PistonBehavior pushReaction() {
        return this.pushReaction;
    }

    public int javaId() {
        return javaId;
    }

    public void setJavaId(int javaId) {
        if (this.javaId != -1) {
            throw new RuntimeException("Block ID has already been set!");
        }
        this.javaId = javaId;
    }

    @Override
    public String toString() {
        return "Item{" +
                "javaIdentifier='" + javaIdentifier + '\'' +
                ", javaId=" + javaId +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Property<?>, List<Comparable<?>>> states = new LinkedHashMap<>();
        private boolean requiresCorrectToolForDrops = false;
        private boolean hasBlockEntity = false;
        private PistonBehavior pushReaction = PistonBehavior.NORMAL;
        private float destroyTime;

        /**
         * For states that we're just tracking for mirroring Java states.
         */
        public Builder enumState(Property<String> property, String... values) {
            states.put(property, List.of(values));
            return this;
        }

        @SafeVarargs
        public final <T extends Enum<T>> Builder enumState(Property<T> property, T... enums) {
            states.put(property, List.of(enums));
            return this;
        }

        public Builder booleanState(Property<Boolean> property) {
            states.put(property, List.of(Boolean.TRUE, Boolean.FALSE)); // Make this list a static constant if it'll survive past initialization
            return this;
        }

        public Builder intState(Property<Integer> property, int low, int high) {
            IntList list = new IntArrayList();
            // There is a state for every number between the low and high.
            for (int i = low; i <= high; i++) {
                list.add(i);
            }
            states.put(property, List.copyOf(list)); // Boxing reasons for that copy I guess.
            return this;
        }

        public Builder requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        public Builder setBlockEntity() {
            this.hasBlockEntity = true;
            return this;
        }

        public Builder destroyTime(float destroyTime) {
            this.destroyTime = destroyTime;
            return this;
        }

        public Builder pushReaction(PistonBehavior pushReaction) {
            this.pushReaction = pushReaction;
            return this;
        }

        private void build(Block block) {
            if (states.isEmpty()) {
                BlockRegistries.BLOCK_STATES.get().add(new BlockState(block, BlockRegistries.BLOCK_STATES.get().size()));
            } else {
                // Think of this stream as another list containing, at the start, one empty list.
                // It's two collections. Not a stream from the empty list.
                Stream<List<Pair<Property<?>, Comparable<?>>>> stream = Stream.of(Collections.emptyList());
                for (var state : this.states.entrySet()) {
                    // OK, so here's how I understand this works. Because this was staring at vanilla Java code trying
                    // to figure out exactly how it works so we don't have any discrepencies.
                    // For each existing pair in the list, a new list is created, adding one of the new values.
                    // Property up [true/false] would exist as true and false
                    // Both entries will get duplicated, adding down, true and false.
                    stream = stream.flatMap(aPreviousPropertiesList ->
                            // So the above is a list. It may be empty if this is the first property,
                            // or it may be populated if this is not the first property.
                            // We're about to create a new stream, each with a new list,
                            // for every previous property
                            state.getValue().stream().map(value -> {
                                var newProperties = new ArrayList<>(aPreviousPropertiesList);
                                newProperties.add(Pair.of(state.getKey(), value));
                                return newProperties;
                            }));
                }

                // Now we have a list of Pair<Property, Value>s. Each list is a block state!
                // If we have two boolean properties: up [true/false] and down [true/false],
                // We'll see [up=true,down=true], [up=false,down=true], [up=true,down=false], [up=false,down=false]
                stream.forEach(properties -> {
                    Reference2ObjectMap<Property<?>, Comparable<?>> propertyMap = new Reference2ObjectArrayMap<>(properties.size());
                    for (int i = 0; i < properties.size(); i++) {
                        Pair<Property<?>, Comparable<?>> property = properties.get(i);
                        propertyMap.put(property.key(), property.value());
                    }
                    BlockRegistries.BLOCK_STATES.get().add(new BlockState(block, BlockRegistries.BLOCK_STATES.get().size(), propertyMap));
                });
            }
        }

        private Builder() {
        }
    }
}
