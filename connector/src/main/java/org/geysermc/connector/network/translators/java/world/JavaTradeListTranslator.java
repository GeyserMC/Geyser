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
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.ItemData;
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
        updateTradePacket.setWindowId((short) packet.getWindowId());
        updateTradePacket.setWindowType((short) ContainerType.TRADING.id());
        String displayName;
        Entity realVillager = session.getEntityCache().getEntityByGeyserId(session.getLastInteractedVillagerEid());
        if (realVillager != null && realVillager.getMetadata().containsKey(EntityData.NAMETAG) && realVillager.getMetadata().getString(EntityData.NAMETAG) != null) {
            displayName = realVillager.getMetadata().getString(EntityData.NAMETAG);
        } else {
            displayName = packet.isRegularVillager() ? "Villager" : "Wandering Trader";
        }
        updateTradePacket.setDisplayName(displayName);
        updateTradePacket.setUnknownInt(0);
        updateTradePacket.setScreen2(true);
        updateTradePacket.setWilling(true);
        updateTradePacket.setPlayerUniqueEntityId(session.getPlayerEntity().getGeyserId());
        updateTradePacket.setTraderUniqueEntityId(session.getPlayerEntity().getGeyserId());
        CompoundTagBuilder builder = CompoundTagBuilder.builder();
        List<CompoundTag> tags = new ArrayList<>();
        for (VillagerTrade trade : packet.getTrades()) {
            CompoundTagBuilder recipe = CompoundTagBuilder.builder();
            recipe.intTag("maxUses", trade.getMaxUses());
            recipe.intTag("traderExp", trade.getXp());
            recipe.floatTag("priceMultiplierA", trade.getPriceMultiplier());
            recipe.tag(getItemTag(session, trade.getOutput(), "sell", 0));
            recipe.floatTag("priceMultiplierB", 0.0f);
            recipe.intTag("buyCountB", trade.getSecondInput() != null ? trade.getSecondInput().getAmount() : 0);
            recipe.intTag("buyCountA", trade.getFirstInput().getAmount());
            recipe.intTag("demand", trade.getDemand());
            recipe.intTag("tier", packet.getVillagerLevel() - 1);
            recipe.tag(getItemTag(session, trade.getFirstInput(), "buyA", trade.getSpecialPrice()));
            if (trade.getSecondInput() != null) {
                recipe.tag(getItemTag(session, trade.getSecondInput(), "buyB", 0));
            }
            recipe.intTag("uses", trade.getNumUses());
            recipe.byteTag("rewardExp", (byte) 1);
            tags.add(recipe.buildRootTag());
        }

        //Hidden trade to fix visual experience bug
        if (packet.isRegularVillager() && packet.getVillagerLevel() < 5) {
            tags.add(CompoundTagBuilder.builder()
                    .intTag("maxUses", 0)
                    .intTag("traderExp", 0)
                    .floatTag("priceMultiplierA", 0.0f)
                    .floatTag("priceMultiplierB", 0.0f)
                    .intTag("buyCountB", 0)
                    .intTag("buyCountA", 0)
                    .intTag("demand", 0)
                    .intTag("tier", 5)
                    .intTag("uses", 0)
                    .byteTag("rewardExp", (byte) 0)
                    .buildRootTag());
        }

        builder.listTag("Recipes", CompoundTag.class, tags);
        List<CompoundTag> expTags = new ArrayList<>();
        expTags.add(CompoundTagBuilder.builder().intTag("0", 0).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("1", 10).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("2", 70).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("3", 150).buildRootTag());
        expTags.add(CompoundTagBuilder.builder().intTag("4", 250).buildRootTag());
        builder.listTag("TierExpRequirements", CompoundTag.class, expTags);
        updateTradePacket.setOffers(builder.buildRootTag());
        session.sendUpstreamPacket(updateTradePacket);
    }

    private CompoundTag getItemTag(GeyserSession session, ItemStack stack, String name, int specialPrice) {
        ItemData itemData = ItemTranslator.translateToBedrock(session, stack);
        ItemEntry itemEntry = ItemRegistry.getItem(stack);
        CompoundTagBuilder builder = CompoundTagBuilder.builder();
        builder.byteTag("Count", (byte) (Math.max(itemData.getCount() + specialPrice, 1)));
        builder.shortTag("Damage", itemData.getDamage());
        builder.shortTag("id", (short) itemEntry.getBedrockId());
        if (itemData.getTag() != null) {
            CompoundTag tag = itemData.getTag().toBuilder().build("tag");
            builder.tag(tag);
        }
        return builder.build(name);
    }
}
