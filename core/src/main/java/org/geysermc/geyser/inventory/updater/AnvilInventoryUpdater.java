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

package org.geysermc.geyser.inventory.updater;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundRenameItemPacket;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.AnvilContainer;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.inventory.item.Enchantment.JavaEnchantment;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.EnchantmentData;
import org.geysermc.geyser.util.ItemUtils;

import java.util.Objects;
import java.util.Set;

public class AnvilInventoryUpdater extends InventoryUpdater {
    public static final AnvilInventoryUpdater INSTANCE = new AnvilInventoryUpdater();

    private static final int MAX_LEVEL_COST = 40;

    @Override
    public void updateInventory(InventoryTranslator translator, GeyserSession session, Inventory inventory) {
        super.updateInventory(translator, session, inventory);
        AnvilContainer anvilContainer = (AnvilContainer) inventory;
        updateInventoryState(session, anvilContainer);
        int targetSlot = getTargetSlot(session, anvilContainer);
        for (int i = 0; i < translator.size; i++) {
            final int bedrockSlot = translator.javaSlotToBedrock(i);
            if (bedrockSlot == 50)
                continue;
            if (i == targetSlot) {
                updateTargetSlot(translator, session, anvilContainer, targetSlot);
            } else {
                InventorySlotPacket slotPacket = new InventorySlotPacket();
                slotPacket.setContainerId(ContainerId.UI);
                slotPacket.setSlot(bedrockSlot);
                slotPacket.setItem(inventory.getItem(i).getItemData(session));
                session.sendUpstreamPacket(slotPacket);
            }
        }
    }

    @Override
    public boolean updateSlot(InventoryTranslator translator, GeyserSession session, Inventory inventory, int javaSlot) {
        if (super.updateSlot(translator, session, inventory, javaSlot))
            return true;
        AnvilContainer anvilContainer = (AnvilContainer) inventory;
        updateInventoryState(session, anvilContainer);

        int lastTargetSlot = anvilContainer.getLastTargetSlot();
        int targetSlot = getTargetSlot(session, anvilContainer);
        if (targetSlot != javaSlot) {
            // Update the requested slot
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(translator.javaSlotToBedrock(javaSlot));
            slotPacket.setItem(inventory.getItem(javaSlot).getItemData(session));
            session.sendUpstreamPacket(slotPacket);
        } else if (lastTargetSlot != javaSlot) {
            // Update the previous target slot to remove repair cost changes
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(translator.javaSlotToBedrock(lastTargetSlot));
            slotPacket.setItem(inventory.getItem(lastTargetSlot).getItemData(session));
            session.sendUpstreamPacket(slotPacket);
        }

        updateTargetSlot(translator, session, anvilContainer, targetSlot);
        return true;
    }

    private void updateInventoryState(GeyserSession session, AnvilContainer anvilContainer) {
        GeyserItemStack input = anvilContainer.getInput();
        if (!input.equals(anvilContainer.getLastInput())) {
            anvilContainer.setLastInput(input.copy());
            anvilContainer.setUseJavaLevelCost(false);

            // Changing the item in the input slot resets the name field on Bedrock, but
            // does not result in a FilterTextPacket
            String originalName = MessageTranslator.convertToPlainText(ItemUtils.getCustomName(input.getNbt()), session.getLocale());
            ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(originalName);
            session.sendDownstreamPacket(renameItemPacket);

            anvilContainer.setNewName(null);
        }

        GeyserItemStack material = anvilContainer.getMaterial();
        if (!material.equals(anvilContainer.getLastMaterial())) {
            anvilContainer.setLastMaterial(material.copy());
            anvilContainer.setUseJavaLevelCost(false);
        }
    }

    /**
     * @param anvilContainer the anvil inventory
     * @return the slot to change the repair cost
     */
    private int getTargetSlot(GeyserSession session, AnvilContainer anvilContainer) {
        GeyserItemStack input = anvilContainer.getInput();
        GeyserItemStack material = anvilContainer.getMaterial();

        if (!material.isEmpty()) {
            if (!input.isEmpty() && isRepairing(session, input, material)) {
                // Changing the repair cost on the material item makes it non-stackable
                return 0;
            }
            // Prefer changing the material item because it does not reset the name field
            return 1;
        }
        return 0;
    }

    private void updateTargetSlot(InventoryTranslator translator, GeyserSession session, AnvilContainer anvilContainer, int slot) {
        ItemData itemData = anvilContainer.getItem(slot).getItemData(session);
        itemData = hijackRepairCost(session, anvilContainer, itemData);

        if (slot == 0 && isRenaming(session, anvilContainer, true)) {
            // Can't change the RepairCost because it resets the name field on Bedrock
            return;
        }

        anvilContainer.setLastTargetSlot(slot);

        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(ContainerId.UI);
        slotPacket.setSlot(translator.javaSlotToBedrock(slot));
        slotPacket.setItem(itemData);
        session.sendUpstreamPacket(slotPacket);
    }

    private ItemData hijackRepairCost(GeyserSession session, AnvilContainer anvilContainer, ItemData itemData) {
        if (itemData.isNull()) {
            return itemData;
        }
        // Fix level count by adjusting repair cost
        int newRepairCost;
        if (anvilContainer.isUseJavaLevelCost()) {
            newRepairCost = anvilContainer.getJavaLevelCost();
        } else {
            // Did not receive a ServerWindowPropertyPacket with the level cost
            newRepairCost = calcLevelCost(session, anvilContainer, false);
        }

        int bedrockLevelCost = calcLevelCost(session, anvilContainer, true);
        if (bedrockLevelCost == -1) {
            // Bedrock is unable to combine/repair the items
            return itemData;
        }

        newRepairCost -= bedrockLevelCost;
        if (newRepairCost == 0) {
            // No change to the repair cost needed
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
        GeyserItemStack input = anvilContainer.getInput();
        GeyserItemStack material = anvilContainer.getMaterial();

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
                    // No damage to repair
                    return -1;
                }
            } else {
                return -1;
            }
        }

        int totalCost = totalRepairCost + cost;
        if (isRenaming(session, anvilContainer, bedrock)) {
            totalCost++;
            if (cost == 0 && totalCost >= MAX_LEVEL_COST) {
                // Items can still be renamed when the level cost for renaming exceeds 40
                totalCost = MAX_LEVEL_COST - 1;
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
            // No damage to repair
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
        // If the material item is damaged 112% or more, then the input item will not be repaired
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
        Object2IntMap<JavaEnchantment> combinedEnchantments = getEnchantments(session, input, bedrock);
        int cost = 0;
        for (Object2IntMap.Entry<JavaEnchantment> entry : getEnchantments(session, material, bedrock).object2IntEntrySet()) {
            JavaEnchantment enchantment = entry.getKey();
            EnchantmentData data = Registries.ENCHANTMENTS.get(enchantment);
            if (data == null) {
                GeyserImpl.getInstance().getLogger().debug("Java enchantment not in registry: " + enchantment);
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
                int newLevel = entry.getIntValue();
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

    private Object2IntMap<JavaEnchantment> getEnchantments(GeyserSession session, GeyserItemStack itemStack, boolean bedrock) {
        if (itemStack.getNbt() == null) {
            return Object2IntMaps.emptyMap();
        }
        Object2IntMap<JavaEnchantment> enchantments = new Object2IntOpenHashMap<>();
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
                            GeyserImpl.getInstance().getLogger().debug("Unknown java enchantment: " + javaEnchId.getValue());
                            continue;
                        }

                        Tag javaEnchLvl = enchantTag.get("lvl");
                        if (!(javaEnchLvl instanceof ShortTag || javaEnchLvl instanceof IntTag))
                            continue;

                        // Handle duplicate enchantments
                        if (bedrock) {
                            enchantments.putIfAbsent(enchantment, ((Number) javaEnchLvl.getValue()).intValue());
                        } else {
                            enchantments.mergeInt(enchantment, ((Number) javaEnchLvl.getValue()).intValue(), Math::max);
                        }
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
        Set<String> repairMaterials = input.getMapping(session).getRepairMaterials();
        return repairMaterials != null && repairMaterials.contains(material.getMapping(session).getJavaIdentifier());
    }

    private boolean isRenaming(GeyserSession session, AnvilContainer anvilContainer, boolean bedrock) {
        if (anvilContainer.getResult().isEmpty()) {
            return false;
        }
        // This should really check the name field in all cases, but that requires the localized name
        // of the item which can change depending on NBT and Minecraft Edition
        String originalName = ItemUtils.getCustomName(anvilContainer.getInput().getNbt());
        if (bedrock && originalName != null && anvilContainer.getNewName() != null) {
            // Check text and formatting
            String legacyOriginalName = MessageTranslator.convertMessageLenient(originalName, session.getLocale());
            return !legacyOriginalName.equals(anvilContainer.getNewName());
        }
        return !Objects.equals(originalName, ItemUtils.getCustomName(anvilContainer.getResult().getNbt()));
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
}
