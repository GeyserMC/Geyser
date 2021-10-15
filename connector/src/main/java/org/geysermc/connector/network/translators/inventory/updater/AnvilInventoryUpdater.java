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

package org.geysermc.connector.network.translators.inventory.updater;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.item.Enchantment.JavaEnchantment;
import org.geysermc.connector.registry.Registries;
import org.geysermc.connector.registry.type.EnchantmentData;

import java.util.EnumMap;
import java.util.Map;

public class AnvilInventoryUpdater extends InventoryUpdater {
    public static final AnvilInventoryUpdater INSTANCE = new AnvilInventoryUpdater();

    @Override
    public void updateInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        super.updateInventory(translator, session, inventory);

        for (int i = 0; i < translator.size; i++) {
            final int bedrockSlot = translator.javaSlotToBedrock(i);
            if (bedrockSlot == 50)
                continue;
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(bedrockSlot);
            slotPacket.setItem(inventory.getItem(i).getItemData(session));
            if (i == 0) { // Input item slot
                slotPacket.setItem(hijackRepairCost(session, inventory, slotPacket.getItem()));
            }
            session.sendUpstreamPacket(slotPacket);
        }
    }

    @Override
    public boolean updateSlot(InventoryTranslator translator, GeyserSession session, Inventory inventory, int javaSlot) {
        if (super.updateSlot(translator, session, inventory, javaSlot))
            return true;

        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(ContainerId.UI);
        slotPacket.setSlot(translator.javaSlotToBedrock(javaSlot));
        slotPacket.setItem(inventory.getItem(javaSlot).getItemData(session));
        if (javaSlot == 0) { // Input item slot
            slotPacket.setItem(hijackRepairCost(session, inventory, slotPacket.getItem()));
        } else {
            updateSlot(translator, session, inventory, 0);
        }
        session.sendUpstreamPacket(slotPacket);
        return true;
    }

    private ItemData hijackRepairCost(GeyserSession session, Inventory inventory, ItemData itemData) {
        // Fix level count by adjusting repair cost
        int newRepairCost = calcLevelDiff(session, inventory.getItem(0), inventory.getItem(1));
        if (itemData.getTag() != null) {
            newRepairCost += itemData.getTag().getInt("RepairCost", 0);
            // TODO There has to be a better way
            NbtMapBuilder newNBTMapBuilder = itemData.getTag().toBuilder();
            newNBTMapBuilder.put("RepairCost", newRepairCost);
            return itemData.toBuilder().tag(newNBTMapBuilder.build()).build();
        }
        return itemData.toBuilder().tag(NbtMap.builder().putInt("RepairCost", newRepairCost).build()).build();
    }

    /**
     * Calculate the difference between xp levels of Java anvil mechanics and Bedrock anvil mechanics
     * @param session the geyser session
     * @param input left item stack
     * @param material right item stack
     * @return the number of levels needed to match Java edition
     */
    private int calcLevelDiff(GeyserSession session, GeyserItemStack input, GeyserItemStack material) {
        if (input.isEmpty() || material.isEmpty()) {
            return 0;
        }

        boolean isInputEnchantedBook = isEnchantedBook(session, input);
        boolean isMaterialEnchantedBook = isEnchantedBook(session, material);
        if (input.getJavaId() != material.getJavaId() && !isMaterialEnchantedBook) {
            return 0;
        }

        int diff = 0;
        boolean creativeOverride = session.getGameMode() == GameMode.CREATIVE; // TODO: Check abilities instead
        boolean hasCompatible = false;
        Map<JavaEnchantment, Integer> combinedEnchantments = getEnchantments(session, input);
        for (Map.Entry<JavaEnchantment, Integer> entry : getEnchantments(session, material).entrySet()) {
            JavaEnchantment enchantment = entry.getKey();
            EnchantmentData data = Registries.ENCHANTMENTS.get(enchantment);
            if (data == null) {
                GeyserConnector.getInstance().getLogger().debug("Java enchantment not in registry: " + enchantment);
                continue;
            }

            boolean canApply = isInputEnchantedBook || data.validItems().contains(input.getJavaId());
            for (JavaEnchantment incompatible : data.incompatibleEnchantments()) {
                if (combinedEnchantments.containsKey(incompatible)) {
                    canApply = false;
                    diff++;
                }
            }

            if (creativeOverride || canApply) {
                hasCompatible = true;
                int currentLevel = combinedEnchantments.getOrDefault(enchantment, 0);
                int newLevel = entry.getValue();
                if (newLevel == currentLevel) {
                    newLevel++;
                }
                newLevel = Math.max(currentLevel, newLevel);
                if (newLevel > data.maxLevel()) {
                    newLevel = data.maxLevel();
                }
                combinedEnchantments.put(enchantment, newLevel);

                int rarityMultiplier = data.rarityMultiplier();
                if (isMaterialEnchantedBook && rarityMultiplier > 1) {
                    rarityMultiplier /= 2;
                }
                int bedrockRarityMultiplier = rarityMultiplier;
                if (enchantment == JavaEnchantment.IMPALING) {
                    // Multiplier is halved on Bedrock for some reason
                    bedrockRarityMultiplier /= 2;
                } else if (enchantment == JavaEnchantment.SWEEPING) {
                    // Doesn't exist on Bedrock
                    bedrockRarityMultiplier = 0;
                }

                int javaCost = rarityMultiplier * newLevel;
                int bedrockCost = canApply ? bedrockRarityMultiplier * (newLevel - currentLevel) : 0;
                diff += javaCost - bedrockCost;
            }
        }

        if (!hasCompatible) {
            return 0;
        }
        return diff;
    }

    private Map<JavaEnchantment, Integer> getEnchantments(GeyserSession session, GeyserItemStack itemStack) {
        if (itemStack.getNbt() == null) {
            return Object2ObjectMaps.emptyMap();
        }
        EnumMap<JavaEnchantment, Integer> enchantments = new EnumMap<>(JavaEnchantment.class);
        Tag enchantmentTag;
        if (isEnchantedBook(session, itemStack)) {
            enchantmentTag = itemStack.getNbt().get("StoredEnchantments");
        } else {
            enchantmentTag = itemStack.getNbt().get("Enchantments");
        }
        if (enchantmentTag instanceof ListTag listTag) {
            for (Tag tag : listTag.getValue()) {
                if (tag instanceof CompoundTag enchantTag) {
                    if (enchantTag.get("id") instanceof StringTag javaEnchId) {
                        JavaEnchantment enchantment = JavaEnchantment.getByJavaIdentifier(javaEnchId.getValue());
                        if (enchantment == null) {
                            GeyserConnector.getInstance().getLogger().debug("Unknown java enchantment: " + javaEnchId.getValue());
                            continue;
                        }

                        Tag javaEnchLvl = enchantTag.get("lvl");
                        if (!(javaEnchLvl instanceof ShortTag || javaEnchLvl instanceof IntTag))
                            continue;

                        enchantments.put(enchantment, ((Number) javaEnchLvl.getValue()).intValue());
                    }
                }
            }
        }
        return enchantments;
    }

    private boolean isEnchantedBook(GeyserSession session, GeyserItemStack itemStack) {
        return itemStack.getJavaId() == session.getItemMappings().getStoredItems().enchantedBook().getJavaId();
    }
}
