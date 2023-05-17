/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.data.game.Identifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.DoubleTag;
import com.github.steveice10.opennbt.tag.builtin.FloatTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.LongArrayTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.util.*;

public final class ItemTranslator {
    private ItemTranslator() {
    }

    /**
     * @param mappings item mappings to use while translating. This can't just be a Geyser session as this method is used
     *                 when loading recipes.
     */
    public static ItemStack translateToJava(ItemData data, ItemMappings mappings) {
        if (data == null) {
            return new ItemStack(0);
        }

        ItemMapping bedrockItem = mappings.getMapping(data);
        Item javaItem = bedrockItem.getJavaItem();

        ItemStack itemStack = javaItem.translateToJava(data, bedrockItem, mappings);

        if (itemStack != null && itemStack.getNbt() != null) {
            javaItem.translateNbtToJava(itemStack.getNbt(), bedrockItem);
            if (itemStack.getNbt().isEmpty()) {
                // Otherwise, seems to cause issues with villagers accepting books, and I don't see how this will break anything else. - Camotoy
                itemStack = new ItemStack(itemStack.getId(), itemStack.getAmount(), null);
            }
        }
        return itemStack;
    }

    @Nonnull
    public static ItemData.Builder translateToBedrock(GeyserSession session, int javaId, int count, CompoundTag tag) {
        ItemMapping bedrockItem = session.getItemMappings().getMapping(javaId);
        if (bedrockItem == ItemMapping.AIR) {
            session.getGeyser().getLogger().debug("ItemMapping returned air: " + javaId);
            return ItemData.builder();
        }
        return translateToBedrock(session, Registries.JAVA_ITEMS.get().get(javaId), bedrockItem, count, tag);
    }

    @Nonnull
    public static ItemData translateToBedrock(GeyserSession session, ItemStack stack) {
        if (stack == null) {
            return ItemData.AIR;
        }

        ItemMapping bedrockItem = session.getItemMappings().getMapping(stack);
        if (bedrockItem == ItemMapping.AIR) {
            session.getGeyser().getLogger().debug("ItemMapping returned air: " + stack);
            return ItemData.AIR;
        }
        // Java item needs to be loaded separately. The mapping for tipped arrow would
        return translateToBedrock(session, Registries.JAVA_ITEMS.get().get(stack.getId()), bedrockItem, stack.getAmount(), stack.getNbt())
                .build();
    }

    @Nonnull
    private static ItemData.Builder translateToBedrock(GeyserSession session, Item javaItem, ItemMapping bedrockItem, int count, CompoundTag tag) {
        CompoundTag nbt = tag != null ? tag.clone() : null;

        if (nbt != null) {
            javaItem.translateNbtToBedrock(session, nbt);
        }

        nbt = translateDisplayProperties(session, nbt, bedrockItem);

        if (nbt != null) {
            addAttributes(nbt, javaItem, session.locale());
        }

        if (session.isAdvancedTooltips()) {
            nbt = addAdvancedTooltips(nbt, javaItem, session.locale());
        }

        ItemStack itemStack = new ItemStack(javaItem.javaId(), count, nbt);

        ItemData.Builder builder = javaItem.translateToBedrock(itemStack, bedrockItem, session.getItemMappings());
        if (bedrockItem.isBlock()) {
            CustomBlockData customBlockData = BlockRegistries.CUSTOM_BLOCK_ITEM_OVERRIDES.getOrDefault(
                    bedrockItem.getJavaItem().javaIdentifier(), null);
            if (customBlockData != null) {
                translateCustomBlock(customBlockData, session, builder);
            } else {
                builder.blockDefinition(bedrockItem.getBedrockBlockDefinition());
            }
        }

        if (bedrockItem.getJavaItem().equals(Items.PLAYER_HEAD)) {
            translatePlayerHead(session, nbt, builder);
        }

        translateCustomItem(nbt, builder, bedrockItem);

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

        return builder;
    }

    private static CompoundTag addAttributes(CompoundTag nbt, Item item, String language) {
        ListTag modifiers = nbt.get("AttributeModifiers");
        if (modifiers == null) return nbt;
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
        String[] allSlots = new String[]{"mainhand", "offhand", "feet", "legs", "chest", "head"};
        DecimalFormat decimalFormat = new DecimalFormat("0.#####");
        Map<String, List<Tag>> slotsToModifiers = new HashMap<>();
        for (String slot : allSlots) {
            slotsToModifiers.put(slot, new ArrayList<>());
        }
        for (Tag modifier : modifiers) {
            Map<String, Tag> modifierValue = (Map) modifier.getValue();
            String[] slots = allSlots;
            if (modifierValue.get("Slot") != null) {
                slots = new String[]{(String) modifierValue.get("Slot").getValue()};
            }
            for (String slot : slots) {
                List<Tag> list = slotsToModifiers.get(slot);
                list.add(modifier);
                slotsToModifiers.put(slot, list);
            }
        }

        for (String slot : allSlots) {
            List<Tag> modifiersList = slotsToModifiers.get(slot);
            if (modifiersList.isEmpty()) continue;
            Component slotComponent = Component.text()
                    .resetStyle()
                    .color(NamedTextColor.GRAY)
                    .append(Component.newline(), Component.translatable("item.modifiers." + slot))
                    .build();
            listTag.add(new StringTag("", MessageTranslator.convertMessage(slotComponent, language)));


            for (Tag modifier : modifiersList) {
                Map<String, Tag> modifierValue = (Map) modifier.getValue();
                double amount;
                if (modifierValue.get("Amount") instanceof IntTag intTag) {
                    amount = (double) intTag.getValue();
                } else if (modifierValue.get("Amount") instanceof DoubleTag doubleTag) {
                    amount = doubleTag.getValue();
                } else {
                    continue;
                }
                if (amount == 0) {
                    continue;
                }
                ModifierOperation operation = ModifierOperation.from((int) modifierValue.get("Operation").getValue());
                String operationTotal;
                if (operation == ModifierOperation.ADD) {
                    if (modifierValue.get("Name").equals("knockback_resistance")) {
                        amount *= 10;
                    }
                    operationTotal = decimalFormat.format(amount);
                } else if (operation == ModifierOperation.ADD_MULTIPLIED || operation == ModifierOperation.MULTIPLY) {
                    operationTotal = decimalFormat.format(amount * 100) + "%";
                } else {
                    continue;
                }
                if (amount > 0) {
                    operationTotal = "+" + operationTotal;
                }

                Component attributeComponent = Component.text()
                        .resetStyle()
                        .color(amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED)
                        .append(Component.text(operationTotal), Component.text(" "), Component.translatable("attribute.name." + modifierValue.get("Name").getValue()))
                        .build();
                listTag.add(new StringTag("", MessageTranslator.convertMessage(attributeComponent, language)));
            }

        }

        compoundTag.put(listTag);
        newNbt.put(compoundTag);
        return newNbt;
    }

    private static CompoundTag addAdvancedTooltips(CompoundTag nbt, Item item, String language) {
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
        int maxDurability = item.maxDamage();

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

        listTag.add(new StringTag("", ChatColor.RESET + ChatColor.DARK_GRAY + item.javaIdentifier()));
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
                String block = Identifier.formalize(((StringTag) canModifyJava.get(i)).getValue());
                // Get the Bedrock identifier of the item and replace it.
                // This will unfortunately be limited - for example, beds and banners will be translated weirdly
                canModifyBedrock[i] = BlockRegistries.JAVA_TO_BEDROCK_IDENTIFIERS.getOrDefault(block, block).replace("minecraft:", "");
            }
            return canModifyBedrock;
        }
        return null;
    }

    /**
     * Given an item stack, determine the Bedrock item definition that should be applied to Bedrock players.
     */
    @NonNull
    public static ItemDefinition getBedrockItemDefinition(GeyserSession session, @Nonnull GeyserItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemDefinition.AIR;
        }

        ItemMapping mapping = itemStack.asItem().toBedrockDefinition(itemStack.getNbt(), session.getItemMappings());

        ItemDefinition itemDefinition = mapping.getBedrockDefinition();
        CustomBlockData customBlockData = BlockRegistries.CUSTOM_BLOCK_ITEM_OVERRIDES.getOrDefault(
                mapping.getJavaItem().javaIdentifier(), null);
        if (customBlockData != null) {
            itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
        }

        if (mapping.getJavaItem().equals(Items.PLAYER_HEAD)) {
            CustomSkull customSkull = getCustomSkull(session, itemStack.getNbt());
            if (customSkull != null) {
                itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customSkull.getCustomBlockData());
            }
        }

        ItemDefinition definition = CustomItemTranslator.getCustomItem(itemStack.getNbt(), mapping);
        if (definition == null) {
            // No custom item
            return itemDefinition;
        } else {
            return definition;
        }
    }

    public static NbtMap translateNbtToBedrock(CompoundTag tag) {
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

    private static Object translateToBedrockNBT(Tag tag) {
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
            //return new org.cloudburstmc.nbt.tag.LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
            return null;
        }

        return tag.getValue();
    }

    public static CompoundTag translateToJavaNBT(String name, NbtMap tag) {
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

    private static Tag translateToJavaNBT(String name, Object object) {
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
            display.put(new StringTag("Name", ChatColor.RESET + ChatColor.ESCAPE + translationColor + MinecraftLocale.getLocaleString(translationKey, session.locale())));
        }

        return tag;
    }

    /**
     * Translates the custom model data of an item
     */
    public static void translateCustomItem(CompoundTag nbt, ItemData.Builder builder, ItemMapping mapping) {
        ItemDefinition definition = CustomItemTranslator.getCustomItem(nbt, mapping);
        if (definition != null) {
            builder.definition(definition);
            builder.blockDefinition(null);
        }
    }

    /**
     * Translates a custom block override
     */
    private static void translateCustomBlock(CustomBlockData customBlockData, GeyserSession session, ItemData.Builder builder) {
        ItemDefinition itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
        BlockDefinition blockDefinition = session.getBlockMappings().getCustomBlockStateDefinitions().get(customBlockData.defaultBlockState());
        builder.definition(itemDefinition);
        builder.blockDefinition(blockDefinition);
    }

    private static CustomSkull getCustomSkull(GeyserSession session, CompoundTag nbt) {
        if (nbt != null && nbt.contains("SkullOwner")) {
            if (!(nbt.get("SkullOwner") instanceof CompoundTag skullOwner)) {
                // It's a username give up d:
                return null;
            }
            SkinManager.GameProfileData data = SkinManager.GameProfileData.from(skullOwner);
            if (data == null) {
                session.getGeyser().getLogger().debug("Not sure how to handle skull head item display. " + nbt);
                return null;
            }

            String skinHash = data.skinUrl().substring(data.skinUrl().lastIndexOf('/') + 1);
            return BlockRegistries.CUSTOM_SKULLS.get(skinHash);
        }
        return null;
    }

    private static void translatePlayerHead(GeyserSession session, CompoundTag nbt, ItemData.Builder builder) {
        CustomSkull customSkull = getCustomSkull(session, nbt);
        if (customSkull != null) {
            CustomBlockData customBlockData = customSkull.getCustomBlockData();
            ItemDefinition itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
            BlockDefinition blockDefinition = session.getBlockMappings().getCustomBlockStateDefinitions().get(customBlockData.defaultBlockState());
            builder.definition(itemDefinition);
            builder.blockDefinition(blockDefinition);
        }
    }
}
