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

package org.geysermc.connector.network.translators.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.message.MessageSerializer;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.events.registry.ItemRemapperRegistryEvent;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.MessageUtils;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ItemTranslator {

    public static final Int2ObjectMap<ItemTranslator> ITEM_STACK_TRANSLATORS = new Int2ObjectOpenHashMap<>();
    public static final List<NbtItemStackTranslator> NBT_TRANSLATORS = new ArrayList<>();

    protected ItemTranslator() {
    }

    public static void init() {
        // no-op
    }

    static {
        /* Load item translators */
        ItemRemapperRegistryEvent itemRemapperEvent = EventManager.getInstance().triggerEvent(new ItemRemapperRegistryEvent(
                new Reflections("org.geysermc.connector.network.translators.item").getTypesAnnotatedWith(ItemRemapper.class))
        ).getEvent();
        
        Map<NbtItemStackTranslator, Integer> loadedNbtItemTranslators = new HashMap<>();
        for (Class<?> clazz : itemRemapperEvent.getRegisteredTranslators()) {
            int priority = clazz.getAnnotation(ItemRemapper.class).priority();

            GeyserConnector.getInstance().getLogger().debug("Found annotated item translator: " + clazz.getCanonicalName());

            try {
                if (NbtItemStackTranslator.class.isAssignableFrom(clazz)) {
                    NbtItemStackTranslator nbtItemTranslator = (NbtItemStackTranslator) clazz.newInstance();
                    loadedNbtItemTranslators.put(nbtItemTranslator, priority);
                    continue;
                }
                ItemTranslator itemStackTranslator = (ItemTranslator) clazz.newInstance();
                List<ItemEntry> appliedItems = itemStackTranslator.getAppliedItems();
                for (ItemEntry item : appliedItems) {
                    ItemTranslator registered = ITEM_STACK_TRANSLATORS.get(item.getJavaId());
                    if (registered != null) {
                        GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.network.translator.item.already_registered", clazz.getCanonicalName(), registered.getClass().getCanonicalName(), item.getJavaIdentifier()));
                        continue;
                    }
                    ITEM_STACK_TRANSLATORS.put(item.getJavaId(), itemStackTranslator);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.network.translator.item.failed", clazz.getCanonicalName()));
            }
        }

        NBT_TRANSLATORS.addAll(loadedNbtItemTranslators.keySet().stream().sorted(Comparator.comparingInt(loadedNbtItemTranslators::get)).collect(Collectors.toList()));
    }

    public static ItemStack translateToJava(ItemData data) {
        if (data == null) {
            return new ItemStack(0);
        }
        ItemEntry javaItem = ItemRegistry.getItem(data);

        ItemStack itemStack;
        ItemTranslator itemStackTranslator = ITEM_STACK_TRANSLATORS.get(javaItem.getJavaId());
        if (itemStackTranslator != null) {
            itemStack = itemStackTranslator.translateToJava(data, javaItem);
        } else {
            itemStack = DEFAULT_TRANSLATOR.translateToJava(data, javaItem);
        }

        if (itemStack != null && itemStack.getNbt() != null) {
            for (NbtItemStackTranslator translator : NBT_TRANSLATORS) {
                if (translator.acceptItem(javaItem)) {
                    translator.translateToJava(itemStack.getNbt(), javaItem);
                }
            }
        }
        return itemStack;
    }

    public static ItemData translateToBedrock(GeyserSession session, ItemStack stack) {
        if (stack == null) {
            return ItemData.AIR;
        }

        ItemEntry bedrockItem = ItemRegistry.getItem(stack);

        com.github.steveice10.opennbt.tag.builtin.CompoundTag nbt = stack.getNbt() != null ? stack.getNbt().clone() : null;

        // This is a fallback for maps with no nbt
        if (nbt == null && bedrockItem.getJavaIdentifier().equals("minecraft:filled_map")) {
            nbt = new com.github.steveice10.opennbt.tag.builtin.CompoundTag("");
            nbt.put(new IntTag("map", 0));
        }

        ItemStack itemStack = new ItemStack(stack.getId(), stack.getAmount(), nbt);

        if (nbt != null) {
            for (NbtItemStackTranslator translator : NBT_TRANSLATORS) {
                if (translator.acceptItem(bedrockItem)) {
                    translator.translateToBedrock(nbt, bedrockItem);
                }
            }
        }

        ItemData itemData;
        ItemTranslator itemStackTranslator = ITEM_STACK_TRANSLATORS.get(bedrockItem.getJavaId());
        if (itemStackTranslator != null) {
            itemData = itemStackTranslator.translateToBedrock(itemStack, bedrockItem);
        } else {
            itemData = DEFAULT_TRANSLATOR.translateToBedrock(itemStack, bedrockItem);
        }


        // Get the display name of the item
        NbtMap tag = itemData.getTag();
        if (tag != null) {
            NbtMap display = tag.getCompound("display");
            if (display != null && !display.isEmpty() && display.containsKey("Name")) {
                String name = display.getString("Name");

                // If its not a message convert it
                if (!MessageUtils.isMessage(name)) {
                    TextComponent component = LegacyComponentSerializer.legacySection().deserialize(name);
                    name = GsonComponentSerializer.gson().serialize(component);
                }

                // Check if its a message to translate
                if (MessageUtils.isMessage(name)) {
                    // Get the translated name
                    name = MessageUtils.getTranslatedBedrockMessage(MessageSerializer.fromString(name), session.getClientData().getLanguageCode());

                    // Build the new display tag
                    NbtMapBuilder displayBuilder = display.toBuilder();
                    displayBuilder.putString("Name", name);

                    // Build the new root tag
                    NbtMapBuilder builder = tag.toBuilder();
                    builder.put("display", displayBuilder.build());

                    // Create a new item with the original data + updated name
                    itemData = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount(), builder.build());
                }
            }
        }

        return itemData;
    }

    private static final ItemTranslator DEFAULT_TRANSLATOR = new ItemTranslator() {
        @Override
        public List<ItemEntry> getAppliedItems() {
            return null;
        }
    };

    public ItemData translateToBedrock(ItemStack itemStack, ItemEntry itemEntry) {
        if (itemStack == null) {
            return ItemData.AIR;
        }
        if (itemStack.getNbt() == null) {
            return ItemData.of(itemEntry.getBedrockId(), (short) itemEntry.getBedrockData(), itemStack.getAmount());
        }
        return ItemData.of(itemEntry.getBedrockId(), (short) itemEntry.getBedrockData(), itemStack.getAmount(), this.translateNbtToBedrock(itemStack.getNbt()));
    }

    public ItemStack translateToJava(ItemData itemData, ItemEntry itemEntry) {
        if (itemData == null) return null;
        if (itemData.getTag() == null) {
            return new ItemStack(itemEntry.getJavaId(), itemData.getCount(), new com.github.steveice10.opennbt.tag.builtin.CompoundTag(""));
        }
        return new ItemStack(itemEntry.getJavaId(), itemData.getCount(), this.translateToJavaNBT("", itemData.getTag()));
    }

    public abstract List<ItemEntry> getAppliedItems();

    public NbtMap translateNbtToBedrock(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag) {
        Map<String, Object> javaValue = new HashMap<>();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                com.github.steveice10.opennbt.tag.builtin.Tag javaTag = tag.get(str);
                Object translatedTag = translateToBedrockNBT(javaTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(javaTag.getName(), translatedTag);
            }
        }
        NbtMapBuilder builder = NbtMap.builder();
        javaValue.forEach(builder::put);
        return builder.build();
    }

    private Object translateToBedrockNBT(com.github.steveice10.opennbt.tag.builtin.Tag tag) {
        if (tag instanceof ByteArrayTag) {
            return ((ByteArrayTag) tag).getValue();
        }

        if (tag instanceof ByteTag) {
            return ((ByteTag) tag).getValue();
        }

        if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).getValue();
        }

        if (tag instanceof FloatTag) {
            return ((FloatTag) tag).getValue();
        }

        if (tag instanceof IntArrayTag) {
            return ((IntArrayTag) tag).getValue();
        }

        if (tag instanceof IntTag) {
            return ((IntTag) tag).getValue();
        }

        if (tag instanceof LongArrayTag) {
            //Long array tag does not exist in BE
            //LongArrayTag longArrayTag = (LongArrayTag) tag;
            //return new com.nukkitx.nbt.tag.LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
            return null;
        }

        if (tag instanceof LongTag) {
            return ((LongTag) tag).getValue();
        }

        if (tag instanceof ShortTag) {
            return ((ShortTag) tag).getValue();
        }

        if (tag instanceof StringTag) {
            return ((StringTag) tag).getValue();
        }

        if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;

            List<Object> tagList = new ArrayList<>();
            for (com.github.steveice10.opennbt.tag.builtin.Tag value : listTag) {
                tagList.add(translateToBedrockNBT(value));
            }
            NbtType<?> type = NbtType.COMPOUND;
            if (!tagList.isEmpty()) {
                type = NbtType.byClass(tagList.get(0).getClass());
            }
            return new NbtList(type, tagList);
        }

        if (tag instanceof com.github.steveice10.opennbt.tag.builtin.CompoundTag) {
            com.github.steveice10.opennbt.tag.builtin.CompoundTag compoundTag = (com.github.steveice10.opennbt.tag.builtin.CompoundTag) tag;
            return translateNbtToBedrock(compoundTag);
        }

        return null;
    }

    public com.github.steveice10.opennbt.tag.builtin.CompoundTag translateToJavaNBT(String name, NbtMap tag) {
        com.github.steveice10.opennbt.tag.builtin.CompoundTag javaTag = new com.github.steveice10.opennbt.tag.builtin.CompoundTag(name);
        Map<String, com.github.steveice10.opennbt.tag.builtin.Tag> javaValue = javaTag.getValue();
        if (tag != null && !tag.isEmpty()) {
            for (String str : tag.keySet()) {
                Object bedrockTag = tag.get(str);
                com.github.steveice10.opennbt.tag.builtin.Tag translatedTag = translateToJavaNBT(str, bedrockTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(translatedTag.getName(), translatedTag);
            }
        }

        javaTag.setValue(javaValue);
        return javaTag;
    }

    private com.github.steveice10.opennbt.tag.builtin.Tag translateToJavaNBT(String name, Object object) {
        if (object instanceof int[]) {
            return new IntArrayTag(name, (int[]) object);
        }

        if (object instanceof byte[]) {
            return new ByteArrayTag(name, (byte[]) object);
        }
        
        if (object instanceof Byte) {
            return new ByteTag(name, (byte) object);
        }

        if (object instanceof Float) {
            return new FloatTag(name, (float) object);
        }

        if (object instanceof Double) {
            return new DoubleTag(name, (double) object);
        }

        if (object instanceof Integer) {
            return new IntTag(name, (int) object);
        }

        if (object instanceof long[]) {
            return new LongArrayTag(name, (long[]) object);
        }

        if (object instanceof Long) {
            return new LongTag(name, (long) object);
        }

        if (object instanceof Short) {
            return new ShortTag(name, (short) object);
        }

        if (object instanceof String) {
            return new StringTag(name, (String) object);
        }

        if (object instanceof List) {
            List<com.github.steveice10.opennbt.tag.builtin.Tag> tags = new ArrayList<>();

            for (Object value : (List<?>) object) {
                com.github.steveice10.opennbt.tag.builtin.Tag javaTag = translateToJavaNBT("", value);
                if (javaTag != null)
                    tags.add(javaTag);
            }
            return new ListTag(name, tags);
        }

        if (object instanceof NbtMap) {
            NbtMap map = (NbtMap) object;
            return translateToJavaNBT(name, map);
        }

        return null;
    }

    /**
     * Checks if an {@link ItemStack} is equal to another item stack
     *
     * @param itemStack the item stack to check
     * @param equalsItemStack the item stack to check if equal to
     * @param checkAmount if the amount should be taken into account
     * @param trueIfAmountIsGreater if this should return true if the amount of the
     *                              first item stack is greater than that of the second
     * @param checkNbt if NBT data should be checked
     * @return if an item stack is equal to another item stack
     */
    public boolean equals(ItemStack itemStack, ItemStack equalsItemStack, boolean checkAmount, boolean trueIfAmountIsGreater, boolean checkNbt) {
        if (itemStack.getId() != equalsItemStack.getId()) {
            return false;
        }
        if (checkAmount) {
            if (trueIfAmountIsGreater) {
                if (itemStack.getAmount() < equalsItemStack.getAmount()) {
                    return false;
                }
            } else {
                if (itemStack.getAmount() != equalsItemStack.getAmount()) {
                    return false;
                }
            }
        }

        if (!checkNbt) {
            return true;
        }
        if ((itemStack.getNbt() == null || itemStack.getNbt().isEmpty()) && (equalsItemStack.getNbt() != null && !equalsItemStack.getNbt().isEmpty())) {
            return false;
        }

        if ((itemStack.getNbt() != null && !itemStack.getNbt().isEmpty() && (equalsItemStack.getNbt() == null || !equalsItemStack.getNbt().isEmpty()))) {
            return false;
        }

        if (itemStack.getNbt() != null && equalsItemStack.getNbt() != null) {
            return itemStack.getNbt().equals(equalsItemStack.getNbt());
        }

        return true;
    }

}
