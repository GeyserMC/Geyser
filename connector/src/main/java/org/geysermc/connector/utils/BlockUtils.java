/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3i;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ToolItemEntry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.registry.type.BlockMapping;

public class BlockUtils {
    /**
     * A static constant of {@link Position} with all values being zero.
     */
    public static final Position POSITION_ZERO = new Position(0, 0, 0);

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
                return blockMapping.getJavaBlockId() == BlockTranslator.JAVA_COBWEB_BLOCK_ID;
            default:
                session.getConnector().getLogger().warning("Unknown tool type: " + itemToolType);
                return false;
        }
    }

    private static double toolBreakTimeBonus(String toolType, String toolTier, boolean isShearsEffective) {
        if (toolType.equals("shears")) return isShearsEffective ? 5.0 : 15.0;
        if (toolType.equals("")) return 1.0;
        switch (toolTier) {
            // https://minecraft.gamepedia.com/Breaking#Speed
            case "wooden":
                return 2.0;
            case "stone":
                return 4.0;
            case "iron":
                return 6.0;
            case "diamond":
                return 8.0;
            case "netherite":
                return 9.0;
            case "golden":
                return 12.0;
            default:
                return 1.0;
        }
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

    // https://minecraft.gamepedia.com/Breaking
    private static double calculateBreakTime(double blockHardness, String toolTier, boolean canHarvestWithHand, boolean correctTool, boolean canTierMineBlock,
                                             String toolType, boolean isShearsEffective, int toolEfficiencyLevel, int hasteLevel, int miningFatigueLevel,
                                             boolean insideOfWaterWithoutAquaAffinity, boolean outOfWaterButNotOnGround, boolean insideWaterAndNotOnGround) {
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
        if (outOfWaterButNotOnGround) speed *= 0.2;
        if (insideWaterAndNotOnGround) speed *= 0.2;
        return 1.0 / speed;
    }

    public static double getBreakTime(GeyserSession session, BlockMapping blockMapping, ItemEntry item, CompoundTag nbtData, boolean isSessionPlayer) {
        boolean isShearsEffective = session.getTagCache().isShearsEffective(blockMapping); //TODO called twice
        boolean canHarvestWithHand = blockMapping.isCanBreakWithHand();
        String toolType = "";
        String toolTier = "";
        boolean correctTool = false;
        boolean toolCanBreak = false;
        if (item instanceof ToolItemEntry) {
            ToolItemEntry toolItem = (ToolItemEntry) item;
            toolType = toolItem.getToolType();
            toolTier = toolItem.getToolTier();
            correctTool = correctTool(session, blockMapping, toolType);
            toolCanBreak = canToolTierBreakBlock(session, blockMapping, toolTier);
        }
        int toolEfficiencyLevel = ItemUtils.getEnchantmentLevel(nbtData, "minecraft:efficiency");
        int hasteLevel = 0;
        int miningFatigueLevel = 0;

        if (!isSessionPlayer) {
            // Another entity is currently mining; we have all the information we know
            return calculateBreakTime(blockMapping.getHardness(), toolTier, canHarvestWithHand, correctTool, toolCanBreak, toolType, isShearsEffective,
                    toolEfficiencyLevel, hasteLevel, miningFatigueLevel, false,
                    false, false);
        }

        hasteLevel = Math.max(session.getEffectCache().getEffectLevel(Effect.FASTER_DIG), session.getEffectCache().getEffectLevel(Effect.CONDUIT_POWER));
        miningFatigueLevel = session.getEffectCache().getEffectLevel(Effect.SLOWER_DIG);

        boolean isInWater = session.getCollisionManager().isPlayerInWater();

        boolean insideOfWaterWithoutAquaAffinity = isInWater &&
                ItemUtils.getEnchantmentLevel(session.getPlayerInventory().getItem(5).getNbt(), "minecraft:aqua_affinity") < 1;

        boolean outOfWaterButNotOnGround = (!isInWater) && (!session.getPlayerEntity().isOnGround());
        boolean insideWaterNotOnGround = isInWater && !session.getPlayerEntity().isOnGround();
        return calculateBreakTime(blockMapping.getHardness(), toolTier, canHarvestWithHand, correctTool, toolCanBreak, toolType, isShearsEffective,
                toolEfficiencyLevel, hasteLevel, miningFatigueLevel, insideOfWaterWithoutAquaAffinity,
                outOfWaterButNotOnGround, insideWaterNotOnGround);
    }

    /**
     * Given a position, return the position if a block were located on the specified block face.
     * @param blockPos the block position
     * @param face the face of the block - see {@link com.github.steveice10.mc.protocol.data.game.world.block.BlockFace}
     * @return the block position with the block face accounted for
     */
    public static Vector3i getBlockPosition(Vector3i blockPos, int face) {
        switch (face) {
            case 0:
                return blockPos.sub(0, 1, 0);
            case 1:
                return blockPos.add(0, 1, 0);
            case 2:
                return blockPos.sub(0, 0, 1);
            case 3:
                return blockPos.add(0, 0, 1);
            case 4:
                return blockPos.sub(1, 0, 0);
            case 5:
                return blockPos.add(1, 0, 0);
        }
        return blockPos;
    }

}
