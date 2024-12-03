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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.item.BedrockEnchantment;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityEffectCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;

public final class BlockUtils {

    public static float getBlockDestroyProgress(GeyserSession session, BlockState blockState, GeyserItemStack itemInHand) {
        float destroySpeed = blockState.block().destroyTime();
        if (destroySpeed == -1) {
            return 0;
        }

        int speedMultiplier = hasCorrectTool(session, blockState.block(), itemInHand) ? 30 : 100;
        return getPlayerDestroySpeed(session, blockState, itemInHand) / destroySpeed / speedMultiplier;
    }

    private static boolean hasCorrectTool(GeyserSession session, Block block, GeyserItemStack stack) {
        return !block.requiresCorrectToolForDrops() || isCorrectItemForDrops(session, block, stack);
    }

    private static boolean isCorrectItemForDrops(GeyserSession session, Block block, GeyserItemStack stack) {
        ToolData tool = stack.getComponent(DataComponentType.TOOL);
        if (tool == null) {
            return false;
        }

        for (ToolData.Rule rule : tool.getRules()) {
            if (rule.getCorrectForDrops() != null) {
                GeyserHolderSet<Block> set = GeyserHolderSet.convertHolderSet(JavaRegistries.BLOCK, rule.getBlocks());
                if (session.getTagCache().is(set, block)) {
                    return rule.getCorrectForDrops();
                }
            }
        }

        return false;
    }

    private static float getItemDestroySpeed(GeyserSession session, Block block, GeyserItemStack stack) {
        ToolData tool = stack.getComponent(DataComponentType.TOOL);
        if (tool == null) {
            return 1f;
        }

        for (ToolData.Rule rule : tool.getRules()) {
            if (rule.getSpeed() != null) {
                GeyserHolderSet<Block> set = GeyserHolderSet.convertHolderSet(JavaRegistries.BLOCK, rule.getBlocks());
                if (session.getTagCache().is(set, block)) {
                    return rule.getSpeed();
                }
            }
        }

        return tool.getDefaultMiningSpeed();
    }

    private static float getPlayerDestroySpeed(GeyserSession session, BlockState blockState, GeyserItemStack itemInHand) {
        float destroySpeed = getItemDestroySpeed(session, blockState.block(), itemInHand);
        EntityEffectCache effectCache = session.getEffectCache();

        if (destroySpeed > 1.0F) {
            destroySpeed += session.getPlayerEntity().attributeOrDefault(GeyserAttributeType.MINING_EFFICIENCY);
        }

        int miningSpeedMultiplier = getMiningSpeedAmplification(effectCache);
        if (miningSpeedMultiplier > 0) {
            destroySpeed *= miningSpeedMultiplier * 0.2F;
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

        destroySpeed *= session.getPlayerEntity().attributeOrDefault(GeyserAttributeType.BLOCK_BREAK_SPEED);
        if (session.getCollisionManager().isWaterInEyes()) {
            destroySpeed *= session.getPlayerEntity().attributeOrDefault(GeyserAttributeType.SUBMERGED_MINING_SPEED);
        }

        if (!session.getPlayerEntity().isOnGround()) {
            destroySpeed /= 5F;
        }

        return destroySpeed;
    }

    private static int getMiningSpeedAmplification(EntityEffectCache cache) {
        return Math.max(cache.getHaste(), cache.getConduitPower());
    }

    public int getDestroyStage(GeyserSession session) {
        return session.getDestroyProgress() > 0F ? (int) session.getDestroyProgress() * 10 : -1;
    }

    // TODO 1.21.4 this changed probably; no more tiers
    public static double getBreakTime(GeyserSession session, Block block, ItemMapping item, @Nullable DataComponents components, boolean isSessionPlayer) {
//        boolean isShearsEffective = session.getTagCache().is(BlockTag.LEAVES, block) || session.getTagCache().is(BlockTag.WOOL, block); //TODO called twice
//        boolean canHarvestWithHand = !block.requiresCorrectToolForDrops();
//        String toolType = "";
//        String toolTier = "";
//        boolean correctTool = false;
//        boolean toolCanBreak = false;
//        if (item.isTool()) {
//            toolType = item.getToolType();
//            toolTier = item.getToolTier();
//            correctTool = correctTool(session, block, toolType);
//            toolCanBreak = canToolTierBreakBlock(session, block, toolTier);
//        }
//
//        int toolEfficiencyLevel = ItemUtils.getEnchantmentLevel(session, components, BedrockEnchantment.EFFICIENCY);
//        int hasteLevel = 0;
//        int miningFatigueLevel = 0;
//
//        if (!isSessionPlayer) {
//            // Another entity is currently mining; we have all the information we know
//            return calculateBreakTime(block.destroyTime(), toolTier, canHarvestWithHand, correctTool, toolCanBreak, toolType, isShearsEffective,
//                    toolEfficiencyLevel, hasteLevel, miningFatigueLevel, false, true);
//        }
//
//        hasteLevel = Math.max(session.getEffectCache().getHaste(), session.getEffectCache().getConduitPower());
//        miningFatigueLevel = session.getEffectCache().getMiningFatigue();
//
//        boolean waterInEyes = session.getCollisionManager().isWaterInEyes();
//        boolean insideOfWaterWithoutAquaAffinity = waterInEyes &&
//                ItemUtils.getEnchantmentLevel(session, session.getPlayerInventory().getItem(5).getAllComponents(), BedrockEnchantment.AQUA_AFFINITY) < 1;
//
//        return calculateBreakTime(block.destroyTime(), toolTier, canHarvestWithHand, correctTool, toolCanBreak, toolType, isShearsEffective,
//                toolEfficiencyLevel, hasteLevel, miningFatigueLevel, insideOfWaterWithoutAquaAffinity, session.getPlayerEntity().isOnGround());
    }

    public static double getSessionBreakTime(GeyserSession session, Block block) {
//        PlayerInventory inventory = session.getPlayerInventory();
//        GeyserItemStack item = inventory.getItemInHand();
//        ItemMapping mapping = ItemMapping.AIR;
//        DataComponents components = null;
//        if (item != null) {
//            mapping = item.getMapping(session);
//            components = item.getAllComponents();
//        }
//        return getBreakTime(session, block, mapping, components, true);
    }

    /**
     * Given a position, return the position if a block were located on the specified block face.
     * @param blockPos the block position
     * @param face the face of the block - see {@link org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction}
     * @return the block position with the block face accounted for
     */
    public static Vector3i getBlockPosition(Vector3i blockPos, int face) {
        return switch (face) {
            case 0 -> blockPos.sub(0, 1, 0);
            case 1 -> blockPos.add(0, 1, 0);
            case 2 -> blockPos.sub(0, 0, 1);
            case 3 -> blockPos.add(0, 0, 1);
            case 4 -> blockPos.sub(1, 0, 0);
            case 5 -> blockPos.add(1, 0, 0);
            default -> blockPos;
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

    public static BlockCollision getCollisionAt(GeyserSession session, Vector3i blockPos) {
        return getCollision(session.getGeyser().getWorldManager().getBlockAt(session, blockPos));
    }

    private BlockUtils() {
    }
}
