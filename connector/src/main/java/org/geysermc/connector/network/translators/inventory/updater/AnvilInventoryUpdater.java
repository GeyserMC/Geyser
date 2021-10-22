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
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.inventory.AnvilContainer;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.item.Enchantment.JavaEnchantment;
import org.geysermc.connector.registry.Registries;
import org.geysermc.connector.registry.type.EnchantmentData;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class AnvilInventoryUpdater extends InventoryUpdater {
    public static final AnvilInventoryUpdater INSTANCE = new AnvilInventoryUpdater();

    @Override
    public void updateInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        super.updateInventory(translator, session, inventory);
        updateInventoryState(session, (AnvilContainer) inventory);
        int targetSlot = getTargetSlot(inventory);
        for (int i = 0; i < translator.size; i++) {
            final int bedrockSlot = translator.javaSlotToBedrock(i);
            if (bedrockSlot == 50)
                continue;
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(bedrockSlot);
            slotPacket.setItem(inventory.getItem(i).getItemData(session));
            if (i == targetSlot) {
                slotPacket.setItem(hijackRepairCost(session, (AnvilContainer) inventory, slotPacket.getItem()));
            }
            session.sendUpstreamPacket(slotPacket);
        }
    }

    @Override
    public boolean updateSlot(InventoryTranslator translator, GeyserSession session, Inventory inventory, int javaSlot) {
        if (super.updateSlot(translator, session, inventory, javaSlot))
            return true;
        updateInventoryState(session, (AnvilContainer) inventory);
        int targetSlot = getTargetSlot(inventory);

        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(ContainerId.UI);
        slotPacket.setSlot(translator.javaSlotToBedrock(javaSlot));
        slotPacket.setItem(inventory.getItem(javaSlot).getItemData(session));
        if (javaSlot == targetSlot) {
            slotPacket.setItem(hijackRepairCost(session, (AnvilContainer) inventory, slotPacket.getItem()));
        } else {
            session.sendUpstreamPacket(slotPacket);

            slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(translator.javaSlotToBedrock(targetSlot));
            slotPacket.setItem(hijackRepairCost(session, (AnvilContainer) inventory, inventory.getItem(targetSlot).getItemData(session)));
        }
        session.sendUpstreamPacket(slotPacket);
        return true;
    }

    private void updateInventoryState(GeyserSession session, AnvilContainer anvilContainer) {
        GeyserItemStack input = anvilContainer.getItem(0);
        if (!input.equals(anvilContainer.getLastInput())) {
            anvilContainer.setLastInput(input.copy());
            anvilContainer.setUseJavaLevelCost(false);

            // Changing the item in the input slot resets the name field on Bedrock, but
            // does not result in a FilterTextPacket
            ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket("");
            session.sendDownstreamPacket(renameItemPacket);
        }

        GeyserItemStack material = anvilContainer.getItem(1);
        if (!material.equals(anvilContainer.getLastMaterial())) {
            anvilContainer.setLastMaterial(material.copy());
            anvilContainer.setUseJavaLevelCost(false);
        }
    }

    private int getTargetSlot(Inventory inventory) {
        if (!inventory.getItem(1).isEmpty()) {
            return 1;
        }
        return 0;
    }

    private ItemData hijackRepairCost(GeyserSession session, AnvilContainer anvilContainer, ItemData itemData) {
        // Fix level count by adjusting repair cost
        int newRepairCost;
        if (anvilContainer.isUseJavaLevelCost()) {
            newRepairCost = anvilContainer.getJavaLevelCost();
        } else {
            // Did not receive a ServerWindowPropertyPacket with the level cost
            newRepairCost = calcLevelCost(session, anvilContainer, false);
        }
        newRepairCost -= calcLevelCost(session, anvilContainer, true);
        if (newRepairCost == 0) {
            return itemData;
        }

        NbtMapBuilder tagBuilder = NbtMap.builder();
        if (itemData.getTag() != null) {
            newRepairCost += itemData.getTag().getInt("RepairCost", 0);
            tagBuilder.putAll(itemData.getTag());
        }
        tagBuilder.put("RepairCost", newRepairCost);
        return itemData.toBuilder().tag(tagBuilder.build()).build();
    }

    /**
     * Calculate the number of levels needed to combine/rename an item
     *
     * @param session        the geyser session
     * @param anvilContainer the anvil container
     * @param bedrock        True to count enchantments like Bedrock
     * @return the number of levels needed
     */
    public int calcLevelCost(GeyserSession session, AnvilContainer anvilContainer, boolean bedrock) {
        GeyserItemStack input = anvilContainer.getItem(0);
        GeyserItemStack material = anvilContainer.getItem(1);
        GeyserItemStack result = anvilContainer.getItem(2);

        if (input.isEmpty()) {
            return 0;
        }
        int totalRepairCost = getRepairCost(input);
        int cost = 0;
        if (!material.isEmpty()) {
            totalRepairCost += getRepairCost(material);
            if (isCombining(session, input, material)) {
                if (hasDurability(session, input) && input.getJavaId() == material.getJavaId()) {
                    cost += calcMergeRepairCost(session, input, material);
                }

                int enchantmentLevelCost = calcMergeEnchantmentCost(session, input, material, bedrock);
                if (enchantmentLevelCost != -1) {
                    cost += enchantmentLevelCost;
                } else if (cost == 0) {
                    // Can't repair or merge enchantments
                    return -1;
                }
            } else if (hasDurability(session, input) && isRepairing(session, input, material)) {
                cost = calcRepairLevelCost(session, input, material);
                if (cost == -1) {
                    return -1;
                }
            } else {
                return -1;
            }
        }

        int totalCost = totalRepairCost + cost;
        // This should really check the name field, but that requires
        // the localized name of the item and other pains
        boolean renaming = !Objects.equals(getCustomName(session, input), getCustomName(session, result));
        if (renaming) {
            totalCost++;
            if (cost == 0 && totalCost >= 40) {
                totalCost = 39;
            }
        }
        return totalCost;
    }

    /**
     * Calculate the levels needed to repair an item with its repair material
     * E.g. iron_sword + iron_ingot
     *
     * @param session  Geyser session
     * @param input    an item with durability
     * @param material the item's respective repair material
     * @return the number of levels needed or 0 if it is not possible to repair any further
     */
    private int calcRepairLevelCost(GeyserSession session, GeyserItemStack input, GeyserItemStack material) {
        int newDamage = getDamage(input);
        int unitRepair = Math.min(newDamage, input.getMapping(session).getMaxDamage() / 4);
        if (unitRepair <= 0) {
            return -1;
        }
        for (int i = 0; i < material.getAmount(); i++) {
            newDamage -= unitRepair;
            unitRepair = Math.min(newDamage, input.getMapping(session).getMaxDamage() / 4);
            if (unitRepair <= 0) {
                return i + 1;
            }
        }
        return material.getAmount();
    }

    /**
     * Calculate the levels cost for repairing items by combining two of the same item
     *
     * @param session  Geyser session
     * @param input    an item with durability
     * @param material a matching item
     * @return the number of levels needed or 0 if it is not possible to repair any further
     */
    private int calcMergeRepairCost(GeyserSession session, GeyserItemStack input, GeyserItemStack material) {
        if (getDamage(input) > 0 && getDamage(material) < (material.getMapping(session).getMaxDamage() * 112 / 100)) {
            return 2;
        }
        return 0;
    }

    /**
     * Calculate the levels needed for combining the enchantments of two items
     *
     * @param session  Geyser session
     * @param input    an item with durability
     * @param material a matching item
     * @param bedrock  True to count enchantments like Bedrock, False to count like Java
     * @return the number of levels needed or -1 if no enchantments can be applied
     */
    private int calcMergeEnchantmentCost(GeyserSession session, GeyserItemStack input, GeyserItemStack material, boolean bedrock) {
        boolean hasCompatible = false;
        Map<JavaEnchantment, Integer> combinedEnchantments = getEnchantments(session, input);
        int cost = 0;
        for (Map.Entry<JavaEnchantment, Integer> entry : getEnchantments(session, material).entrySet()) {
            JavaEnchantment enchantment = entry.getKey();
            EnchantmentData data = Registries.ENCHANTMENTS.get(enchantment);
            if (data == null) {
                GeyserConnector.getInstance().getLogger().debug("Java enchantment not in registry: " + enchantment);
                continue;
            }

            boolean canApply = isEnchantedBook(session, input) || data.validItems().contains(input.getJavaId());
            for (JavaEnchantment incompatible : data.incompatibleEnchantments()) {
                if (combinedEnchantments.containsKey(incompatible)) {
                    canApply = false;
                    if (!bedrock) {
                        cost++;
                    }
                }
            }

            if (canApply || (!bedrock && session.getGameMode() == GameMode.CREATIVE)) {
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
                if (isEnchantedBook(session, material) && rarityMultiplier > 1) {
                    rarityMultiplier /= 2;
                }
                if (bedrock) {
                    if (newLevel > currentLevel) {
                        hasCompatible = true;
                    }
                    if (enchantment == JavaEnchantment.IMPALING) {
                        // Multiplier is halved on Bedrock for some reason
                        rarityMultiplier /= 2;
                    } else if (enchantment == JavaEnchantment.SWEEPING) {
                        // Doesn't exist on Bedrock
                        rarityMultiplier = 0;
                    }
                    cost += rarityMultiplier * (newLevel - currentLevel);
                } else {
                    hasCompatible = true;
                    cost += rarityMultiplier * newLevel;
                }
            }
        }

        if (!hasCompatible) {
            return -1;
        }
        return cost;
    }

    private Map<JavaEnchantment, Integer> getEnchantments(GeyserSession session, GeyserItemStack itemStack) {
        if (itemStack.getNbt() == null) {
            return Object2ObjectMaps.emptyMap();
        }
        Map<JavaEnchantment, Integer> enchantments = new EnumMap<>(JavaEnchantment.class);
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

    private boolean isCombining(GeyserSession session, GeyserItemStack input, GeyserItemStack material) {
        return isEnchantedBook(session, material) || (input.getJavaId() == material.getJavaId() && hasDurability(session, input));
    }

    private boolean isRepairing(GeyserSession session, GeyserItemStack input, GeyserItemStack material) {
        return input.getMapping(session).getRepairMaterials().contains(material.getMapping(session).getJavaIdentifier());
    }

    private int getTagIntValueOr(GeyserItemStack itemStack, String tagName, int defaultValue) {
        if (itemStack.getNbt() != null) {
            Tag tag = itemStack.getNbt().get(tagName);
            if (tag != null && tag.getValue() instanceof Number value) {
                return value.intValue();
            }
        }
        return defaultValue;
    }

    private int getRepairCost(GeyserItemStack itemStack) {
        return getTagIntValueOr(itemStack, "RepairCost", 0);
    }

    private boolean hasDurability(GeyserSession session, GeyserItemStack itemStack) {
        if (itemStack.getMapping(session).getMaxDamage() > 0) {
            return getTagIntValueOr(itemStack, "Unbreakable", 0) == 0;
        }
        return false;
    }

    private int getDamage(GeyserItemStack itemStack) {
        return getTagIntValueOr(itemStack, "Damage", 0);
    }

    private String getCustomName(GeyserSession session, GeyserItemStack itemStack) {
        if (itemStack.getNbt() != null) {
            Tag tag = itemStack.getNbt().get("display");
            if (tag instanceof CompoundTag displayTag) {
                if (displayTag.get("Name") instanceof StringTag nameTag) {
                    String name = MessageTranslator.convertToPlainText(nameTag.getValue(), session.getLocale());
                    if (!name.isBlank()) {
                        return name;
                    }
                }
            }
        }
        return null;
    }
}
