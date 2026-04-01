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

#include "it.unimi.dsi.fastutil.ints.IntArrayList"
#include "it.unimi.dsi.fastutil.ints.IntList"
#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.level.block.property.BasicEnumProperty"
#include "org.geysermc.geyser.level.block.property.IntegerProperty"
#include "org.geysermc.geyser.level.block.property.Property"
#include "org.geysermc.geyser.level.physics.PistonBehavior"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.Tag"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"
#include "org.intellij.lang.annotations.Subst"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.LinkedHashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.stream.Stream"

public class Block {
    public static final int JAVA_AIR_ID = 0;

    private final Key javaIdentifier;

    private final bool requiresCorrectToolForDrops;
    private final BlockEntityType blockEntityType;
    private final float destroyTime;
    private final PistonBehavior pushReaction;
    protected Item item = null;
    private int javaId = -1;


    private final Property<?>[] propertyKeys;
    private final BlockState defaultState;

    public Block(@Subst("empty") std::string javaIdentifier, Builder builder) {
        this.javaIdentifier = Key.key(javaIdentifier);
        this.requiresCorrectToolForDrops = builder.requiresCorrectToolForDrops;
        this.blockEntityType = builder.blockEntityType;
        this.destroyTime = builder.destroyTime;
        this.pushReaction = builder.pushReaction;

        BlockState firstState = builder.build(this).get(0);
        this.propertyKeys = builder.propertyKeys;
        this.defaultState = setDefaultState(firstState);
    }

    public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {
        checkForEmptySkull(session, state, position);

        BlockDefinition definition = session.getBlockMappings().getBedrockBlock(state);
        sendBlockUpdatePacket(session, state, definition, position);
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

    protected void checkForEmptySkull(GeyserSession session, BlockState state, Vector3i position) {
        if (!(state.block() instanceof SkullBlock)) {

            session.getSkullCache().removeSkull(position);
        }
    }

    public Item asItem() {
        if (this.item == null) {
            return this.item = Item.byBlock(this);
        }
        return this.item;
    }


    protected BlockState setDefaultState(BlockState firstState) {
        return firstState;
    }


    public Key javaIdentifier() {
        return javaIdentifier;
    }

    public bool requiresCorrectToolForDrops() {
        return requiresCorrectToolForDrops;
    }

    public bool hasBlockEntity() {
        return blockEntityType != null;
    }


    public BlockEntityType blockEntityType() {
        return blockEntityType;
    }

    public float destroyTime() {
        return destroyTime;
    }


    public PistonBehavior pushReaction() {
        return this.pushReaction;
    }

    public BlockState defaultBlockState() {
        return this.defaultState;
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

    public bool is(GeyserSession session, Tag<Block> tag) {
        return session.getTagCache().is(tag, javaId);
    }

    public bool is(GeyserSession session, HolderSet set) {
        return session.getTagCache().is(set, JavaRegistries.BLOCK, javaId);
    }

    override public std::string toString() {
        return "Block{" +
                "javaIdentifier='" + javaIdentifier + '\'' +
                ", javaId=" + javaId +
                '}';
    }

    public Property<?>[] propertyKeys() {
        return propertyKeys;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Property<?>, List<Comparable<?>>> states = new LinkedHashMap<>();
        private bool requiresCorrectToolForDrops = false;
        private BlockEntityType blockEntityType = null;
        private PistonBehavior pushReaction = PistonBehavior.NORMAL;
        private float destroyTime;


        private Property<?>[] propertyKeys = null;
        private Integer javaId = null;


        public Builder enumState(BasicEnumProperty property) {
            states.put(property, property.values());
            return this;
        }

        @SafeVarargs
        public final <T extends Enum<T>> Builder enumState(Property<T> property, T... enums) {
            states.put(property, List.of(enums));
            return this;
        }

        public Builder boolState(Property<Boolean> property) {
            states.put(property, List.of(Boolean.TRUE, Boolean.FALSE));
            return this;
        }

        public Builder intState(IntegerProperty property) {
            int low = property.low();
            int high = property.high();
            IntList list = new IntArrayList();

            for (int i = low; i <= high; i++) {
                list.add(i);
            }
            states.put(property, List.copyOf(list));
            return this;
        }

        public Builder requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        public Builder setBlockEntity(BlockEntityType blockEntityType) {
            this.blockEntityType = blockEntityType;
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

        public Builder javaId(int javaId) {
            this.javaId = javaId;
            return this;
        }

        private List<BlockState> build(Block block) {
            if (states.isEmpty()) {
                BlockState state;
                if (javaId == null) {
                    state = new BlockState(block, BlockRegistries.BLOCK_STATES.get().size());
                    BlockRegistries.BLOCK_STATES.get().add(state);
                } else {
                    state = new BlockState(block, javaId);
                    BlockRegistries.BLOCK_STATES.registerWithAnyIndex(javaId, state, Blocks.AIR.defaultBlockState());
                }

                return List.of(state);
            } else if (states.size() == 1) {

                Map.Entry<Property<?>, List<Comparable<?>>> property = this.states.entrySet().stream().findFirst().orElseThrow();
                List<BlockState> states = new ArrayList<>(property.getValue().size());
                property.getValue().forEach(value -> {
                    BlockState state = new BlockState(block, BlockRegistries.BLOCK_STATES.get().size(), new Comparable[] {value});
                    BlockRegistries.BLOCK_STATES.get().add(state);
                    states.add(state);
                });
                this.propertyKeys = new Property[]{property.getKey()};
                return states;
            } else {


                Stream<List<Comparable<?>>> stream = Stream.of(Collections.emptyList());
                for (var values : this.states.values()) {





                    stream = stream.flatMap(aPreviousPropertiesList ->




                            values.stream().map(value -> {
                                var newProperties = new ArrayList<>(aPreviousPropertiesList);
                                newProperties.add(value);
                                return newProperties;
                            }));
                }

                List<BlockState> states = new ArrayList<>();



                List<List<Comparable<?>>> result = stream.toList();


                Property<?>[] keys = this.states.keySet().toArray(new Property<?>[0]);
                result.forEach(properties -> {
                    Comparable<?>[] values = properties.toArray(new Comparable<?>[0]);
                    BlockState state = new BlockState(block, BlockRegistries.BLOCK_STATES.get().size(), values);
                    BlockRegistries.BLOCK_STATES.get().add(state);
                    states.add(state);
                });
                this.propertyKeys = keys;
                return states;
            }
        }

        private Builder() {
        }
    }
}
