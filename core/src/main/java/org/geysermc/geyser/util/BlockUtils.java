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

package org.geysermc.geyser.util;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.property.Property;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.SkullBlock;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityEffectCache;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;

import java.util.List;
import java.util.Optional;

public final class BlockUtils {

    /**
     * Returns the total mining progress added by mining the block in a single tick
     * Mirrors mojmap BlockBehaviour#getDestroyProgress
     *
     * @return the mining progress added by this tick.
     */
    public static float getBlockMiningProgressPerTick(GeyserSession session, Block block, GeyserItemStack itemInHand) {
        float destroySpeed = block.destroyTime();
        if (destroySpeed == -1.0F) {
            return 0;
        }

        int speedMultiplier = hasCorrectTool(session, block, itemInHand) ? 30 : 100;
        return getPlayerDestroySpeed(session, block, itemInHand) / destroySpeed / (float) speedMultiplier;
    }

    private static boolean hasCorrectTool(GeyserSession session, Block block, GeyserItemStack stack) {
        return !block.requiresCorrectToolForDrops() || isCorrectItemForDrops(session, block, stack);
    }

    private static boolean isCorrectItemForDrops(GeyserSession session, Block block, GeyserItemStack stack) {
        ToolData tool = stack.getComponent(DataComponentTypes.TOOL);
        if (tool == null) {
            return false;
        }

        for (ToolData.Rule rule : tool.getRules()) {
            if (rule.getCorrectForDrops() != null) {
                if (block.is(session, rule.getBlocks())) {
                    return rule.getCorrectForDrops();
                }
            }
        }

        return false;
    }

    private static float getItemDestroySpeed(GeyserSession session, Block block, GeyserItemStack stack) {
        ToolData tool = stack.getComponent(DataComponentTypes.TOOL);
        if (tool == null) {
            return 1.0F;
        }

        for (ToolData.Rule rule : tool.getRules()) {
            if (rule.getSpeed() != null) {
                if (block.is(session, rule.getBlocks())) {
                    return rule.getSpeed();
                }
            }
        }

        return tool.getDefaultMiningSpeed();
    }

    private static float getPlayerDestroySpeed(GeyserSession session, Block block, GeyserItemStack itemInHand) {
        float destroySpeed = getItemDestroySpeed(session, block, itemInHand);

        if (destroySpeed > 1.0F) {
            destroySpeed += (float) session.getPlayerEntity().getMiningEfficiency();
        }

        EntityEffectCache effectCache = session.getEffectCache();
        int miningSpeedMultiplier = getMiningSpeedAmplification(effectCache);
        if (miningSpeedMultiplier > 0) {
            destroySpeed *= 1.0F + miningSpeedMultiplier * 0.2F;
        }

        if (effectCache.getMiningFatigue() != 0) {
            float slowdown = switch (effectCache.getMiningFatigue()) {
                case 1 -> 0.3F;
                case 2 -> 0.09F;
                case 3 -> 0.0027F;
                default -> 8.1E-4F;
            };
            destroySpeed *= slowdown;
        }

        destroySpeed *= (float) session.getPlayerEntity().getBlockBreakSpeed();
        if (session.getCollisionManager().isWaterInEyes()) {
            destroySpeed *= (float) session.getPlayerEntity().getSubmergedMiningSpeed();
        }

        if (!session.getPlayerEntity().isOnGround()) {
            destroySpeed /= 5.0F;
        }

        return destroySpeed;
    }

    private static int getMiningSpeedAmplification(EntityEffectCache cache) {
        return Math.max(cache.getHaste(), cache.getConduitPower());
    }

    public static double reciprocal(double progress) {
        return Math.ceil(1 / progress);
    }

    /**
     * Given a position, return the position if a block were located on the specified block face.
     * @param blockPos the block position
     * @param face the face of the block - see {@link org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction}
     * @return the block position with the block face accounted for
     */
    public static Vector3i getBlockPosition(Vector3i blockPos, Direction face) {
        return switch (face) {
            case DOWN -> blockPos.sub(0, 1, 0);
            case UP -> blockPos.add(0, 1, 0);
            case NORTH -> blockPos.sub(0, 0, 1);
            case SOUTH -> blockPos.add(0, 0, 1);
            case WEST -> blockPos.sub(1, 0, 0);
            case EAST -> blockPos.add(1, 0, 0);
        };
    }

    /**
     * Taking in a complete Java block state identifier, output just the block ID of this block state without the states.
     * Examples:
     * minecraft:oak_log[axis=x] = minecraft:oak_log
     * minecraft:stone_brick_wall[east=low,north=tall,south=none,up=true,waterlogged=false,west=tall] = minecraft:stone_brick_wall
     * minecraft:stone = minecraft:stone
     *
     * @param fullJavaIdentifier a full Java block identifier, with possible block states.
     * @return a clean identifier in the format of minecraft:block
     */
    public static String getCleanIdentifier(String fullJavaIdentifier) {
        int stateIndex = fullJavaIdentifier.indexOf('[');
        if (stateIndex == -1) {
            // Identical to its clean variation
            return fullJavaIdentifier;
        }
        return fullJavaIdentifier.substring(0, stateIndex);
    }

    public static BlockCollision getCollision(int blockId) {
        return BlockRegistries.COLLISIONS.get(blockId);
    }

    public static void spawnBlockBreakParticles(GeyserSession session, Direction direction, Vector3i position, BlockState blockState) {
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        switch (direction) {
            case UP -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_UP);
            case DOWN -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_DOWN);
            case NORTH -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_NORTH);
            case EAST -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_EAST);
            case SOUTH -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH);
            case WEST -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_WEST);
        }
        levelEventPacket.setPosition(position.toFloat());
        levelEventPacket.setData(session.getBlockMappings().getBedrockBlock(blockState).getRuntimeId());
        session.sendUpstreamPacket(levelEventPacket);
    }

    public static void sendBedrockStopBlockBreak(GeyserSession session, Vector3f vector) {
        LevelEventPacket stopBreak = new LevelEventPacket();
        stopBreak.setType(LevelEvent.BLOCK_STOP_BREAK);
        stopBreak.setPosition(vector);
        stopBreak.setData(0);
        session.sendUpstreamPacket(stopBreak);
    }

    public static void sendBedrockBlockDestroy(GeyserSession session, Vector3f vector, int blockState) {
        LevelEventPacket blockBreakPacket = new LevelEventPacket();
        blockBreakPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
        blockBreakPacket.setPosition(vector);
        blockBreakPacket.setData(session.getBlockMappings().getBedrockBlockId(blockState));
        session.sendUpstreamPacket(blockBreakPacket);
    }

    public static void restoreCorrectBlock(GeyserSession session, Vector3i vector, BlockState blockState) {
        restoreCorrectBlockAndItem(session, vector, blockState, session.getPlayerInventory().getHeldItemSlot());
    }

    public static void restoreCorrectBlock(GeyserSession session, Vector3i blockPos, int slot) {
        restoreCorrectBlockAndItem(session, blockPos, session.getGeyser().getWorldManager().blockAt(session, blockPos), slot);
    }

    public static void restoreCorrectBlockAndItem(GeyserSession session, Vector3i vector, BlockState blockState, int slot) {
        BlockDefinition bedrockBlock = session.getBlockMappings().getBedrockBlock(blockState);

        if (blockState.block() instanceof SkullBlock skullBlock && skullBlock.skullType() == SkullBlock.Type.PLAYER) {
            // The changed block was a player skull so check if a custom block was defined for this skull
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(vector);
            if (skull != null && skull.getBlockDefinition() != null) {
                bedrockBlock = skull.getBlockDefinition();
            }
        }

        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(vector);
        updateBlockPacket.setDefinition(bedrockBlock);
        updateBlockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateBlockPacket);

        UpdateBlockPacket updateWaterPacket = new UpdateBlockPacket();
        updateWaterPacket.setDataLayer(1);
        updateWaterPacket.setBlockPosition(vector);
        updateWaterPacket.setDefinition(BlockRegistries.WATERLOGGED.get().get(blockState.javaId()) ? session.getBlockMappings().getBedrockWater() : session.getBlockMappings().getBedrockAir());
        updateWaterPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateWaterPacket);

        // Reset the item in hand to prevent "missing" blocks
        session.getPlayerInventoryHolder().updateSlot(session.getPlayerInventory().getOffsetForHotbar(slot));
    }

    public static void stopBreakAndRestoreBlock(GeyserSession session, Vector3i vector, BlockState blockState) {
        sendBedrockStopBlockBreak(session, vector.toFloat());
        restoreCorrectBlock(session, vector, blockState);
    }

    public static boolean blockMatchesPredicate(GeyserSession session, BlockState state, AdventureModePredicate.BlockPredicate predicate) {
        if (predicate.getBlocks() != null && !state.block().is(session, predicate.getBlocks())) {
            return false;
        } else if (predicate.getProperties() != null) {
            List<AdventureModePredicate.PropertyMatcher> matchers = predicate.getProperties();
            if (!matchers.isEmpty()) {
                for (AdventureModePredicate.PropertyMatcher matcher : matchers) {
                    for (Property<?> property : state.block().propertyKeys()) {
                        if (matcher.getName().equals(property.name())) {
                            if (!propertyMatchesPredicate(state, property, matcher)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        // Not checking NBT or data components - assume the predicate matches
        return true;
    }

    private static <T extends Comparable<T>> boolean propertyMatchesPredicate(BlockState state, Property<T> property, AdventureModePredicate.PropertyMatcher matcher) {
        T stateValue = state.getValue(property);
        if (matcher.getValue() != null) {
            Optional<T> value = property.valueOf(matcher.getValue());
            return value.isPresent() && stateValue.equals(value.get());
        } else {
            if (matcher.getMinValue() != null) {
                Optional<T> min = property.valueOf(matcher.getMinValue());
                if (min.isEmpty() || stateValue.compareTo(min.get()) < 0) {
                    return false;
                }
            }
            if (matcher.getMaxValue() != null) {
                Optional<T> max = property.valueOf(matcher.getMaxValue());
                if (max.isEmpty() || stateValue.compareTo(max.get()) > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    private BlockUtils() {
    }
}
