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

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientClickWindowButtonPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.geysermc.connector.common.ChatColor;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;
import org.geysermc.connector.utils.LocaleUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A temporary reconstruction of the enchantment table UI until our inventory rewrite is complete.
 * The enchantment table on Bedrock without server authoritative inventories doesn't tell us which button is pressed
 * when selecting an enchantment.
 */
public class EnchantmentInventoryTranslator extends BlockInventoryTranslator {

    private static final int DYE_ID = ItemRegistry.getItemEntry("minecraft:lapis_lazuli").getBedrockId();
    private static final int ENCHANTED_BOOK_ID = ItemRegistry.getItemEntry("minecraft:enchanted_book").getBedrockId();

    public EnchantmentInventoryTranslator(InventoryUpdater updater) {
        super(2, "minecraft:hopper[enabled=false,facing=down]", ContainerType.HOPPER, updater);
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        for (InventoryActionData action : actions) {
            if (action.getSource().getContainerId() == inventory.getId()) {
                // This is the hopper UI
                switch (action.getSlot()) {
                    case 1:
                        // Don't allow the slot to be put through if the item isn't lapis
                        if ((action.getToItem().getId() != DYE_ID) && action.getToItem() != ItemData.AIR) {
                            updateInventory(session, inventory);
                            InventoryUtils.updateCursor(session);
                            return;
                        }
                        break;
                    case 2:
                    case 3:
                    case 4:
                        // The books here act as buttons
                        ClientClickWindowButtonPacket packet = new ClientClickWindowButtonPacket(inventory.getId(), action.getSlot() - 2);
                        session.sendDownstreamPacket(packet);
                        updateInventory(session, inventory);
                        InventoryUtils.updateCursor(session);
                        return;
                    default:
                        break;
                }
            }
        }

        super.translateActions(session, inventory, actions);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        super.updateInventory(session, inventory);
        ItemData[] items = new ItemData[5];
        items[0] = ItemTranslator.translateToBedrock(session, inventory.getItem(0));
        items[1] = ItemTranslator.translateToBedrock(session, inventory.getItem(1));
        for (int i = 0; i < 3; i++) {
            items[i + 2] = session.getEnchantmentSlotData()[i].getItem() != null ? session.getEnchantmentSlotData()[i].getItem() : createEnchantmentBook();
        }

        InventoryContentPacket contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(inventory.getId());
        contentPacket.setContents(items);
        session.sendUpstreamPacket(contentPacket);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        int bookSlotToUpdate;
        switch (key) {
            case 0:
            case 1:
            case 2:
                // Experience required
                bookSlotToUpdate = key;
                session.getEnchantmentSlotData()[bookSlotToUpdate].setExperienceRequired(value);
                break;
            case 4:
            case 5:
            case 6:
                // Enchantment name
                bookSlotToUpdate = key - 4;
                if (value != -1) {
                    session.getEnchantmentSlotData()[bookSlotToUpdate].setEnchantmentType(EnchantmentTableEnchantments.values()[value - 1]);
                } else {
                    // -1 means no enchantment specified
                    session.getEnchantmentSlotData()[bookSlotToUpdate].setEnchantmentType(null);
                }
                break;
            case 7:
            case 8:
            case 9:
                // Enchantment level
                bookSlotToUpdate = key - 7;
                session.getEnchantmentSlotData()[bookSlotToUpdate].setEnchantmentLevel(value);
                break;
            default:
                return;
        }
        updateEnchantmentBook(session, inventory, bookSlotToUpdate);
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        super.openInventory(session, inventory);
        for (int i = 0; i < session.getEnchantmentSlotData().length; i++) {
            session.getEnchantmentSlotData()[i] = new EnchantmentSlotData();
        }
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        super.closeInventory(session, inventory);
        Arrays.fill(session.getEnchantmentSlotData(), null);
    }

    private ItemData createEnchantmentBook() {
        NbtMapBuilder root = NbtMap.builder();
        NbtMapBuilder display = NbtMap.builder();

        display.putString("Name", ChatColor.RESET + "No Enchantment");

        root.put("display", display.build());
        return ItemData.of(ENCHANTED_BOOK_ID, (short) 0, 1, root.build());
    }

    private void updateEnchantmentBook(GeyserSession session, Inventory inventory, int slot) {
        NbtMapBuilder root = NbtMap.builder();
        NbtMapBuilder display = NbtMap.builder();
        EnchantmentSlotData data = session.getEnchantmentSlotData()[slot];
        if (data.getEnchantmentType() != null) {
            display.putString("Name", ChatColor.ITALIC + data.getEnchantmentType().toString(session) +
                    (data.getEnchantmentLevel() != -1 ? " " + toRomanNumeral(session, data.getEnchantmentLevel()) : "") + "?");
        } else {
            display.putString("Name", ChatColor.RESET + "No Enchantment");
        }

        display.putList("Lore", NbtType.STRING, Collections.singletonList(ChatColor.DARK_GRAY + data.getExperienceRequired() + "xp"));
        root.put("display", display.build());
        ItemData book = ItemData.of(ENCHANTED_BOOK_ID, (short) 0, 1, root.build());

        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(inventory.getId());
        slotPacket.setSlot(slot + 2);
        slotPacket.setItem(book);
        session.sendUpstreamPacket(slotPacket);
        data.setItem(book);
    }

    private String toRomanNumeral(GeyserSession session, int level) {
        return LocaleUtils.getLocaleString("enchantment.level." + level,
                session.getLocale());
    }

    /**
     * Stores the data of each slot in an enchantment table
     */
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class EnchantmentSlotData {
        private EnchantmentTableEnchantments enchantmentType = null;
        private int enchantmentLevel = 0;
        private int experienceRequired = 0;
        private ItemData item;
    }

    /**
     * Classifies enchantments by Java order
     */
    public enum EnchantmentTableEnchantments {
        PROTECTION,
        FIRE_PROTECTION,
        FEATHER_FALLING,
        BLAST_PROTECTION,
        PROJECTILE_PROTECTION,
        RESPIRATION,
        AQUA_AFFINITY,
        THORNS,
        DEPTH_STRIDER,
        FROST_WALKER,
        BINDING_CURSE,
        SHARPNESS,
        SMITE,
        BANE_OF_ARTHROPODS,
        KNOCKBACK,
        FIRE_ASPECT,
        LOOTING,
        SWEEPING,
        EFFICIENCY,
        SILK_TOUCH,
        UNBREAKING,
        FORTUNE,
        POWER,
        PUNCH,
        FLAME,
        INFINITY,
        LUCK_OF_THE_SEA,
        LURE,
        LOYALTY,
        IMPALING,
        RIPTIDE,
        CHANNELING,
        MENDING,
        VANISHING_CURSE, // After this is not documented
        MULTISHOT,
        PIERCING,
        QUICK_CHARGE,
        SOUL_SPEED;

        public String toString(GeyserSession session) {
            return LocaleUtils.getLocaleString("enchantment.minecraft." + this.toString().toLowerCase(),
                    session.getLocale());
        }
    }
}
