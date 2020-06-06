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
import com.github.steveice10.mc.protocol.data.message.Message;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.Tag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.utils.MessageUtils;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ItemTranslator {

    private static final Int2ObjectMap<ItemTranslator> ITEM_STACK_TRANSLATORS = new Int2ObjectOpenHashMap<>();
    private static final List<NbtItemStackTranslator> NBT_TRANSLATORS;

    protected ItemTranslator() {
    }

    public static void init() {
        // no-op
    }

    static {
        /* Load item translators */
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
                ItemTranslator itemStackTranslator = (ItemTranslator) clazz.newInstance();
                List<ItemEntry> appliedItems = itemStackTranslator.getAppliedItems();
                for (ItemEntry item : appliedItems) {
                    ItemTranslator registered = ITEM_STACK_TRANSLATORS.get(item.getJavaId());
                    if (registered != null) {
                        GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated item translator " + clazz.getCanonicalName() + "." +
                                " Item translator " + registered.getClass().getCanonicalName() + " is already registered for the item " + item.getJavaIdentifier());
                        continue;
                    }
                    ITEM_STACK_TRANSLATORS.put(item.getJavaId(), itemStackTranslator);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Could not instantiate annotated item translator " + clazz.getCanonicalName() + ".");
            }
        }

        NBT_TRANSLATORS = loadedNbtItemTranslators.keySet().stream().sorted(Comparator.comparingInt(loadedNbtItemTranslators::get)).collect(Collectors.toList());
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

        ItemStack itemStack = new ItemStack(stack.getId(), stack.getAmount(), stack.getNbt() != null ? stack.getNbt().clone() : null);

        if (itemStack.getNbt() != null) {
            for (NbtItemStackTranslator translator : NBT_TRANSLATORS) {
                if (translator.acceptItem(bedrockItem)) {
                    translator.translateToBedrock(itemStack.getNbt(), bedrockItem);
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
        CompoundTag tag = itemData.getTag();
        if (tag != null) {
            CompoundTag display = tag.getCompound("display");
            if (display != null) {
                String name = display.getString("Name");

                // Check if its a message to translate
                if (MessageUtils.isMessage(name)) {
                    // Get the translated name
                    name = MessageUtils.getTranslatedBedrockMessage(Message.fromString(name), session.getClientData().getLanguageCode());

                    // Build the new display tag
                    CompoundTagBuilder displayBuilder = display.toBuilder();
                    displayBuilder.stringTag("Name", name);

                    // Build the new root tag
                    CompoundTagBuilder builder = tag.toBuilder();
                    builder.tag(displayBuilder.build("display"));

                    // Create a new item with the original data + updated name
                    itemData = ItemData.of(itemData.getId(), itemData.getDamage(), itemData.getCount(), builder.buildRootTag());
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
        return new ItemStack(itemEntry.getJavaId(), itemData.getCount(), this.translateToJavaNBT(itemData.getTag()));
    }

    public abstract List<ItemEntry> getAppliedItems();

    public CompoundTag translateNbtToBedrock(com.github.steveice10.opennbt.tag.builtin.CompoundTag tag) {
        Map<String, Tag<?>> javaValue = new HashMap<>();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                com.github.steveice10.opennbt.tag.builtin.Tag javaTag = tag.get(str);
                com.nukkitx.nbt.tag.Tag<?> translatedTag = translateToBedrockNBT(javaTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(translatedTag.getName(), translatedTag);
            }
        }

        return new CompoundTag(tag.getName(), javaValue);
    }

    private Tag<?> translateToBedrockNBT(com.github.steveice10.opennbt.tag.builtin.Tag tag) {
        if (tag instanceof ByteArrayTag) {
            ByteArrayTag byteArrayTag = (ByteArrayTag) tag;
            return new com.nukkitx.nbt.tag.ByteArrayTag(byteArrayTag.getName(), byteArrayTag.getValue());
        }

        if (tag instanceof ByteTag) {
            ByteTag byteTag = (ByteTag) tag;
            return new com.nukkitx.nbt.tag.ByteTag(byteTag.getName(), byteTag.getValue());
        }

        if (tag instanceof DoubleTag) {
            DoubleTag doubleTag = (DoubleTag) tag;
            return new com.nukkitx.nbt.tag.DoubleTag(doubleTag.getName(), doubleTag.getValue());
        }

        if (tag instanceof FloatTag) {
            FloatTag floatTag = (FloatTag) tag;
            return new com.nukkitx.nbt.tag.FloatTag(floatTag.getName(), floatTag.getValue());
        }

        if (tag instanceof IntArrayTag) {
            IntArrayTag intArrayTag = (IntArrayTag) tag;
            return new com.nukkitx.nbt.tag.IntArrayTag(intArrayTag.getName(), intArrayTag.getValue());
        }

        if (tag instanceof IntTag) {
            IntTag intTag = (IntTag) tag;
            return new com.nukkitx.nbt.tag.IntTag(intTag.getName(), intTag.getValue());
        }

        if (tag instanceof LongArrayTag) {
            LongArrayTag longArrayTag = (LongArrayTag) tag;
            return new com.nukkitx.nbt.tag.LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
        }

        if (tag instanceof LongTag) {
            LongTag longTag = (LongTag) tag;
            return new com.nukkitx.nbt.tag.LongTag(longTag.getName(), longTag.getValue());
        }

        if (tag instanceof ShortTag) {
            ShortTag shortTag = (ShortTag) tag;
            return new com.nukkitx.nbt.tag.ShortTag(shortTag.getName(), shortTag.getValue());
        }

        if (tag instanceof StringTag) {
            StringTag stringTag = (StringTag) tag;
            return new com.nukkitx.nbt.tag.StringTag(stringTag.getName(), stringTag.getValue());
        }

        if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;

            List<Tag<?>> tagList = new ArrayList<>();
            for (com.github.steveice10.opennbt.tag.builtin.Tag value : listTag) {
                tagList.add(translateToBedrockNBT(value));
            }
            Class<?> clazz = CompoundTag.class;
            if (!tagList.isEmpty()) {
                clazz = tagList.get(0).getClass();
            }
            return new com.nukkitx.nbt.tag.ListTag(listTag.getName(), clazz, tagList);
        }

        if (tag instanceof com.github.steveice10.opennbt.tag.builtin.CompoundTag) {
            com.github.steveice10.opennbt.tag.builtin.CompoundTag compoundTag = (com.github.steveice10.opennbt.tag.builtin.CompoundTag) tag;

            return translateNbtToBedrock(compoundTag);
        }

        return null;
    }

    public com.github.steveice10.opennbt.tag.builtin.CompoundTag translateToJavaNBT(com.nukkitx.nbt.tag.CompoundTag tag) {
        com.github.steveice10.opennbt.tag.builtin.CompoundTag javaTag = new com.github.steveice10.opennbt.tag.builtin.CompoundTag(tag.getName());
        Map<String, com.github.steveice10.opennbt.tag.builtin.Tag> javaValue = javaTag.getValue();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                Tag<?> bedrockTag = tag.get(str);
                com.github.steveice10.opennbt.tag.builtin.Tag translatedTag = translateToJavaNBT(bedrockTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(translatedTag.getName(), translatedTag);
            }
        }

        javaTag.setValue(javaValue);
        return javaTag;
    }

    private com.github.steveice10.opennbt.tag.builtin.Tag translateToJavaNBT(com.nukkitx.nbt.tag.Tag<?> tag) {
        if (tag instanceof com.nukkitx.nbt.tag.ByteArrayTag) {
            com.nukkitx.nbt.tag.ByteArrayTag byteArrayTag = (com.nukkitx.nbt.tag.ByteArrayTag) tag;
            return new ByteArrayTag(byteArrayTag.getName(), byteArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ByteTag) {
            com.nukkitx.nbt.tag.ByteTag byteTag = (com.nukkitx.nbt.tag.ByteTag) tag;
            return new ByteTag(byteTag.getName(), byteTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.DoubleTag) {
            com.nukkitx.nbt.tag.DoubleTag doubleTag = (com.nukkitx.nbt.tag.DoubleTag) tag;
            return new DoubleTag(doubleTag.getName(), doubleTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.FloatTag) {
            com.nukkitx.nbt.tag.FloatTag floatTag = (com.nukkitx.nbt.tag.FloatTag) tag;
            return new FloatTag(floatTag.getName(), floatTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.IntArrayTag) {
            com.nukkitx.nbt.tag.IntArrayTag intArrayTag = (com.nukkitx.nbt.tag.IntArrayTag) tag;
            return new IntArrayTag(intArrayTag.getName(), intArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.IntTag) {
            com.nukkitx.nbt.tag.IntTag intTag = (com.nukkitx.nbt.tag.IntTag) tag;
            return new IntTag(intTag.getName(), intTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.LongArrayTag) {
            com.nukkitx.nbt.tag.LongArrayTag longArrayTag = (com.nukkitx.nbt.tag.LongArrayTag) tag;
            return new LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.LongTag) {
            com.nukkitx.nbt.tag.LongTag longTag = (com.nukkitx.nbt.tag.LongTag) tag;
            return new LongTag(longTag.getName(), longTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ShortTag) {
            com.nukkitx.nbt.tag.ShortTag shortTag = (com.nukkitx.nbt.tag.ShortTag) tag;
            return new ShortTag(shortTag.getName(), shortTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.StringTag) {
            com.nukkitx.nbt.tag.StringTag stringTag = (com.nukkitx.nbt.tag.StringTag) tag;
            return new StringTag(stringTag.getName(), stringTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ListTag) {
            com.nukkitx.nbt.tag.ListTag<?> listTag = (com.nukkitx.nbt.tag.ListTag<?>) tag;

            List<com.github.steveice10.opennbt.tag.builtin.Tag> tags = new ArrayList<>();

            for (Object value : listTag.getValue()) {
                if (!(value instanceof com.nukkitx.nbt.tag.Tag))
                    continue;

                com.nukkitx.nbt.tag.Tag<?> tagValue = (com.nukkitx.nbt.tag.Tag<?>) value;
                com.github.steveice10.opennbt.tag.builtin.Tag javaTag = translateToJavaNBT(tagValue);
                if (javaTag != null)
                    tags.add(javaTag);
            }
            return new ListTag(listTag.getName(), tags);
        }

        if (tag instanceof com.nukkitx.nbt.tag.CompoundTag) {
            com.nukkitx.nbt.tag.CompoundTag compoundTag = (com.nukkitx.nbt.tag.CompoundTag) tag;
            return translateToJavaNBT(compoundTag);
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
