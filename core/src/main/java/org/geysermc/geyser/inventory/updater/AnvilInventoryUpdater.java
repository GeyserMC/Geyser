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
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
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
        if (super.updateSlot(translator, session, inventory, javaSlot)) {
            return true;
        }
        AnvilContainer anvilContainer = (AnvilContainer) inventory;
        updateInventoryState(session, anvilContainer);

        int lastTargetSlot = anvilContainer.getLastTargetSlot();
        int targetSlot = getTargetSlot(anvilContainer, session);
        System.out.println("updating java slot " + javaSlot + ", last target slot " + lastTargetSlot + " current target slot " + targetSlot);
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

    /**
     * @param anvilContainer the anvil inventory
     * @return the slot to change the repair cost
     */
    private int getTargetSlot(AnvilContainer anvilContainer, GeyserSession session) {
        GeyserItemStack input = anvilContainer.getInput();
        GeyserItemStack material = anvilContainer.getMaterial();

        if (!material.isEmpty()) {
            if (!input.isEmpty() && isRepairItem(input, material, session)) {
                // Changing the repair cost on the material item makes it non-stackable
                return 0;
            }
            // Prefer changing the material item because it does not reset the name field
            return 1;
        }
        return 0;
    }

    private void updateTargetSlot(InventoryTranslator<?> translator, GeyserSession session, AnvilContainer anvilContainer, int slot) {
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
        if (itemData.getTag() != null) {
            System.out.println("existing repair cost " + itemData.getTag().getInt("RepairCost", 0));
        }

        // Fix level count by adjusting repair cost
        int newRepairCost;
        if (anvilContainer.isUseJavaLevelCost()) {
            newRepairCost = anvilContainer.getJavaLevelCost();
            System.out.println("using java repair cost in hijack " + newRepairCost);
        } else {
            // Did not receive a ServerWindowPropertyPacket with the level cost
            newRepairCost = calcLevelCost(session, anvilContainer, false);
            System.out.println("calculated own java cost in hijack " + newRepairCost);
        }

        int bedrockLevelCost = calcLevelCost(session, anvilContainer, true);
        System.out.println("calculated bedrock level cost of " + bedrockLevelCost);
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

    // See Java's AnvilMenu#createResult for reference. Bedrock differs only slightly when calculating the cost required for combining enchantments.
    // Note that that method has all calculations in one single method, while this method uses multiple smaller methods.
    // Also see https://gist.github.com/eclipseisoffline/fd6e46f3b1d7202c038663ff2e7ad97c for the same method but with local variables properly named and some comments added
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

        // Requirements to repair or enchant an item
        if (input.isEmpty() || !canUseInput(input)) {
            return 0;
        }

        int modifyCost = 0;
        int baseRepairCost = getRepairCost(input);
        int nameCost = 0;

        if (!material.isEmpty()) {
            // Adding material

            baseRepairCost += getRepairCost(material);

            boolean materialHasStoredEnchantments = material.hasComponent(DataComponentTypes.STORED_ENCHANTMENTS);
            if (input.isDamageable() && isRepairItem(input, material, session)) {
                // Input is damageable and item is repair item (repairing by repairable component, not by combining 2 of the same items)
                modifyCost += calcRepairLevelCost(input, material);
                if (modifyCost == -1) {
                    // No damage to repair
                    return -1;
                }
            } else {
                if (!materialHasStoredEnchantments && (!input.isSameItem(material) || !input.isDamageable())) {
                    // Material has no stored enchantments (not enchanted book) and input is not the same as material or is not damageable (can't repair/combine)
                    return -1;
                }

                if (input.isDamageable() && !materialHasStoredEnchantments) {
                    // Input is damageable and material has no stored enchantments
                    // Attempt merge repair - can add 2 to the cost if successful
                    modifyCost += calcMergeRepairCost(input, material);
                }

                int enchantingCost = calcMergeEnchantmentCost(session, input, material, bedrock);
                if (enchantingCost != -1) {
                    modifyCost += enchantingCost;
                } else {
                    // Merging enchantments failed - incompatible
                    return -1;
                }
            }
        }

        if (isRenaming(session, anvilContainer, bedrock)) {
            nameCost = 1;
            modifyCost += nameCost;
        }

        if (modifyCost <= 0) {
            return -1;
        }
        int finalCost = baseRepairCost + modifyCost;

        // If modifyCost is only the nameCost, so no enchantments have been added or repairments have been made, allow only renaming at the maximum level
        if (nameCost == modifyCost) {
            if (finalCost >= MAX_LEVEL_COST) {
                finalCost = MAX_LEVEL_COST - 1;
            }
        }

        return finalCost;
    }

    /**
     * Calculate the levels needed to repair an item with its repair material
     * E.g. iron_sword + iron_ingot
     *
     * @param input    an item with durability
     * @param material the item's respective repair material
     * @return the number of levels needed or 0 if it is not possible to repair any further // TODO
     */
    private int calcRepairLevelCost(GeyserItemStack input, GeyserItemStack material) {
        int newDamage = input.getDamage();
        int repairAmount = Math.min(newDamage, input.getMaxDamage() / 4);
        if (repairAmount <= 0) {
            // No damage to repair
            return -1;
        }

        // Calculate cost based on how much of the material is being used to repair
        int repairCost = 0;
        for (int itemsUsed = 0; repairAmount > 0 && itemsUsed < material.getAmount(); itemsUsed++) { // Run as long as the repair amount if above 0 and there are items left to use
            newDamage -= repairAmount;
            repairCost++;
            repairAmount = Math.min(newDamage, input.getMaxDamage() / 4);
        }
        return repairCost;
    }

    /**
     * Calculate the levels cost for repairing items by combining two of the same item
     *
     * @param input    an item with durability
     * @param material a matching item
     * @return the number of levels needed or 0 if it is not possible to repair any further
     */
    private int calcMergeRepairCost(GeyserItemStack input, GeyserItemStack material) {
        // If the material item is damaged 112% or more, then the input item will not be repaired
        // TODO check
        if (getDamage(input) > 0 && getDamage(material) < (material.asItem().defaultMaxDamage() * 112 / 100)) {
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
        Object2IntMap<Enchantment> inputEnchantments = getEnchantments(session, input);
        Object2IntMap<Enchantment> materialEnchantments = getEnchantments(session, material);

        boolean enchantingSucceeded = false;
        boolean enchantingFailed = false;

        boolean materialHasStoredEnchantments = getEnchantmentComponent(material) == DataComponentTypes.STORED_ENCHANTMENTS;

        int enchantCost = 0;
        for (Object2IntMap.Entry<Enchantment> entry : materialEnchantments.object2IntEntrySet()) {
            Enchantment materialEnchantment = entry.getKey();

            int currentLevel = inputEnchantments.getOrDefault(materialEnchantment, 0);
            int newLevel = entry.getIntValue();

            // If both levels are same, add 1 to new level, else take the highest level
            newLevel = currentLevel == newLevel ? newLevel + 1 : Math.max(newLevel, currentLevel);

            boolean canApply = isEnchantedBook(input) || session.getTagCache().is(materialEnchantment.supportedItems(), input.asItem())
                || (!bedrock && session.isInstabuild());

            for (Enchantment existingEnchantment : inputEnchantments.keySet()) {
                // Check for conflicting enchantments
                if (!existingEnchantment.equals(materialEnchantment) && !isCompatible(session, materialEnchantment, existingEnchantment)) {
                    canApply = false;
                    if (!bedrock) {
                        enchantCost++;
                    }
                }
            }

            if (!canApply) {
                // At least one enchantment failed
                enchantingFailed = true;
            } else {
                // Cap level
                if (newLevel > materialEnchantment.maxLevel()) {
                    newLevel = materialEnchantment.maxLevel();
                }
                // and set
                inputEnchantments.put(materialEnchantment, newLevel);

                int anvilCost = materialEnchantment.anvilCost();
                if (materialHasStoredEnchantments) {
                    // If enchanted book then half cost with a min of 1
                    anvilCost = Math.max(1, anvilCost / 2);
                }

                if (bedrock) {
                    if (newLevel > currentLevel) {
                        // At least one enchantment succeeded
                        enchantingSucceeded = true;
                    } else {
                        // At least one enchantment failed
                        enchantingFailed = true;
                    }

                    if (materialEnchantment.bedrockEnchantment() == BedrockEnchantment.IMPALING) {
                        // Multiplier is halved on Bedrock for some reason
                        anvilCost /= 2;
                    } else if (materialEnchantment.bedrockEnchantment() == null) {
                        // Whatever this is, doesn't exist on Bedrock
                        anvilCost = 0;
                    }
                    enchantCost += anvilCost * (newLevel - currentLevel);
                } else {
                    // At least one enchantment succeeded
                    enchantingSucceeded = true;
                    enchantCost += anvilCost * newLevel;

                    // TODO check
                    // If enchanting an item stack with more than 1 item, immediately cap at 40, making it impossible to actually enchant unless in creative mode
                    if (input.getAmount() > 1) {
                        enchantCost = 40;
                    }
                }
            }
        }

        // If at least one enchantment failed and none succeeded, fail
        if (enchantingFailed && !enchantingSucceeded) {
            return -1;
        }

        return enchantCost;
    }

    private Object2IntMap<Enchantment> getEnchantments(GeyserSession session, GeyserItemStack itemStack) {
        ItemEnchantments enchantmentComponent = getHeldEnchantments(itemStack);
        Object2IntMap<Enchantment> enchantments = new Object2IntOpenHashMap<>();

        if (enchantmentComponent != null) {
            for (Map.Entry<Integer, Integer> entry : enchantmentComponent.getEnchantments().entrySet()) {
                Enchantment enchantment = JavaRegistries.ENCHANTMENT.fromNetworkId(session, entry.getKey());
                if (enchantment == null) {
                    GeyserImpl.getInstance().getLogger().debug("Unknown Java enchantment in anvil: " + entry.getKey());
                    continue;
                }
                enchantments.put(enchantment, entry.getValue().intValue());
            }
        }

        return enchantments;
    }

    private boolean hasEnchantments(GeyserItemStack stack) {
        ItemEnchantments enchantments = stack.getComponent(DataComponentTypes.ENCHANTMENTS);
        return enchantments != null && !enchantments.getEnchantments().isEmpty();
    }

    private boolean isEnchantedBook(GeyserItemStack itemStack) {
        return itemStack.asItem() == Items.ENCHANTED_BOOK;
    }

    private boolean isCombining(GeyserItemStack input, GeyserItemStack material) {
        return isEnchantedBook(material)
            || (input.getJavaId() == material.getJavaId() && (hasDurability(input) || (hasEnchantments(input) && hasEnchantments(material))));
    }

    private boolean isRepairItem(GeyserItemStack input, GeyserItemStack material, GeyserSession session) {
        HolderSet repairable = input.getComponent(DataComponentTypes.REPAIRABLE);
        if (repairable == null) {
            return false;
        }

        return session.getTagCache().isItem(repairable, material.asItem());
    }

    private boolean isRenaming(GeyserSession session, AnvilContainer anvilContainer, boolean bedrock) {
        if (anvilContainer.getResult().isEmpty()) {
            //System.out.println("not renaming because result is empty");
            return false;
        }

        Component originalName = anvilContainer.getInput().getName();
        //System.out.println("original name is " + originalName);
        //System.out.println("new bedrock name " + anvilContainer.getNewName());
        if (bedrock && anvilContainer.getNewName() != null) {
            // Check text and formatting
            String bedrockOriginalName = MessageTranslator.convertMessage(originalName, session.locale());
//            System.out.println("original bedrock name " + bedrockOriginalName + " , " + (bedrockOriginalName.equals(anvilContainer.getNewName()) ? "equal" : "not equal"));
            return !bedrockOriginalName.equals(anvilContainer.getNewName());
        }

//        System.out.println("new name is " + anvilContainer.getResult().getName() + " , " + (originalName.equals(anvilContainer.getResult().getName()) ? "equal" : "not equal"));
        return !originalName.equals(anvilContainer.getResult().getName());
    }

    private int getRepairCost(GeyserItemStack itemStack) {
        return itemStack.getComponentElseGet(DataComponentTypes.REPAIR_COST, () -> 0);
    }

    private boolean hasDurability(GeyserItemStack itemStack) {
        if (itemStack.getMaxDamage() > 0) {
            return !itemStack.hasComponent(DataComponentTypes.UNBREAKABLE);
        }
        return false;
    }

    private int getDamage(GeyserItemStack itemStack) {
        return itemStack.getComponentElseGet(DataComponentTypes.DAMAGE, () -> 0);
    }

    private static boolean isCompatible(GeyserSession session, Enchantment first, Enchantment second) {
        return !first.equals(second) && !first.exclusiveSet().contains(session, second) && !second.exclusiveSet().contains(session, first);
    }

    private static boolean canUseInput(GeyserItemStack input) {
        return input.hasComponent(getEnchantmentComponent(input));
    }

    private static ItemEnchantments getHeldEnchantments(GeyserItemStack stack) {
        return stack.getComponent(getEnchantmentComponent(stack));
    }

    private static DataComponentType<ItemEnchantments> getEnchantmentComponent(GeyserItemStack stack) {
        return stack.getJavaId() == Items.ENCHANTED_BOOK.javaId() ? DataComponentTypes.STORED_ENCHANTMENTS : DataComponentTypes.ENCHANTMENTS;
    }
}
