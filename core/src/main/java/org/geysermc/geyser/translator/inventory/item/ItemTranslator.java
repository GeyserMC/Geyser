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

package org.geysermc.geyser.translator.inventory.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.text.MinecraftLocale;

import javax.annotation.Nonnull;
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
        Map<NbtItemStackTranslator, Integer> loadedNbtItemTranslators = new HashMap<>();
        for (Class<?> clazz : FileUtils.getGeneratedClassesForAnnotation(ItemRemapper.class)) {
            int priority = clazz.getAnnotation(ItemRemapper.class).priority();

            GeyserImpl.getInstance().getLogger().debug("Found annotated item translator: " + clazz.getCanonicalName());

            try {
                if (NbtItemStackTranslator.class.isAssignableFrom(clazz)) {
                    NbtItemStackTranslator nbtItemTranslator = (NbtItemStackTranslator) clazz.newInstance();
                    loadedNbtItemTranslators.put(nbtItemTranslator, priority);
                    continue;
                }
                ItemTranslator itemStackTranslator = (ItemTranslator) clazz.newInstance();
                List<ItemMapping> appliedItems = itemStackTranslator.getAppliedItems();
                for (ItemMapping item : appliedItems) {
                    ItemTranslator registered = ITEM_STACK_TRANSLATORS.get(item.getJavaId());
                    if (registered != null) {
                        GeyserImpl.getInstance().getLogger().error("Could not instantiate annotated item translator " +
                                clazz.getCanonicalName() + ". Item translator " + registered.getClass().getCanonicalName() +
                                " is already registered for the item " + item.getJavaIdentifier());
                        continue;
                    }
                    ITEM_STACK_TRANSLATORS.put(item.getJavaId(), itemStackTranslator);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                GeyserImpl.getInstance().getLogger().error("Could not instantiate annotated item translator " + clazz.getCanonicalName());
            }
        }

        NBT_TRANSLATORS = loadedNbtItemTranslators.keySet().stream().sorted(Comparator.comparingInt(loadedNbtItemTranslators::get)).collect(Collectors.toList());
    }

    /**
     * @param mappings item mappings to use while translating. This can't just be a Geyser session as this method is used
     *                 when loading recipes.
     */
    public static ItemStack translateToJava(ItemData data, ItemMappings mappings) {
        if (data == null) {
            return new ItemStack(0);
        }

        ItemMapping javaItem = mappings.getMapping(data);

        ItemStack itemStack;
        ItemTranslator itemStackTranslator = ITEM_STACK_TRANSLATORS.get(javaItem.getJavaId());
        if (itemStackTranslator != null) {
            itemStack = itemStackTranslator.translateToJava(data, javaItem, mappings);
        } else {
            itemStack = DEFAULT_TRANSLATOR.translateToJava(data, javaItem, mappings);
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

    @Nonnull
    public static ItemData translateToBedrock(GeyserSession session, ItemStack stack) {
        if (stack == null) {
            return ItemData.AIR;
        }

        ItemMapping bedrockItem = session.getItemMappings().getMapping(stack);
        if (bedrockItem == null) {
            session.getGeyser().getLogger().debug("No matching ItemMapping for " + stack);
            return ItemData.AIR;
        }

        CompoundTag nbt = stack.getNbt() != null ? stack.getNbt().clone() : null;

        // This is a fallback for maps with no nbt
        if (nbt == null && bedrockItem.getJavaIdentifier().equals("minecraft:filled_map")) {
            nbt = new CompoundTag("");
            nbt.put(new IntTag("map", 0));
        }

        if (nbt != null) {
            for (NbtItemStackTranslator translator : NBT_TRANSLATORS) {
                if (translator.acceptItem(bedrockItem)) {
                    translator.translateToBedrock(session, nbt, bedrockItem);
                }
            }
        }

        nbt = translateDisplayProperties(session, nbt, bedrockItem);
        if (session.isAdvancedTooltips()) {
            nbt = addAdvancedTooltips(nbt, session.getItemMappings().getMapping(stack), session.getLocale());
        }

        ItemStack itemStack = new ItemStack(stack.getId(), stack.getAmount(), nbt);

        ItemData.Builder builder;
        ItemTranslator itemStackTranslator = ITEM_STACK_TRANSLATORS.get(bedrockItem.getJavaId());
        if (itemStackTranslator != null) {
            builder = itemStackTranslator.translateToBedrock(itemStack, bedrockItem, session.getItemMappings());
        } else {
            builder = DEFAULT_TRANSLATOR.translateToBedrock(itemStack, bedrockItem, session.getItemMappings());
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
            canBreak = getCanModify(canDestroy, canBreak);
            canPlace = getCanModify(canPlaceOn, canPlace);
            builder.canBreak(canBreak);
            builder.canPlace(canPlace);
        }

        return builder.build();
    }

    private static CompoundTag addAdvancedTooltips(CompoundTag nbt, ItemMapping mapping, String language) {
        CompoundTag newNbt = nbt;
        if (newNbt == null) {
            newNbt = new CompoundTag("nbt");
            CompoundTag display = new CompoundTag("display");
            display.put(new ListTag("Lore"));
            newNbt.put(display);
        }
        CompoundTag compoundTag = newNbt.get("display");
        if (compoundTag == null) {
            compoundTag = new CompoundTag("display");
        }
        ListTag listTag = compoundTag.get("Lore");

        if (listTag == null) {
            listTag = new ListTag("Lore");
        }
        int maxDurability = mapping.getMaxDamage();

        if (maxDurability != 0) {
            int durability = maxDurability - ((IntTag) newNbt.get("Damage")).getValue();
            if (durability != maxDurability) {
                listTag.add(new StringTag("", "§r§f" + String.format(MessageTranslator.convertMessage("item.durability", language), durability, maxDurability)));
            }
        }

        listTag.add(new StringTag("", "§r§8" + mapping.getJavaIdentifier()));
        if (nbt != null) {
            listTag.add(new StringTag("", "§r§8" + String.format(MessageTranslator.convertMessage("item.nbt_tags", language), nbt.size())));
        }
        compoundTag.put(listTag);
        newNbt.put(compoundTag);
        return newNbt;
    }

    /**
     * Translates the Java NBT of canDestroy and canPlaceOn to its Bedrock counterparts.
     * In Java, this is treated as normal NBT, but in Bedrock, these arguments are extra parts of the item data itself.
     *
     * @param canModifyJava the list of items in Java
     * @param canModifyBedrock the empty list of items in Bedrock
     * @return the new list of items in Bedrock
     */
    private static String[] getCanModify(ListTag canModifyJava, String[] canModifyBedrock) {
        if (canModifyJava != null && canModifyJava.size() > 0) {
            canModifyBedrock = new String[canModifyJava.size()];
            for (int i = 0; i < canModifyBedrock.length; i++) {
                // Get the Java identifier of the block that can be placed
                String block = ((StringTag) canModifyJava.get(i)).getValue();
                // Sometimes this is done but it's still valid
                if (!block.startsWith("minecraft:")) block = "minecraft:" + block;
                // Get the Bedrock identifier of the item and replace it.
                // This will unfortunately be limited - for example, beds and banners will be translated weirdly
                canModifyBedrock[i] = BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.getOrDefault(block, block).replace("minecraft:", "");
            }
        }
        return canModifyBedrock;
    }

    private static final ItemTranslator DEFAULT_TRANSLATOR = new ItemTranslator() {
        @Override
        public List<ItemMapping> getAppliedItems() {
            return null;
        }
    };

    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        if (itemStack == null) {
            // Return, essentially, air
            return ItemData.builder();
        }
        ItemData.Builder builder = ItemData.builder()
                .id(mapping.getBedrockId())
                .damage(mapping.getBedrockData())
                .count(itemStack.getAmount());
        if (itemStack.getNbt() != null) {
            builder.tag(this.translateNbtToBedrock(itemStack.getNbt()));
        }
        return builder;
    }

    public ItemStack translateToJava(ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        if (itemData == null) return null;
        if (itemData.getTag() == null) {
            return new ItemStack(mapping.getJavaId(), itemData.getCount(), new CompoundTag(""));
        }
        return new ItemStack(mapping.getJavaId(), itemData.getCount(), this.translateToJavaNBT("", itemData.getTag()));
    }

    public abstract List<ItemMapping> getAppliedItems();

    public NbtMap translateNbtToBedrock(CompoundTag tag) {
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

    private Object translateToBedrockNBT(Tag tag) {
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

        if (tag instanceof ListTag listTag) {

            List<Object> tagList = new ArrayList<>();
            for (Tag value : listTag) {
                tagList.add(translateToBedrockNBT(value));
            }
            NbtType<?> type = NbtType.COMPOUND;
            if (!tagList.isEmpty()) {
                type = NbtType.byClass(tagList.get(0).getClass());
            }
            return new NbtList(type, tagList);
        }

        if (tag instanceof CompoundTag compoundTag) {
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

        if (object instanceof NbtMap map) {
            return translateToJavaNBT(name, map);
        }

        return null;
    }

    /**
     * Translates the display name of the item
     * @param session the Bedrock client's session
     * @param tag the tag to translate
     * @param mapping the item entry, in case it requires translation
     *
     * @return the new tag to use, should the current one be null
     */
    public static CompoundTag translateDisplayProperties(GeyserSession session, CompoundTag tag, ItemMapping mapping) {
        return translateDisplayProperties(session, tag, mapping, 'f');
    }

    /**
     * @param translationColor if this item is not available on Java, the color that the new name should be.
     *                         Normally, this should just be white, but for shulker boxes this should be gray.
     */
    public static CompoundTag translateDisplayProperties(GeyserSession session, CompoundTag tag, ItemMapping mapping, char translationColor) {
        boolean hasCustomName = false;
        if (tag != null) {
            CompoundTag display = tag.get("display");
            if (display != null && display.contains("Name")) {
                String name = ((StringTag) display.get("Name")).getValue();

                // Get the translated name and prefix it with a reset char
                name = MessageTranslator.convertMessageLenient(name, session.getLocale());

                // Add the new name tag
                display.put(new StringTag("Name", name));
                // Indicate that a custom name is present
                hasCustomName = true;

                // Add to the new root tag
                tag.put(display);
            }
        }

        if (!hasCustomName && mapping.hasTranslation()) {
            // No custom name, but we need to localize the item's name
            if (tag == null) {
                tag = new CompoundTag("");
            }
            CompoundTag display = tag.get("display");
            if (display == null) {
                display = new CompoundTag("display");
                // Add to the new root tag
                tag.put(display);
            }

            String translationKey = mapping.getTranslationString();
            // Reset formatting since Bedrock defaults to italics
            display.put(new StringTag("Name", "§r§" + translationColor + MinecraftLocale.getLocaleString(translationKey, session.getLocale())));
        }

        return tag;
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
