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
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.ChunkUtils;
import org.geysermc.connector.utils.Toolbox;

import java.util.Arrays;

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
                float blockHardness = BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(packet.getNewState().getId());

                PlayerInventory inventory = session.getInventory();
                ItemStack[] items = inventory.getItems();
                System.out.println(Arrays.deepToString(items));
                for (int i = 0; i != items.length; i++) {
                    System.out.println("(" + i + ") " + items[i]);
                }
                int itemHandSlot = inventory.getHeldItemSlot();
                System.out.println(itemHandSlot);
                ItemStack item = inventory.getItemInHand();
                System.out.println(item);
                /*if (itemStack.getNbt() == null) {
                    itemStack = new ItemStack(itemStack.getId(), itemStack.getAmount(), new CompoundTag(""));
                }*/
                //ItemEntry item = Toolbox.ITEM_ENTRIES.get(itemStack.getId());
                //System.out.println(item);
                double breakTime = Math.ceil(getBreakTime(blockHardness, packet.getNewState().getId()/*, item*/) * 20);
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

    private double toolBreakTimeBonus0(
            /*String toolType,*/ String toolTier/*, boolean isWoolBlock, boolean isCobweb*/) {
        //if (toolType == ItemTool.TYPE_SWORD) return isCobweb ? 15.0 : 1.0;
        //if (toolType == ItemTool.TYPE_SHEARS) return isWoolBlock ? 5.0 : 15.0;
        //if (toolType == ItemTool.TYPE_NONE) return 1.0;
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

    /*private static double speedBonusByEfficiencyLore0(int efficiencyLoreLevel) {
        if (efficiencyLoreLevel == 0) return 0;
        return efficiencyLoreLevel * efficiencyLoreLevel + 1;
    }*/

    /*private static double speedRateByHasteLore0(int hasteLoreLevel) {
        return 1.0 + (0.2 * hasteLoreLevel);
    }*/

    /*private static int toolType0(Item item) {
        if (item.isSword()) return ItemTool.TYPE_SWORD;
        if (item.isShovel()) return ItemTool.TYPE_SHOVEL;
        if (item.isPickaxe()) return ItemTool.TYPE_PICKAXE;
        if (item.isAxe()) return ItemTool.TYPE_AXE;
        if (item.isShears()) return ItemTool.TYPE_SHEARS;
        return ItemTool.TYPE_NONE;
    }*/

    /*private static boolean correctTool0(int blockToolType, Item item) {
        return (blockToolType == ItemTool.TYPE_SWORD && item.isSword()) ||
                (blockToolType == ItemTool.TYPE_SHOVEL && item.isShovel()) ||
                (blockToolType == ItemTool.TYPE_PICKAXE && item.isPickaxe()) ||
                (blockToolType == ItemTool.TYPE_AXE && item.isAxe()) ||
                (blockToolType == ItemTool.TYPE_SHEARS && item.isShears()) ||
                blockToolType == ItemTool.TYPE_NONE;
    }*/

    //http://minecraft.gamepedia.com/Breaking
    private double breakTime0(double blockHardness/*, String toolTier*/
            /*double blockHardness, boolean correctTool, boolean canHarvestWithHand,
                                     int blockId, int toolType, String toolTier, int efficiencyLoreLevel, int hasteEffectLevel,
                                     boolean insideOfWaterWithoutAquaAffinity, boolean outOfWaterButNotOnGround*/) {
        double baseTime = (/*(correctTool || canHarvestWithHand)*/true ? 1.5 : 5.0) * blockHardness;
        double speed = 1.0 / baseTime;
        //boolean isWoolBlock = blockId == Block.WOOL, isCobweb = blockId == Block.COBWEB;
        //if (correctTool)
        //speed *= toolBreakTimeBonus0(toolTier/*toolType, toolTier, isWoolBlock, isCobweb*/);
        //speed += speedBonusByEfficiencyLore0(efficiencyLoreLevel);
        //speed *= speedRateByHasteLore0(hasteEffectLevel);
        //if (insideOfWaterWithoutAquaAffinity) speed *= 0.2;
        //if (outOfWaterButNotOnGround) speed *= 0.2;
        return 1.0 / speed;
    }

    private double getBreakTime(double blockHardness, int blockId/*, ItemEntry item*/) {
        //Objects.requireNonNull(item, "getBreakTime: Item can not be null");
        //Objects.requireNonNull(player, "getBreakTime: Player can not be null");
        //boolean correctTool = correctTool0(getToolType(), item);
        //boolean canHarvestWithHand = canHarvestWithHand();
        //int blockId = getId();
        //int itemToolType = toolType0(item);
        //int itemTier = item.getTier();
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
        return breakTime0(blockHardness/*, item.getToolTier()*/);
    }

}
