/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ToolItemEntry;

import java.util.Optional;

public class BlockUtils {

    private static boolean correctTool(String blockToolType, String itemToolType) {
        return (blockToolType.equals("sword") && itemToolType.equals("sword")) ||
                (blockToolType.equals("shovel") && itemToolType.equals("shovel")) ||
                (blockToolType.equals("pickaxe") && itemToolType.equals("pickaxe")) ||
                (blockToolType.equals("axe") && itemToolType.equals("axe")) ||
                (blockToolType.equals("shears") && itemToolType.equals("shears"));
    }

    private static double toolBreakTimeBonus(String toolType, String toolTier, boolean isWoolBlock) {
        if (toolType.equals("shears")) return isWoolBlock ? 5.0 : 15.0;
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

    //http://minecraft.gamepedia.com/Breaking
    private static double calculateBreakTime(double blockHardness, String toolTier, boolean canHarvestWithHand, boolean correctTool,
                                             String toolType, boolean isWoolBlock, boolean isCobweb, int toolEfficiencyLevel, int hasteLevel, int miningFatigueLevel,
                                             boolean insideOfWaterWithoutAquaAffinity, boolean outOfWaterButNotOnGround, boolean insideWaterAndNotOnGround) {
        double baseTime = ((correctTool || canHarvestWithHand) ? 1.5 : 5.0) * blockHardness;
        double speed = 1.0 / baseTime;

        if (correctTool) {
            speed *= toolBreakTimeBonus(toolType, toolTier, isWoolBlock);
            speed += toolEfficiencyLevel == 0 ? 0 : toolEfficiencyLevel * toolEfficiencyLevel + 1;
        } else if (toolType.equals("sword")) {
            speed*= (isCobweb ? 15.0 : 1.5);
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

    public static double getBreakTime(double blockHardness, int blockId, ItemEntry item, CompoundTag nbtData, GeyserSession session) {
        boolean isWoolBlock = BlockTranslator.JAVA_RUNTIME_WOOL_IDS.contains(blockId);
        boolean isCobweb = blockId == BlockTranslator.JAVA_RUNTIME_COBWEB_ID;
        String blockToolType = BlockTranslator.JAVA_RUNTIME_ID_TO_TOOL_TYPE.getOrDefault(blockId, "");
        boolean canHarvestWithHand = BlockTranslator.JAVA_RUNTIME_ID_TO_CAN_HARVEST_WITH_HAND.get(blockId);
        String toolType = "";
        String toolTier = "";
        boolean correctTool = false;
        if (item instanceof ToolItemEntry) {
            ToolItemEntry toolItem = (ToolItemEntry) item;
            toolType = toolItem.getToolType();
            toolTier = toolItem.getToolTier();
            correctTool = correctTool(blockToolType, toolType);
        }
        int toolEfficiencyLevel = ItemUtils.getEnchantmentLevel(nbtData, "minecraft:efficiency");
        int hasteLevel = 0;
        int miningFatigueLevel = 0;

        if (session == null) {
            return calculateBreakTime(blockHardness, toolTier, canHarvestWithHand, correctTool, toolType, isWoolBlock, isCobweb, toolEfficiencyLevel, hasteLevel, miningFatigueLevel, false, false, false);
        }

        hasteLevel = session.getPlayerEntity().getEffectCache().getEffectLevel(Effect.FASTER_DIG);
        miningFatigueLevel = session.getPlayerEntity().getEffectCache().getEffectLevel(Effect.SLOWER_DIG);

        boolean isInWater = session.getConnector().getConfig().isCacheChunks()
                && BlockTranslator.getBedrockBlockId(session.getConnector().getWorldManager().getBlockAt(session, session.getPlayerEntity().getPosition().toInt())) == BlockTranslator.BEDROCK_WATER_ID;

        boolean insideOfWaterWithoutAquaAffinity = isInWater &&
                ItemUtils.getEnchantmentLevel(Optional.ofNullable(session.getInventory().getItem(5)).map(ItemStack::getNbt).orElse(null), "minecraft:aqua_affinity") < 1;

        boolean outOfWaterButNotOnGround = (!isInWater) && (!session.getPlayerEntity().isOnGround());
        boolean insideWaterNotOnGround = isInWater && !session.getPlayerEntity().isOnGround();
        return calculateBreakTime(blockHardness, toolTier, canHarvestWithHand, correctTool, toolType, isWoolBlock, isCobweb, toolEfficiencyLevel, hasteLevel, miningFatigueLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround, insideWaterNotOnGround);
    }

}
