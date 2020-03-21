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

package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerActionAckPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ToolItemEntry;
import org.geysermc.connector.utils.ChunkUtils;

public class JavaPlayerActionAckTranslator extends PacketTranslator<ServerPlayerActionAckPacket> {

    @Override
    public void translate(ServerPlayerActionAckPacket packet, GeyserSession session) {
        switch (packet.getAction()) {
            case FINISH_DIGGING:
                ChunkUtils.updateBlock(session, packet.getNewState(), packet.getPosition());
                break;

            case START_DIGGING: {
                LevelEventPacket levelEvent = new LevelEventPacket();
                levelEvent.setType(LevelEventType.BLOCK_START_BREAK);
                levelEvent.setPosition(Vector3f.from(
                        packet.getPosition().getX(),
                        packet.getPosition().getY(),
                        packet.getPosition().getZ()
                ));
                double blockHardness = BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(packet.getNewState().getId());

                PlayerInventory inventory = session.getInventory();
                ItemStack item = inventory.getItemInHand();
                System.out.println("item.getNbt() = " + item.getNbt());

                ItemEntry itemEntry = null;
                if (item != null) {
                    itemEntry = TranslatorsInit.getItemTranslator().getItem(item);
                }
                double breakTime = Math.ceil(getBreakTime(blockHardness, packet.getNewState().getId(), itemEntry) * 20);
                System.out.println("breakTime = " + breakTime);
                int data = (int) (65535 / breakTime);
                System.out.println("data = " + data);
                levelEvent.setData((int) (65535 / breakTime));
                session.getUpstream().sendPacket(levelEvent);
                break;
            }

            case CANCEL_DIGGING: {
                LevelEventPacket levelEvent = new LevelEventPacket();
                levelEvent.setType(LevelEventType.BLOCK_STOP_BREAK);
                levelEvent.setPosition(Vector3f.from(
                        packet.getPosition().getX(),
                        packet.getPosition().getY(),
                        packet.getPosition().getZ()
                ));
                levelEvent.setData(0);
                session.getUpstream().sendPacket(levelEvent);
                break;
            }
        }
    }

     /*private static double speedBonusByEfficiencyLore0(int efficiencyLoreLevel) {
        if (efficiencyLoreLevel == 0) return 0;
        return efficiencyLoreLevel * efficiencyLoreLevel + 1;
    }*/

    /*private static double speedRateByHasteLore0(int hasteLoreLevel) {
        return 1.0 + (0.2 * hasteLoreLevel);
    }*/


    private boolean correctTool(String blockToolType, String itemToolType) {
        return (blockToolType.equals("sword") && itemToolType.equals("sword")) ||
                (blockToolType.equals("shovel") && itemToolType.equals("shovel")) ||
                (blockToolType.equals("pickaxe") && itemToolType.equals("pickaxe")) ||
                (blockToolType.equals("axe") && itemToolType.equals("axe")) ||
                (blockToolType.equals("shears") && itemToolType.equals("shears")) ||
                blockToolType.equals("");
    }

    private double toolBreakTimeBonus(String toolType, String toolTier, boolean isWoolBlock) {
        if (toolType.equals("shears")) return isWoolBlock ? 5.0 : 15.0;
        if (toolType.equals("")) return 1.0;
        switch (toolTier) {
            case "wooden":
                return 2.0;
            case "stone":
                return 4.0;
            case "iron":
                return 6.0;
            case "diamond":
                return 8.0;
            case "golden":
                return 12.0;
            default:
                return 1.0;
        }
    }

    //http://minecraft.gamepedia.com/Breaking
    private double calculateBreakTime(double blockHardness, String toolTier, boolean canHarvestWithHand, boolean correctTool,
                              String toolType, boolean isWoolBlock, boolean isCobweb
                              /*int efficiencyLoreLevel, int hasteEffectLevel,
                                     boolean insideOfWaterWithoutAquaAffinity, boolean outOfWaterButNotOnGround*/) {
        System.out.println("blockHardness = " + blockHardness);
        double baseTime = ((correctTool || canHarvestWithHand) ? 1.5 : 5.0) * blockHardness;
        System.out.println("baseTime = " + baseTime);
        double speed = 1.0 / baseTime;
        System.out.println("speed = " + speed);

        if (correctTool) {
            speed *= toolBreakTimeBonus(toolType, toolTier, isWoolBlock);
        } else if (toolType.equals("sword")) {
            speed*= (isCobweb ? 15.0 : 1.5);
        }
        System.out.println("speed = " + speed);
        // TODO implement this math
        //speed += speedBonusByEfficiencyLore0(efficiencyLoreLevel);
        //speed *= speedRateByHasteLore0(hasteEffectLevel);
        //if (insideOfWaterWithoutAquaAffinity) speed *= 0.2;
        //if (outOfWaterButNotOnGround) speed *= 0.2;
        return 1.0 / speed;
    }

    private double getBreakTime(double blockHardness, int blockId, ItemEntry item) {
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
        System.out.println("canHarvestWithHand = " + canHarvestWithHand);
        System.out.println("correctTool = " + correctTool);
        System.out.println("itemToolType = " + toolType);
        System.out.println("toolTier = " + toolTier);
        System.out.println("isWoolBlock = " + isWoolBlock);
        System.out.println("isCobweb = " + isCobweb);
        //int efficiencyLoreLevel = Optional.ofNullable(item.getEnchantment(Enchantment.ID_EFFICIENCY))
        //        .map(Enchantment::getLevel).orElse(0);
        //int hasteEffectLevel = Optional.ofNullable(player.getEffect(Effect.HASTE))
        //       .map(Effect::getAmplifier).orElse(0);
        //boolean insideOfWaterWithoutAquaAffinity = player.isInsideOfWater() &&
        //        Optional.ofNullable(player.getInventory().getHelmet().getEnchantment(Enchantment.ID_WATER_WORKER))
        //                .map(Enchantment::getLevel).map(l -> l >= 1).orElse(false);
        //boolean outOfWaterButNotOnGround = (!player.isInsideOfWater()) && (!player.isOnGround());
        //return breakTime0(blockHardness, correctTool, canHarvestWithHand, blockId, itemToolType, itemTier,
        //        efficiencyLoreLevel, hasteEffectLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround);
        double returnValue = calculateBreakTime(blockHardness, toolTier, canHarvestWithHand, correctTool, toolType, isWoolBlock, isCobweb);
        System.out.println("returnValue = " + returnValue);
        return returnValue;
    }
}


