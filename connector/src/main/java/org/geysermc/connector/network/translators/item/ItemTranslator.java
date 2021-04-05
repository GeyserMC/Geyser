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

package org.geysermc.connector.network.translators.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.reflections.Reflections;

import java.util.*;
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
        Reflections ref = GeyserConnector.getInstance().useXmlReflections() ? FileUtils.getReflections("org.geysermc.connector.network.translators.item") : new Reflections("org.geysermc.connector.network.translators.item");

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
                        GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.network.translator.item.already_registered", clazz.getCanonicalName(), registered.getClass().getCanonicalName(), item.getJavaIdentifier()));
                        continue;
                    }
                    ITEM_STACK_TRANSLATORS.put(item.getJavaId(), itemStackTranslator);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.network.translator.item.failed", clazz.getCanonicalName()));
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
            if (itemStack.getNbt().isEmpty()) {
                // Otherwise, seems to causes issues with villagers accepting books, and I don't see how this will break anything else. - Camotoy
                itemStack = new ItemStack(itemStack.getId(), itemStack.getAmount(), null);
            }
        }
        return itemStack;
    }

    public static ItemData translateToBedrock(GeyserSession session, ItemStack stack) {
        if (stack == null) {
            return ItemData.AIR;
        }

        ItemEntry bedrockItem = ItemRegistry.getItem(stack);
        if (bedrockItem == null) {
            session.getConnector().getLogger().debug("No matching ItemEntry for " + stack);
            return ItemData.AIR;
        }

        CompoundTag nbt = stack.getNbt() != null ? stack.getNbt().clone() : null;

        // This is a fallback for maps with no nbt
        if (nbt == null && bedrockItem.getJavaIdentifier().equals("minecraft:filled_map")) {
            nbt = new CompoundTag("");
            nbt.put(new IntTag("map", 0));
        }

        ItemStack itemStack = new ItemStack(stack.getId(), stack.getAmount(), nbt);

        if (nbt != null) {
            for (NbtItemStackTranslator translator : NBT_TRANSLATORS) {
                if (translator.acceptItem(bedrockItem)) {
                    translator.translateToBedrock(session, nbt, bedrockItem);
                }
            }
        }

        translateDisplayProperties(session, nbt);

        ItemData.Builder builder;
        ItemTranslator itemStackTranslator = ITEM_STACK_TRANSLATORS.get(bedrockItem.getJavaId());
        if (itemStackTranslator != null) {
            builder = itemStackTranslator.translateToBedrock(itemStack, bedrockItem);
        } else {
            builder = DEFAULT_TRANSLATOR.translateToBedrock(itemStack, bedrockItem);
        }
        if (bedrockItem.isBlock()) {
            builder.blockRuntimeId(bedrockItem.getBedrockBlockId());
        }

        if (nbt != null) {
            // Translate the canDestroy and canPlaceOn Java NBT
            ListTag canDestroy = nbt.get("CanDestroy");
            String[] canBreak = new String[0];
            ListTag canPlaceOn = nbt.get("CanPlaceOn");
            String[] canPlace = new String[0];
            canBreak = getCanModify(session, canDestroy, canBreak);
            canPlace = getCanModify(session, canPlaceOn, canPlace);
            builder.canBreak(canBreak);
            builder.canPlace(canPlace);
        }

        return builder.build();
    }

    /**
     * Translates the Java NBT of canDestroy and canPlaceOn to its Bedrock counterparts.
     * In Java, this is treated as normal NBT, but in Bedrock, these arguments are extra parts of the item data itself.
     *
     * @param canModifyJava the list of items in Java
     * @param canModifyBedrock the empty list of items in Bedrock
     * @return the new list of items in Bedrock
     */
    private static String[] getCanModify(GeyserSession session, ListTag canModifyJava, String[] canModifyBedrock) {
        if (canModifyJava != null && canModifyJava.size() > 0) {
            canModifyBedrock = new String[canModifyJava.size()];
            for (int i = 0; i < canModifyBedrock.length; i++) {
                // Get the Java identifier of the block that can be placed
                String block = ((StringTag) canModifyJava.get(i)).getValue();
                // Sometimes this is done but it's still valid
                if (!block.startsWith("minecraft:")) block = "minecraft:" + block;
                // Get the Bedrock identifier of the item and replace it.
                // This will unfortunately be limited - for example, beds and banners will be translated weirdly
                canModifyBedrock[i] = session.getBlockTranslator().getBedrockBlockIdentifier(block).replace("minecraft:", "");
            }
        }
        return canModifyBedrock;
    }

    private static final ItemTranslator DEFAULT_TRANSLATOR = new ItemTranslator() {
        @Override
        public List<ItemEntry> getAppliedItems() {
            return null;
        }
    };

    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemEntry itemEntry) {
        if (itemStack == null) {
            // Return, essentially, air
            return ItemData.builder();
        }
        ItemData.Builder builder = ItemData.builder()
                .id(itemEntry.getBedrockId())
                .damage(itemEntry.getBedrockData())
                .count(itemStack.getAmount());
        if (itemStack.getNbt() != null) {
            builder.tag(this.translateNbtToBedrock(itemStack.getNbt()));
        }
        return builder;
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
        NbtMapBuilder builder = NbtMap.builder();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                Tag javaTag = tag.get(str);
                Object translatedTag = translateToBedrockNBT(javaTag);
                if (translatedTag == null)
                    continue;

                builder.put(javaTag.getName(), translatedTag);
            }
        }
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

        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag) tag;
            return translateNbtToBedrock(compoundTag);
        }

        return null;
    }

    public CompoundTag translateToJavaNBT(String name, NbtMap tag) {
        CompoundTag javaTag = new CompoundTag(name);
        Map<String, Tag> javaValue = javaTag.getValue();
        if (tag != null && !tag.isEmpty()) {
            for (Map.Entry<String, Object> entry : tag.entrySet()) {
                Tag translatedTag = translateToJavaNBT(entry.getKey(), entry.getValue());
                if (translatedTag == null)
                    continue;

                javaValue.put(translatedTag.getName(), translatedTag);
            }
        }

        javaTag.setValue(javaValue);
        return javaTag;
    }

    private Tag translateToJavaNBT(String name, Object object) {
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
            List<Tag> tags = new ArrayList<>();

            for (Object value : (List<?>) object) {
                Tag javaTag = translateToJavaNBT("", value);
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
     * Translates the display name of the item
     * @param session the Bedrock client's session
     * @param tag the tag to translate
     */
    public static void translateDisplayProperties(GeyserSession session, CompoundTag tag) {
        if (tag != null) {
            CompoundTag display = tag.get("display");
            if (display != null && display.contains("Name")) {
                String name = ((StringTag) display.get("Name")).getValue();

                // Get the translated name and prefix it with a reset char
                name = MessageTranslator.convertMessageLenient(name, session.getLocale());

                // Add the new name tag
                display.put(new StringTag("Name", name));

                // Add to the new root tag
                tag.put(display);
            }
        }
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
