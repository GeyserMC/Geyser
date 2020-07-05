/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.VillagerTrade;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerTradeListPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.UpdateTradePacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;

import java.util.ArrayList;
import java.util.List;

@Translator(packet = ServerTradeListPacket.class)
public class JavaTradeListTranslator extends PacketTranslator<ServerTradeListPacket> {

    @Override
    public void translate(ServerTradeListPacket packet, GeyserSession session) {
        Entity villager = session.getPlayerEntity();
        session.setVillagerTrades(packet.getTrades());
        villager.getMetadata().put(EntityData.TRADE_TIER, packet.getVillagerLevel() - 1);
        villager.getMetadata().put(EntityData.MAX_TRADE_TIER, 4);
        villager.getMetadata().put(EntityData.TRADE_XP, packet.getExperience());
        villager.updateBedrockMetadata(session);

        UpdateTradePacket updateTradePacket = new UpdateTradePacket();
        updateTradePacket.setTradeTier(packet.getVillagerLevel() - 1);
        updateTradePacket.setContainerId((short) packet.getWindowId()); //TODO: CHECK THIS AND THE ONE BELOW
        updateTradePacket.setContainerType(ContainerType.TRADE);
        String displayName;
        Entity realVillager = session.getEntityCache().getEntityByGeyserId(session.getLastInteractedVillagerEid());
        if (realVillager != null && realVillager.getMetadata().containsKey(EntityData.NAMETAG) && realVillager.getMetadata().getString(EntityData.NAMETAG) != null) {
            displayName = realVillager.getMetadata().getString(EntityData.NAMETAG);
        } else {
            displayName = packet.isRegularVillager() ? "Villager" : "Wandering Trader";
        }
        updateTradePacket.setDisplayName(displayName);
        updateTradePacket.setSize(0);
        updateTradePacket.setNewTradingUi(true);
        updateTradePacket.setUsingEconomyTrade(true);
        updateTradePacket.setPlayerUniqueEntityId(session.getPlayerEntity().getGeyserId());
        updateTradePacket.setTraderUniqueEntityId(session.getPlayerEntity().getGeyserId());
        NbtMapBuilder builder = NbtMap.builder();
        List<NbtMap> tags = new ArrayList<>();
        for (VillagerTrade trade : packet.getTrades()) {
            NbtMapBuilder recipe = NbtMap.builder();
            recipe.putInt("maxUses", trade.getMaxUses());
            recipe.putInt("traderExp", trade.getXp());
            recipe.putFloat("priceMultiplierA", trade.getPriceMultiplier());
            recipe.put("sell", getItemTag(session, trade.getOutput(), 0));
            recipe.putFloat("priceMultiplierB", 0.0f);
            recipe.putInt("buyCountB", trade.getSecondInput() != null ? trade.getSecondInput().getAmount() : 0);
            recipe.putInt("buyCountA", trade.getFirstInput().getAmount());
            recipe.putInt("demand", trade.getDemand());
            recipe.putInt("tier", packet.getVillagerLevel() - 1);
            recipe.put("buyA", getItemTag(session, trade.getFirstInput(), trade.getSpecialPrice()));
            if (trade.getSecondInput() != null) {
                recipe.put("buyB", getItemTag(session, trade.getSecondInput(), 0));
            }
            recipe.putInt("uses", trade.getNumUses());
            recipe.putByte("rewardExp", (byte) 1);
            tags.add(recipe.build());
        }

        //Hidden trade to fix visual experience bug
        if (packet.isRegularVillager() && packet.getVillagerLevel() < 5) {
            tags.add(NbtMap.builder()
                    .putInt("maxUses", 0)
                    .putInt("traderExp", 0)
                    .putFloat("priceMultiplierA", 0.0f)
                    .putFloat("priceMultiplierB", 0.0f)
                    .putInt("buyCountB", 0)
                    .putInt("buyCountA", 0)
                    .putInt("demand", 0)
                    .putInt("tier", 5)
                    .putInt("uses", 0)
                    .putByte("rewardExp", (byte) 0)
                    .build());
        }

        builder.putList("Recipes", NbtType.COMPOUND, tags);
        List<NbtMap> expTags = new ArrayList<>();
        expTags.add(NbtMap.builder().putInt("0", 0).build());
        expTags.add(NbtMap.builder().putInt("1", 10).build());
        expTags.add(NbtMap.builder().putInt("2", 70).build());
        expTags.add(NbtMap.builder().putInt("3", 150).build());
        expTags.add(NbtMap.builder().putInt("4", 250).build());
        builder.putList("TierExpRequirements", NbtType.COMPOUND, expTags);
        updateTradePacket.setOffers(builder.build());
        session.sendUpstreamPacket(updateTradePacket);
    }

    private NbtMap getItemTag(GeyserSession session, ItemStack stack, int specialPrice) {
        ItemData itemData = ItemTranslator.translateToBedrock(session, stack);
        ItemEntry itemEntry = ItemRegistry.getItem(stack);
        NbtMapBuilder builder = NbtMap.builder();
        builder.putByte("Count", (byte) (Math.max(itemData.getCount() + specialPrice, 1)));
        builder.putShort("Damage", itemData.getDamage());
        builder.putShort("id", (short) itemEntry.getBedrockId());
        if (itemData.getTag() != null) {
            NbtMap tag = itemData.getTag().toBuilder().build();
            builder.put("tag", tag);
        }
        return builder.build();
    }
}
