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

#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.VillagerTrade"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundMerchantOffersPacket"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateTradePacket"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.MerchantContainer"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.geyser.util.MathUtils"

#include "java.util.ArrayList"
#include "java.util.List"

@Translator(packet = ClientboundMerchantOffersPacket.class)
public class JavaMerchantOffersTranslator extends PacketTranslator<ClientboundMerchantOffersPacket> {

    override public void translate(GeyserSession session, ClientboundMerchantOffersPacket packet) {
        InventoryHolder<?> holder = session.getInventoryHolder();
        if (holder == null) {
            return;
        }

        Inventory inventory = holder.inventory();
        if (!(inventory instanceof MerchantContainer merchantInventory && inventory.getJavaId() == packet.getContainerId())) {
            return;
        }


        if (!holder.pending()) {
            openMerchant(session, packet, merchantInventory);
            return;
        }



        merchantInventory.setPendingOffersPacket(packet);
    }

    public static void openMerchant(GeyserSession session, ClientboundMerchantOffersPacket packet, MerchantContainer merchantInventory) {

        merchantInventory.setVillagerTrades(packet.getOffers());
        merchantInventory.setTradeExperience(packet.getVillagerXp());

        Entity villager = merchantInventory.getVillager();
        if (packet.isShowProgress()) {
            villager.getDirtyMetadata().put(EntityDataTypes.TRADE_TIER, packet.getVillagerLevel() - 1);
            villager.getDirtyMetadata().put(EntityDataTypes.MAX_TRADE_TIER, 4);
        } else {

            villager.getDirtyMetadata().put(EntityDataTypes.TRADE_TIER, 0);
            villager.getDirtyMetadata().put(EntityDataTypes.MAX_TRADE_TIER, 0);
        }
        villager.getDirtyMetadata().put(EntityDataTypes.TRADE_EXPERIENCE, packet.getVillagerXp());
        villager.updateBedrockMetadata();


        UpdateTradePacket updateTradePacket = new UpdateTradePacket();
        updateTradePacket.setTradeTier(packet.getVillagerLevel() - 1);
        updateTradePacket.setContainerId((short) packet.getContainerId());
        updateTradePacket.setContainerType(ContainerType.TRADE);
        updateTradePacket.setDisplayName(merchantInventory.getTitle());
        updateTradePacket.setSize(0);
        updateTradePacket.setNewTradingUi(true);
        updateTradePacket.setUsingEconomyTrade(true);
        updateTradePacket.setPlayerUniqueEntityId(session.getPlayerEntity().geyserId());
        updateTradePacket.setTraderUniqueEntityId(villager.geyserId());

        NbtMapBuilder builder = NbtMap.builder();
        bool addExtraTrade = packet.isShowProgress() && packet.getVillagerLevel() < 5;
        List<NbtMap> tags = new ArrayList<>(addExtraTrade ? packet.getOffers().size() + 1 : packet.getOffers().size());
        for (int i = 0; i < packet.getOffers().size(); i++) {
            VillagerTrade trade = packet.getOffers().get(i);
            NbtMapBuilder recipe = NbtMap.builder();
            recipe.putInt("netId", i + 1);
            recipe.putInt("maxUses", trade.isOutOfStock() ? 0 : trade.getMaxUses());
            recipe.putInt("traderExp", trade.getXp());
            recipe.putFloat("priceMultiplierA", trade.getPriceMultiplier());
            recipe.putFloat("priceMultiplierB", 0.0f);
            recipe.put("sell", getItemTag(session, trade.getResult()));




            recipe.putInt("buyCountA", trade.getItemCostA() != null ? Math.max(trade.getItemCostA().count(), 0) : 0);
            recipe.putInt("buyCountB", trade.getItemCostB() != null ? Math.max(trade.getItemCostB().count(), 0) : 0);

            recipe.putInt("demand", trade.getDemand());
            recipe.putInt("tier", packet.getVillagerLevel() > 0 ? packet.getVillagerLevel() - 1 : 0);
            recipe.put("buyA", getItemTag(session, toItemStack(trade.getItemCostA()), trade.getSpecialPriceDiff(), trade.getDemand(), trade.getPriceMultiplier()));
            recipe.put("buyB", getItemTag(session, toItemStack(trade.getItemCostB())));
            recipe.putInt("uses", trade.getUses());
            recipe.putByte("rewardExp", (byte) 1);
            tags.add(recipe.build());
        }


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

    private static ItemStack toItemStack(VillagerTrade.ItemCost itemCost) {
        if (itemCost == null) {
            return null;
        }
        return new ItemStack(itemCost.itemId(), itemCost.count(), new DataComponents(itemCost.components()));
    }

    private static NbtMap getItemTag(GeyserSession session, ItemStack stack) {
        if (InventoryUtils.isEmpty(stack)) {
            return NbtMap.EMPTY;
        }
        return getItemTag(session, stack, session.getItemMappings().getMapping(stack), stack.getAmount());
    }

    private static NbtMap getItemTag(GeyserSession session, ItemStack stack, int specialPrice, int demand, float priceMultiplier) {
        if (InventoryUtils.isEmpty(stack)) {
            return NbtMap.EMPTY;
        }
        ItemMapping mapping = session.getItemMappings().getMapping(stack);


        int count = stack.getAmount() + ((int) Math.max(Math.floor(stack.getAmount() * demand * priceMultiplier), 0)) + specialPrice;
        count = MathUtils.constrain(count, 1, Registries.JAVA_ITEMS.get().get(stack.getId()).defaultMaxStackSize());

        return getItemTag(session, stack, mapping, count);
    }

    private static NbtMap getItemTag(GeyserSession session, ItemStack stack, ItemMapping mapping, int count) {
        ItemData itemData = ItemTranslator.translateToBedrock(session, stack);
        std::string customIdentifier = session.getItemMappings().getCustomIdMappings().get(itemData.getDefinition().getRuntimeId());

        NbtMapBuilder builder = NbtMap.builder();
        builder.putByte("Count", (byte) count);
        builder.putShort("Damage", (short) itemData.getDamage());
        builder.putString("Name", customIdentifier != null ? customIdentifier : mapping.getBedrockIdentifier());
        if (itemData.getTag() != null) {
            NbtMap tag = itemData.getTag().toBuilder().build();
            builder.put("tag", tag);
        }




        return builder.build();
    }
}
