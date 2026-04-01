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

package org.geysermc.geyser.inventory.updater;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.AnvilContainer;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.item.BedrockEnchantment;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundRenameItemPacket;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnvilInventoryUpdater extends InventoryUpdater {
    public static final AnvilInventoryUpdater INSTANCE = new AnvilInventoryUpdater();

    private static final int MAX_LEVEL_COST = 40;

    @Override
    public void updateInventory(InventoryTranslator<?> translator, GeyserSession session, Inventory inventory) {
        super.updateInventory(translator, session, inventory);
        AnvilContainer anvilContainer = (AnvilContainer) inventory;
        updateInventoryState(session, anvilContainer);
        int targetSlot = getTargetSlot(anvilContainer, session);
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
    public boolean updateSlot(InventoryTranslator<?> translator, GeyserSession session, Inventory inventory, int javaSlot) {
        if (super.updateSlot(translator, session, inventory, javaSlot))
            return true;
        AnvilContainer anvilContainer = (AnvilContainer) inventory;
        updateInventoryState(session, anvilContainer);

        int lastTargetSlot = anvilContainer.getLastTargetSlot();
        int targetSlot = getTargetSlot(anvilContainer, session);
        if (targetSlot != javaSlot) {
            
            InventorySlotPacket slotPacket = new InventorySlotPacket();
            slotPacket.setContainerId(ContainerId.UI);
            slotPacket.setSlot(translator.javaSlotToBedrock(javaSlot));
            slotPacket.setItem(inventory.getItem(javaSlot).getItemData(session));
            session.sendUpstreamPacket(slotPacket);
        } else if (lastTargetSlot != javaSlot) {
            
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

            
            
            String originalName = MessageTranslator.convertToPlainText(input.getComponent(DataComponentTypes.CUSTOM_NAME), session.locale());
            ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(originalName);
            session.sendDownstreamGamePacket(renameItemPacket);

            anvilContainer.setNewName(null);
        }

        GeyserItemStack material = anvilContainer.getMaterial();
        if (!material.equals(anvilContainer.getLastMaterial())) {
            anvilContainer.setLastMaterial(material.copy());
            anvilContainer.setUseJavaLevelCost(false);
        }
    }

    
    private int getTargetSlot(AnvilContainer anvilContainer, GeyserSession session) {
        GeyserItemStack input = anvilContainer.getInput();
        GeyserItemStack material = anvilContainer.getMaterial();

        if (!material.isEmpty()) {
            if (!input.isEmpty() && isRepairing(input, material, session)) {
                
                return 0;
            }
            
            return 1;
        }
        return 0;
    }

    private void updateTargetSlot(InventoryTranslator<?> translator, GeyserSession session, AnvilContainer anvilContainer, int slot) {
        ItemData itemData = anvilContainer.getItem(slot).getItemData(session);
        itemData = hijackRepairCost(session, anvilContainer, itemData);

        if (slot == 0 && isRenaming(session, anvilContainer, true)) {
            
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
        
        int newRepairCost;
        if (anvilContainer.isUseJavaLevelCost()) {
            newRepairCost = anvilContainer.getJavaLevelCost();
        } else {
            
            newRepairCost = calcLevelCost(session, anvilContainer, false);
        }

        int bedrockLevelCost = calcLevelCost(session, anvilContainer, true);
        if (bedrockLevelCost == -1) {
            
            return itemData;
        }

        newRepairCost -= bedrockLevelCost;
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
            if (isCombining(input, material)) {
                if (hasDurability(input) && input.isSameItem(material)) {
                    cost += calcMergeRepairCost(input, material);
                }

                int enchantmentLevelCost = calcMergeEnchantmentCost(session, input, material, bedrock);
                if (enchantmentLevelCost != -1) {
                    cost += enchantmentLevelCost;
                } else if (cost == 0) {
                    
                    return -1;
                }
            } else if (hasDurability(input) && isRepairing(input, material, session)) {
                cost = calcRepairLevelCost(input, material);
                if (cost == -1) {
                    
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
                
                totalCost = MAX_LEVEL_COST - 1;
            }
        }
        return totalCost;
    }

    
    private int calcRepairLevelCost(GeyserItemStack input, GeyserItemStack material) {
        int newDamage = getDamage(input);
        int unitRepair = Math.min(newDamage, input.asItem().defaultMaxDamage() / 4);
        if (unitRepair <= 0) {
            
            return -1;
        }
        for (int i = 0; i < material.getAmount(); i++) {
            newDamage -= unitRepair;
            unitRepair = Math.min(newDamage, input.asItem().defaultMaxDamage() / 4);
            if (unitRepair <= 0) {
                return i + 1;
            }
        }
        return material.getAmount();
    }

    
    private int calcMergeRepairCost(GeyserItemStack input, GeyserItemStack material) {
        
        if (getDamage(input) > 0 && getDamage(material) < (material.asItem().defaultMaxDamage() * 112 / 100)) {
            return 2;
        }
        return 0;
    }

    
    private int calcMergeEnchantmentCost(GeyserSession session, GeyserItemStack input, GeyserItemStack material, boolean bedrock) {
        boolean hasCompatible = false;
        Object2IntMap<Enchantment> combinedEnchantments = getEnchantments(session, input);
        int cost = 0;
        for (Object2IntMap.Entry<Enchantment> entry : getEnchantments(session, material).object2IntEntrySet()) {
            Enchantment enchantment = entry.getKey();

            boolean canApply = isEnchantedBook(input) || enchantment.supportedItems().contains(session, input.asItem());

            List<Enchantment> incompatibleEnchantments = enchantment.exclusiveSet().resolve(session);
            for (Enchantment incompatible : incompatibleEnchantments) {
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
                if (newLevel > enchantment.maxLevel()) {
                    newLevel = enchantment.maxLevel();
                }
                combinedEnchantments.put(enchantment, newLevel);

                int rarityMultiplier = enchantment.anvilCost();
                if (isEnchantedBook(material) && rarityMultiplier > 1) {
                    rarityMultiplier /= 2;
                }
                if (bedrock) {
                    if (newLevel > currentLevel) {
                        hasCompatible = true;
                    }
                    if (enchantment.bedrockEnchantment() == BedrockEnchantment.IMPALING) {
                        
                        rarityMultiplier /= 2;
                    } else if (enchantment.bedrockEnchantment() == null) {
                        
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

    private Object2IntMap<Enchantment> getEnchantments(GeyserSession session, GeyserItemStack itemStack) {
        ItemEnchantments enchantmentComponent;
        if (isEnchantedBook(itemStack)) {
            enchantmentComponent = itemStack.getComponent(DataComponentTypes.STORED_ENCHANTMENTS);
        } else {
            enchantmentComponent = itemStack.getComponent(DataComponentTypes.ENCHANTMENTS);
        }
        if (enchantmentComponent != null) {
            Object2IntMap<Enchantment> enchantments = new Object2IntOpenHashMap<>();
            for (Map.Entry<Integer, Integer> entry : enchantmentComponent.getEnchantments().entrySet()) {
                Enchantment enchantment = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).byId(entry.getKey());
                if (enchantment == null) {
                    GeyserImpl.getInstance().getLogger().debug("Unknown Java enchantment in anvil: " + entry.getKey());
                    continue;
                }
                enchantments.put(enchantment, entry.getValue().intValue());
            }
            return enchantments;
        }
        return new Object2IntOpenHashMap<>();
    }

    private boolean isEnchantedBook(GeyserItemStack itemStack) {
        return itemStack.is(Items.ENCHANTED_BOOK);
    }

    private boolean isCombining(GeyserItemStack input, GeyserItemStack material) {
        return isEnchantedBook(material) || (input.isSameItem(material) && hasDurability(input));
    }

    private boolean isRepairing(GeyserItemStack input, GeyserItemStack material, GeyserSession session) {
        HolderSet repairable = input.getComponent(DataComponentTypes.REPAIRABLE);
        if (repairable == null) {
            return false;
        }

        return material.is(session, repairable);
    }

    private boolean isRenaming(GeyserSession session, AnvilContainer anvilContainer, boolean bedrock) {
        if (anvilContainer.getResult().isEmpty()) {
            return false;
        }
        
        
        Component originalName = anvilContainer.getInput().getComponent(DataComponentTypes.CUSTOM_NAME);
        if (bedrock && originalName != null && anvilContainer.getNewName() != null) {
            
            String legacyOriginalName = MessageTranslator.convertMessage(originalName, session.locale());
            return !legacyOriginalName.equals(anvilContainer.getNewName());
        }
        return !Objects.equals(originalName, anvilContainer.getResult().getComponent(DataComponentTypes.CUSTOM_NAME));
    }

    private int getRepairCost(GeyserItemStack itemStack) {
        return itemStack.getComponentElseGet(DataComponentTypes.REPAIR_COST, () -> 0);
    }

    private boolean hasDurability(GeyserItemStack itemStack) {
        if (itemStack.asItem().defaultMaxDamage() > 0) {
            return itemStack.getComponent(DataComponentTypes.UNBREAKABLE) != null;
        }
        return false;
    }

    private int getDamage(GeyserItemStack itemStack) {
        return itemStack.getComponentElseGet(DataComponentTypes.DAMAGE, () -> 0);
    }
}
