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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.FileUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
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
                    NbtItemStackTranslator nbtItemTranslator = (NbtItemStackTranslator) clazz.getDeclaredConstructor().newInstance();
                    loadedNbtItemTranslators.put(nbtItemTranslator, priority);
                    continue;
                }
                ItemTranslator itemStackTranslator = (ItemTranslator) clazz.getDeclaredConstructor().newInstance();
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
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
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
                // Otherwise, seems to cause issues with villagers accepting books, and I don't see how this will break anything else. - Camotoy
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
            nbt = addAdvancedTooltips(nbt, bedrockItem, session.locale());
        }

        ItemStack itemStack = new ItemStack(stack.getId(), stack.getAmount(), nbt);

        ItemTranslator itemStackTranslator = ITEM_STACK_TRANSLATORS.getOrDefault(bedrockItem.getJavaId(), DEFAULT_TRANSLATOR);
        ItemData.Builder builder = itemStackTranslator.translateToBedrock(itemStack, bedrockItem, session.getItemMappings());
        if (bedrockItem.isBlock()) {
            builder.blockRuntimeId(bedrockItem.getBedrockBlockId());
        }

        if (nbt != null) {
            // Translate the canDestroy and canPlaceOn Java NBT
            ListTag canDestroy = nbt.get("CanDestroy");
            ListTag canPlaceOn = nbt.get("CanPlaceOn");
            String[] canBreak = getCanModify(canDestroy);
            String[] canPlace = getCanModify(canPlaceOn);
            if (canBreak != null) {
                builder.canBreak(canBreak);
            }
            if (canPlace != null) {
                builder.canPlace(canPlace);
            }
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
            Tag durabilityTag = newNbt.get("Damage");
            if (durabilityTag instanceof IntTag) {
                int durability = maxDurability - ((IntTag) durabilityTag).getValue();
                if (durability != maxDurability) {
                    Component component = Component.text()
                            .resetStyle()
                            .color(NamedTextColor.WHITE)
                            .append(Component.translatable("item.durability",
                                    Component.text(durability),
                                    Component.text(maxDurability)))
                            .build();
                    listTag.add(new StringTag("", MessageTranslator.convertMessage(component, language)));
                }
            }
        }

        listTag.add(new StringTag("", "§r§8" + mapping.getJavaIdentifier()));
        if (nbt != null) {
            Component component = Component.text()
                    .resetStyle()
                    .color(NamedTextColor.DARK_GRAY)
                    .append(Component.translatable("item.nbt_tags",
                            Component.text(nbt.size())))
                    .build();
            listTag.add(new StringTag("", MessageTranslator.convertMessage(component, language)));
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
     * @return the new list of items in Bedrock
     */
    private static String[] getCanModify(ListTag canModifyJava) {
        if (canModifyJava != null && canModifyJava.size() > 0) {
            String[] canModifyBedrock = new String[canModifyJava.size()];
            for (int i = 0; i < canModifyBedrock.length; i++) {
                // Get the Java identifier of the block that can be placed
                String block = ((StringTag) canModifyJava.get(i)).getValue();
                // Sometimes this is done but it's still valid
                if (!block.startsWith("minecraft:")) block = "minecraft:" + block;
                // Get the Bedrock identifier of the item and replace it.
                // This will unfortunately be limited - for example, beds and banners will be translated weirdly
                canModifyBedrock[i] = BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.getOrDefault(block, block).replace("minecraft:", "");
            }
            return canModifyBedrock;
        }
        return null;
    }

    /**
     * Given an item stack, determine the Bedrock item ID that should be applied to Bedrock players.
     */
    public static int getBedrockItemId(GeyserSession session, @Nonnull GeyserItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemMapping.AIR.getJavaId();
        }
        int javaId = itemStack.getJavaId();
        ItemMapping mapping = ITEM_STACK_TRANSLATORS.getOrDefault(javaId, DEFAULT_TRANSLATOR)
                .getItemMapping(javaId, itemStack.getNbt(), session.getItemMappings());

        int customItemId = CustomItemTranslator.getCustomItem(itemStack.getNbt(), mapping);
        if (customItemId == -1) {
            // No custom item
            return mapping.getBedrockId();
        } else {
            return customItemId;
        }
    }

    private static final ItemTranslator DEFAULT_TRANSLATOR = new ItemTranslator() {
        @Override
        public List<ItemMapping> getAppliedItems() {
            return null;
        }
    };

    protected ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
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

        CompoundTag nbt = itemStack.getNbt();
        translateCustomItem(nbt, builder, mapping);

        return builder;
    }

    public ItemStack translateToJava(ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        if (itemData == null) return null;
        if (itemData.getTag() == null) {
            return new ItemStack(mapping.getJavaId(), itemData.getCount(), new CompoundTag(""));
        }
        return new ItemStack(mapping.getJavaId(), itemData.getCount(), this.translateToJavaNBT("", itemData.getTag()));
    }

    /**
     * Used for initialization only and only called once.
     */
    public abstract List<ItemMapping> getAppliedItems();

    protected ItemMapping getItemMapping(int javaId, CompoundTag nbt, ItemMappings mappings) {
        return mappings.getMapping(javaId);
    }

    protected NbtMap translateNbtToBedrock(CompoundTag tag) {
        if (!tag.getValue().isEmpty()) {
            NbtMapBuilder builder = NbtMap.builder();
            for (Tag javaTag : tag.values()) {
                Object translatedTag = translateToBedrockNBT(javaTag);
                if (translatedTag == null)
                    continue;

                builder.put(javaTag.getName(), translatedTag);
            }
            return builder.build();
        }
        return NbtMap.EMPTY;
    }

    private Object translateToBedrockNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            return translateNbtToBedrock(compoundTag);
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

        if (tag instanceof LongArrayTag) {
            //Long array tag does not exist in BE
            //LongArrayTag longArrayTag = (LongArrayTag) tag;
            //return new com.nukkitx.nbt.tag.LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
            return null;
        }

        return tag.getValue();
    }

    private CompoundTag translateToJavaNBT(String name, NbtMap tag) {
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
            if (tag.get("display") instanceof CompoundTag display && display.get("Name") instanceof StringTag tagName) {
                String name = tagName.getValue();

                // Get the translated name and prefix it with a reset char
                name = MessageTranslator.convertMessageLenient(name, session.locale());

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
            CompoundTag display;
            if (tag.get("display") instanceof CompoundTag oldDisplay) {
                display = oldDisplay;
            } else {
                display = new CompoundTag("display");
                // Add to the new root tag
                tag.put(display);
            }

            String translationKey = mapping.getTranslationString();
            // Reset formatting since Bedrock defaults to italics
            display.put(new StringTag("Name", "§r§" + translationColor + MinecraftLocale.getLocaleString(translationKey, session.locale())));
        }

        return tag;
    }

    /**
     * Translates the custom model data of an item
     */
    private static void translateCustomItem(CompoundTag nbt, ItemData.Builder builder, ItemMapping mapping) {
        int bedrockId = CustomItemTranslator.getCustomItem(nbt, mapping);
        if (bedrockId != -1) {
            builder.id(bedrockId);
        }
    }

}
