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

package org.geysermc.connector.network.translators.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.protocol.bedrock.data.ItemData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.ItemStackTranslator;
import org.geysermc.connector.network.translators.NbtItemStackTranslator;
import org.geysermc.connector.utils.MessageUtils;
import org.geysermc.connector.utils.Toolbox;
import org.reflections.Reflections;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemTranslator {

    private Int2ObjectMap<ItemStackTranslator> itemTranslators = new Int2ObjectOpenHashMap();
    private List<NbtItemStackTranslator> nbtItemTranslators;
    private Map<String, ItemEntry> javaIdentifierMap = new HashMap<>();

    // Shield ID, used in Entity.java
    public static final int SHIELD = 829;

    public void init() {
        Reflections ref = new Reflections("org.geysermc.connector.network.translators.item");

        Map<NbtItemStackTranslator, Integer> loadedNbtItemTranslators = new HashMap<>();
        for (Class<?> clazz : ref.getTypesAnnotatedWith(ItemRemapper.class)) {
            int priority = clazz.getAnnotation(ItemRemapper.class).priority();

            GeyserConnector.getInstance().getLogger().debug("Found annotated item translator: " + clazz.getCanonicalName());

            try {
                if (NbtItemStackTranslator.class.isAssignableFrom(clazz)) {
                    NbtItemStackTranslator nbtItemTranslator = (NbtItemStackTranslator) clazz.newInstance();
                    loadedNbtItemTranslators.put(nbtItemTranslator, priority);
                    continue;
                }
                ItemStackTranslator itemStackTranslator = (ItemStackTranslator) clazz.newInstance();
                List<ItemEntry> appliedItems = itemStackTranslator.getAppliedItems();
                for (ItemEntry item : appliedItems) {
                    ItemStackTranslator registered = itemTranslators.get(item.getJavaId());
                    if (registered != null) {
                        GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated item translator " + clazz.getCanonicalName() + "." +
                                " Item translator " + registered.getClass().getCanonicalName() + " is already registered for the item " + item.getJavaIdentifier());
                        continue;
                    }
                    itemTranslators.put(item.getJavaId(), itemStackTranslator);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated item translator " + clazz.getCanonicalName() + ".");
            }
        }

        nbtItemTranslators = loadedNbtItemTranslators.keySet().stream()
                .sorted(Comparator.comparingInt(value -> loadedNbtItemTranslators.get(value))).collect(Collectors.toList());
    }

    public ItemStack translateToJava(GeyserSession session, ItemData data) {
        if (data == null) {
            return new ItemStack(0);
        }
        ItemEntry javaItem = getItem(data);

        ItemStack itemStack;
        ItemStackTranslator itemStackTranslator = itemTranslators.get(javaItem.getJavaId());
        if (itemStackTranslator != null) {
            itemStack = itemStackTranslator.translateToJava(data, javaItem);
        } else {
            itemStack = DEFAULT_TRANSLATOR.translateToJava(data, javaItem);
        }

        if (itemStack != null && itemStack.getNbt() != null) {
            for (NbtItemStackTranslator translator : nbtItemTranslators) {
                if (translator.acceptItem(javaItem)) {
                    translator.translateToJava(itemStack.getNbt(), javaItem);
                }
            }
        }

        return itemStack;
    }

    public ItemData translateToBedrock(GeyserSession session, ItemStack stack) {
        if (stack == null) {
            return ItemData.AIR;
        }

        ItemEntry bedrockItem = getItem(stack);

        ItemStack itemStack = new ItemStack(stack.getId(), stack.getAmount(), stack.getNbt() != null ? stack.getNbt().clone() : null);

        if (itemStack.getNbt() != null) {
            for (NbtItemStackTranslator translator : nbtItemTranslators) {
                if (translator.acceptItem(bedrockItem)) {
                    translator.translateToBedrock(itemStack.getNbt(), bedrockItem);
                }
            }
        }

        ItemData itemData;
        ItemStackTranslator itemStackTranslator = itemTranslators.get(bedrockItem.getJavaId());
        if (itemStackTranslator != null) {
            itemData = itemStackTranslator.translateToBedrock(itemStack, bedrockItem);
        } else {
            itemData = DEFAULT_TRANSLATOR.translateToBedrock(itemStack, bedrockItem);
        }

        try {
            // Get the display name of the item
            String name = itemData.getTag().getCompound("display").getString("Name");

            // Check if its a message to translate
            if (MessageUtils.isMessage(name)) {
                // Get the translated name
                name = MessageUtils.getTranslatedBedrockMessage(Message.fromString(name), session.getClientData().getLanguageCode());

                // Build the new display tag
                CompoundTagBuilder displayBuilder = itemData.getTag().getCompound("display").toBuilder();
                displayBuilder.stringTag("Name", name);

                // Build the new root tag
                CompoundTagBuilder builder = itemData.getTag().toBuilder();
                builder.tag(displayBuilder.build("display"));

                // Create a new item with the original data + updated name
                itemData = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount(), builder.buildRootTag());
            }
        } catch (NullPointerException e) { } // In case the NBT tag of the item is missing

        return itemData;
    }

    public ItemEntry getItem(ItemStack stack) {
        return Toolbox.ITEM_ENTRIES.get(stack.getId());
    }

    public ItemEntry getItem(ItemData data) {
        for (ItemEntry itemEntry : Toolbox.ITEM_ENTRIES.values()) {
            if (itemEntry.getBedrockId() == data.getId() && (itemEntry.getBedrockData() == data.getDamage() || itemEntry.getJavaIdentifier().endsWith("potion"))) {
                return itemEntry;
            }
        }
        // If item find was unsuccessful first time, we try again while ignoring damage
        // Fixes piston, sticky pistons, dispensers and droppers turning into air from creative inventory
        for (ItemEntry itemEntry : Toolbox.ITEM_ENTRIES.values()) {
            if (itemEntry.getBedrockId() == data.getId()) {
                return itemEntry;
            }
        }

        GeyserConnector.getInstance().getLogger().debug("Missing mapping for bedrock item " + data.getId() + ":" + data.getDamage());
        return ItemEntry.AIR;
    }

    public ItemEntry getItemEntry(String javaIdentifier) {
        return javaIdentifierMap.computeIfAbsent(javaIdentifier, key -> Toolbox.ITEM_ENTRIES.values()
                .stream().filter(itemEntry -> itemEntry.getJavaIdentifier().equals(key)).findFirst().orElse(null));
    }

    private static final ItemStackTranslator DEFAULT_TRANSLATOR = new ItemStackTranslator() {
        @Override
        public List<ItemEntry> getAppliedItems() {
            return null;
        }
    };
}
