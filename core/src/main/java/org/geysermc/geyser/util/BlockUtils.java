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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;

public final class BlockUtils {

    private static boolean correctTool(GeyserSession session, BlockMapping blockMapping, String itemToolType) {
        switch (itemToolType) {
            case "axe":
                return session.getTagCache().isAxeEffective(blockMapping);
            case "hoe":
                return session.getTagCache().isHoeEffective(blockMapping);
            case "pickaxe":
                return session.getTagCache().isPickaxeEffective(blockMapping);
            case "shears":
                return session.getTagCache().isShearsEffective(blockMapping);
            case "shovel":
                return session.getTagCache().isShovelEffective(blockMapping);
            case "sword":
                return blockMapping.getJavaBlockId() == BlockStateValues.JAVA_COBWEB_ID;
            default:
                session.getGeyser().getLogger().warning("Unknown tool type: " + itemToolType);
                return false;
        }
    }

    private static double toolBreakTimeBonus(String toolType, String toolTier, boolean isShearsEffective) {
        if (toolType.equals("shears")) return isShearsEffective ? 5.0 : 15.0;
        if (toolType.equals("")) return 1.0;
        return switch (toolTier) {
            // https://minecraft.wiki/w/Breaking#Speed
            case "wooden" -> 2.0;
            case "stone" -> 4.0;
            case "iron" -> 6.0;
            case "diamond" -> 8.0;
            case "netherite" -> 9.0;
            case "golden" -> 12.0;
            default -> 1.0;
        };
    }

    private static boolean canToolTierBreakBlock(GeyserSession session, BlockMapping blockMapping, String toolTier) {
        if (toolTier.equals("netherite") || toolTier.equals("diamond")) {
            // As of 1.17, these tiers can mine everything that is mineable
            return true;
        }

        switch (toolTier) {
            // Use intentional fall-throughs to check each tier with this block
            default:
                if (session.getTagCache().requiresStoneTool(blockMapping)) {
                    return false;
                }
            case "stone":
                if (session.getTagCache().requiresIronTool(blockMapping)) {
                    return false;
                }
            case "iron":
                if (session.getTagCache().requiresDiamondTool(blockMapping)) {
                    return false;
                }
        }

        return true;
    }

    // https://minecraft.wiki/w/Breaking
    private static double calculateBreakTime(double blockHardness, String toolTier, boolean canHarvestWithHand, boolean correctTool, boolean canTierMineBlock,
                                             String toolType, boolean isShearsEffective, int toolEfficiencyLevel, int hasteLevel, int miningFatigueLevel,
                                             boolean insideOfWaterWithoutAquaAffinity, boolean onGround) {
        double baseTime = (((correctTool && canTierMineBlock) || canHarvestWithHand) ? 1.5 : 5.0) * blockHardness;
        double speed = 1.0 / baseTime;

        if (correctTool) {
            speed *= toolBreakTimeBonus(toolType, toolTier, isShearsEffective);
            speed += toolEfficiencyLevel == 0 ? 0 : toolEfficiencyLevel * toolEfficiencyLevel + 1;
        }
        speed *= 1.0 + (0.2 * hasteLevel);

        switch (miningFatigueLevel) {
            case 0:
                break;
            case 1:
                speed -= (speed * 0.7);
                break;
            case 2:
                speed -= (speed * 0.91);
                break;
            case 3:
                speed -= (speed * 0.9973);
                break;
            default:
                speed -= (speed * 0.99919);
                break;
        }

        if (insideOfWaterWithoutAquaAffinity) speed *= 0.2;
        if (!onGround) speed *= 0.2;
        return 1.0 / speed;
    }

    public static double getBreakTime(GeyserSession session, BlockMapping blockMapping, ItemMapping item, @Nullable CompoundTag nbtData, boolean isSessionPlayer) {
        boolean isShearsEffective = session.getTagCache().isShearsEffective(blockMapping); //TODO called twice
        boolean canHarvestWithHand = blockMapping.isCanBreakWithHand();
        String toolType = "";
        String toolTier = "";
        boolean correctTool = false;
        boolean toolCanBreak = false;
        if (item.isTool()) {
            toolType = item.getToolType();
            toolTier = item.getToolTier();
            correctTool = correctTool(session, blockMapping, toolType);
            toolCanBreak = canToolTierBreakBlock(session, blockMapping, toolTier);
        }
        int toolEfficiencyLevel = ItemUtils.getEnchantmentLevel(nbtData, "minecraft:efficiency");
        int hasteLevel = 0;
        int miningFatigueLevel = 0;

        if (!isSessionPlayer) {
            // Another entity is currently mining; we have all the information we know
            return calculateBreakTime(blockMapping.getHardness(), toolTier, canHarvestWithHand, correctTool, toolCanBreak, toolType, isShearsEffective,
                    toolEfficiencyLevel, hasteLevel, miningFatigueLevel, false, true);
        }

        hasteLevel = Math.max(session.getEffectCache().getHaste(), session.getEffectCache().getConduitPower());
        miningFatigueLevel = session.getEffectCache().getMiningFatigue();

        boolean waterInEyes = session.getCollisionManager().isWaterInEyes();
        boolean insideOfWaterWithoutAquaAffinity = waterInEyes &&
                ItemUtils.getEnchantmentLevel(session.getPlayerInventory().getItem(5).getNbt(), "minecraft:aqua_affinity") < 1;

        return calculateBreakTime(blockMapping.getHardness(), toolTier, canHarvestWithHand, correctTool, toolCanBreak, toolType, isShearsEffective,
                toolEfficiencyLevel, hasteLevel, miningFatigueLevel, insideOfWaterWithoutAquaAffinity, session.getPlayerEntity().isOnGround());
    }

    public static double getSessionBreakTime(GeyserSession session, BlockMapping blockMapping) {
        PlayerInventory inventory = session.getPlayerInventory();
        GeyserItemStack item = inventory.getItemInHand();
        ItemMapping mapping = ItemMapping.AIR;
        CompoundTag nbtData = null;
        if (item != null) {
            mapping = item.getMapping(session);
            nbtData = item.getNbt();
        }
        return getBreakTime(session, blockMapping, mapping, nbtData, true);
    }

    /**
     * Given a position, return the position if a block were located on the specified block face.
     * @param blockPos the block position
     * @param face the face of the block - see {@link com.github.steveice10.mc.protocol.data.game.entity.object.Direction}
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

    public static BlockCollision getCollisionAt(GeyserSession session, int x, int y, int z) {
        return getCollision(session.getGeyser().getWorldManager().getBlockAt(session, x, y, z));
    }

    private BlockUtils() {
    }
}
