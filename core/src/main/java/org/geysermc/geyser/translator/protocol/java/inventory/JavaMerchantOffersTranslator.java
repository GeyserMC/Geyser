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

package org.geysermc.geyser.translator.protocol.java.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.VillagerTrade;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundMerchantOffersPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.UpdateTradePacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.MerchantContainer;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.MathUtils;

import java.util.ArrayList;
import java.util.List;

@Translator(packet = ClientboundMerchantOffersPacket.class)
public class JavaMerchantOffersTranslator extends PacketTranslator<ClientboundMerchantOffersPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundMerchantOffersPacket packet) {
        Inventory openInventory = session.getOpenInventory();
        if (!(openInventory instanceof MerchantContainer merchantInventory && openInventory.getJavaId() == packet.getContainerId())) {
            return;
        }

        // No previous inventory was closed -> no need of queuing the merchant inventory
        if (!openInventory.isPending()) {
            openMerchant(session, packet, merchantInventory);
            return;
        }

        // The inventory is declared as pending due to previous closing inventory -> leads to an incorrect order of execution
        // Handled in BedrockContainerCloseTranslator
        merchantInventory.setPendingOffersPacket(packet);
    }

    public static void openMerchant(GeyserSession session, ClientboundMerchantOffersPacket packet, MerchantContainer merchantInventory) {
        // Retrieve the fake villager involved in the trade, and update its metadata to match with the window information
        merchantInventory.setVillagerTrades(packet.getTrades());
        Entity villager = merchantInventory.getVillager();
        villager.getDirtyMetadata().put(EntityData.TRADE_TIER, packet.getVillagerLevel() - 1);
        villager.getDirtyMetadata().put(EntityData.MAX_TRADE_TIER, 4);
        villager.getDirtyMetadata().put(EntityData.TRADE_XP, packet.getExperience());
        villager.updateBedrockMetadata();

        // Construct the packet that opens the trading window
        UpdateTradePacket updateTradePacket = new UpdateTradePacket();
        updateTradePacket.setTradeTier(packet.getVillagerLevel() - 1);
        updateTradePacket.setContainerId((short) packet.getContainerId());
        updateTradePacket.setContainerType(ContainerType.TRADE);
        updateTradePacket.setDisplayName(session.getOpenInventory().getTitle());
        updateTradePacket.setSize(0);
        updateTradePacket.setNewTradingUi(true);
        updateTradePacket.setUsingEconomyTrade(true);
        updateTradePacket.setPlayerUniqueEntityId(session.getPlayerEntity().getGeyserId());
        updateTradePacket.setTraderUniqueEntityId(villager.getGeyserId());

        NbtMapBuilder builder = NbtMap.builder();
        boolean addExtraTrade = packet.isRegularVillager() && packet.getVillagerLevel() < 5;
        List<NbtMap> tags = new ArrayList<>(addExtraTrade ? packet.getTrades().length + 1 : packet.getTrades().length);
        for (int i = 0; i < packet.getTrades().length; i++) {
            VillagerTrade trade = packet.getTrades()[i];
            NbtMapBuilder recipe = NbtMap.builder();
            recipe.putInt("netId", i + 1);
            recipe.putInt("maxUses", trade.isTradeDisabled() ? 0 : trade.getMaxUses());
            recipe.putInt("traderExp", trade.getXp());
            recipe.putFloat("priceMultiplierA", trade.getPriceMultiplier());
            recipe.putFloat("priceMultiplierB", 0.0f);
            recipe.put("sell", getItemTag(session, trade.getOutput()));

            // The buy count before demand and special price adjustments
            // The first input CAN be null as of Java 1.19.0/Bedrock 1.19.10
            // Replicable item: https://gist.github.com/Camotoy/3f3f23d1f80981d1b4472bdb23bba698 from https://github.com/GeyserMC/Geyser/issues/3171
            recipe.putInt("buyCountA", trade.getFirstInput() != null ? Math.max(trade.getFirstInput().getAmount(), 0) : 0);
            recipe.putInt("buyCountB", trade.getSecondInput() != null ? Math.max(trade.getSecondInput().getAmount(), 0) : 0);

            recipe.putInt("demand", trade.getDemand()); // Seems to have no effect
            recipe.putInt("tier", packet.getVillagerLevel() > 0 ? packet.getVillagerLevel() - 1 : 0); // -1 crashes client
            recipe.put("buyA", getItemTag(session, trade.getFirstInput(), trade.getSpecialPrice(), trade.getDemand(), trade.getPriceMultiplier()));
            recipe.put("buyB", getItemTag(session, trade.getSecondInput()));
            recipe.putInt("uses", trade.getNumUses());
            recipe.putByte("rewardExp", (byte) 1);
            tags.add(recipe.build());
        }

        //Hidden trade to fix visual experience bug
        if (addExtraTrade) {
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

        List<NbtMap> expTags = new ArrayList<>(5);
        expTags.add(NbtMap.builder().putInt("0", 0).build());
        expTags.add(NbtMap.builder().putInt("1", 10).build());
        expTags.add(NbtMap.builder().putInt("2", 70).build());
        expTags.add(NbtMap.builder().putInt("3", 150).build());
        expTags.add(NbtMap.builder().putInt("4", 250).build());
        builder.putList("TierExpRequirements", NbtType.COMPOUND, expTags);

        updateTradePacket.setOffers(builder.build());
        session.sendUpstreamPacket(updateTradePacket);
    }

    private static NbtMap getItemTag(GeyserSession session, ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) { // Negative item counts appear as air on Java
            return NbtMap.EMPTY;
        }
        return getItemTag(session, stack, session.getItemMappings().getMapping(stack), stack.getAmount());
    }

    private static NbtMap getItemTag(GeyserSession session, ItemStack stack, int specialPrice, int demand, float priceMultiplier) {
        if (stack == null || stack.getAmount() <= 0) { // Negative item counts appear as air on Java
            return NbtMap.EMPTY;
        }
        ItemMapping mapping = session.getItemMappings().getMapping(stack);

        // Bedrock expects all price adjustments to be applied to the item's count
        int count = stack.getAmount() + ((int) Math.max(Math.floor(stack.getAmount() * demand * priceMultiplier), 0)) + specialPrice;
        count = MathUtils.constrain(count, 1, mapping.getStackSize());

        return getItemTag(session, stack, mapping, count);
    }

    private static NbtMap getItemTag(GeyserSession session, ItemStack stack, ItemMapping mapping, int count) {
        ItemData itemData = ItemTranslator.translateToBedrock(session, stack);
        String customIdentifier = session.getItemMappings().getCustomIdMappings().get(itemData.getId());

        NbtMapBuilder builder = NbtMap.builder();
        builder.putByte("Count", (byte) count);
        builder.putShort("Damage", (short) itemData.getDamage());
        builder.putString("Name", customIdentifier != null ? customIdentifier : mapping.getBedrockIdentifier());
        if (itemData.getTag() != null) {
            NbtMap tag = itemData.getTag().toBuilder().build();
            builder.put("tag", tag);
        }

        // Implementation note: previously we added a block tag to fix some blocks (black concrete?) that wouldn't stack
        // after buying. This no longer seems to be an issue as of Bedrock 1.18.30, and including it breaks sugar canes.

        return builder.build();
    }
}
