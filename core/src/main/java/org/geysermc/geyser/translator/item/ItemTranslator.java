/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.item;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.data.GameProfile.Texture;
import com.github.steveice10.mc.auth.data.GameProfile.TextureType;
import com.github.steveice10.mc.auth.exception.property.PropertyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.Identifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemTranslator {

    /**
     * The order of these slots is their display order on Java Edition clients
     */
    private static final EnumMap<ItemAttributeModifiers.EquipmentSlotGroup, String> SLOT_NAMES;
    private static final DecimalFormat ATTRIBUTE_FORMAT = new DecimalFormat("0.#####");

    static {
        // These are the only slots that are used and have translation strings
        SLOT_NAMES = new EnumMap<>(ItemAttributeModifiers.EquipmentSlotGroup.class);
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.MAIN_HAND, "mainhand");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.OFF_HAND, "offhand");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.FEET, "feet");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.LEGS, "legs");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.CHEST, "chest");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.HEAD, "head");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.BODY, "body");
    }

    private ItemTranslator() {
    }

    /**
     * @param mappings item mappings to use while translating. This can't just be a Geyser session as this method is used
     *                 when loading recipes.
     */
    public static ItemStack translateToJava(ItemData data, ItemMappings mappings) {
        if (data == null) {
            return new ItemStack(Items.AIR_ID);
        }

        ItemMapping bedrockItem = mappings.getMapping(data);
        Item javaItem = bedrockItem.getJavaItem();

        GeyserItemStack itemStack = javaItem.translateToJava(data, bedrockItem, mappings);

        NbtMap nbt = data.getTag();
        if (nbt != null && !nbt.isEmpty()) {
            // translateToJava may have added components
            DataComponents components = itemStack.getComponents() == null ? new DataComponents(new HashMap<>()) : itemStack.getComponents();
            javaItem.translateNbtToJava(nbt, components, bedrockItem);
            if (!components.getDataComponents().isEmpty()) {
                itemStack.setComponents(components);
            }
        }
        return itemStack.getItemStack();
    }

    public static ItemData.@NonNull Builder translateToBedrock(GeyserSession session, int javaId, int count, DataComponents components) {
        ItemMapping bedrockItem = session.getItemMappings().getMapping(javaId);
        if (bedrockItem == ItemMapping.AIR) {
            session.getGeyser().getLogger().debug("ItemMapping returned air: " + javaId);
            return ItemData.builder();
        }
        return translateToBedrock(session, Registries.JAVA_ITEMS.get().get(javaId), bedrockItem, count, components);
    }

    @NonNull
    public static ItemData translateToBedrock(GeyserSession session, ItemStack stack) {
        if (InventoryUtils.isEmpty(stack)) {
            return ItemData.AIR;
        }

        ItemMapping bedrockItem = session.getItemMappings().getMapping(stack);
        if (bedrockItem == ItemMapping.AIR) {
            session.getGeyser().getLogger().debug("ItemMapping returned air: " + stack);
            return ItemData.AIR;
        }
        // Java item needs to be loaded separately. The mapping for tipped arrow would
        return translateToBedrock(session, Registries.JAVA_ITEMS.get().get(stack.getId()), bedrockItem, stack.getAmount(), stack.getDataComponents())
                .build();
    }

    private static ItemData.@NonNull Builder translateToBedrock(GeyserSession session, Item javaItem, ItemMapping bedrockItem, int count, @Nullable DataComponents components) {
        BedrockItemBuilder nbtBuilder = new BedrockItemBuilder();

        if (components != null) {
            javaItem.translateComponentsToBedrock(session, components, nbtBuilder);
        }

        String customName = getCustomName(session, components, bedrockItem);
        if (customName != null) {
            nbtBuilder.setCustomName(customName);
        }

        if (components != null) {
            ItemAttributeModifiers attributeModifiers = components.get(DataComponentType.ATTRIBUTE_MODIFIERS);
            if (attributeModifiers != null && attributeModifiers.isShowInTooltip()) {
                // only add if attribute modifiers do not indicate to hide them
                addAttributeLore(attributeModifiers, nbtBuilder, session.locale());
            }
        }

        if (session.isAdvancedTooltips()) {
            addAdvancedTooltips(components, nbtBuilder, javaItem, session.locale());
        }

        ItemData.Builder builder = javaItem.translateToBedrock(count, components, bedrockItem, session.getItemMappings());
        // Finalize the Bedrock NBT
        builder.tag(nbtBuilder.build());
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
            translatePlayerHead(session, components, builder);
        }

        translateCustomItem(components, builder, bedrockItem);

        if (components != null) {
            // Translate the canDestroy and canPlaceOn Java NBT
            AdventureModePredicate canDestroy = components.get(DataComponentType.CAN_BREAK);
            AdventureModePredicate canPlaceOn = components.get(DataComponentType.CAN_PLACE_ON);
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

    /**
     * Bedrock Edition does not see attribute modifiers like Java Edition does,
     * so we add them as lore instead.
     *
     * @param modifiers the attribute modifiers of the ItemStack
     * @param language the locale of the player
     */
    private static void addAttributeLore(ItemAttributeModifiers modifiers, BedrockItemBuilder builder, String language) {
        // maps each slot to the modifiers applied when in such slot
        Map<ItemAttributeModifiers.EquipmentSlotGroup, List<String>> slotsToModifiers = new HashMap<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.getModifiers()) {
            // convert the modifier tag to a lore entry
            String loreEntry = attributeToLore(entry.getModifier(), language);
            if (loreEntry == null) {
                continue; // invalid or failed
            }

            ItemAttributeModifiers.EquipmentSlotGroup slotGroup = entry.getSlot();
            if (slotGroup == ItemAttributeModifiers.EquipmentSlotGroup.ANY) {
                // modifier applies to all slots implicitly
                for (var slot : SLOT_NAMES.keySet()) {
                    slotsToModifiers.computeIfAbsent(slot, s -> new ArrayList<>()).add(loreEntry);
                }
            } else {
                // modifier applies to only the specified slot
                slotsToModifiers.computeIfAbsent(slotGroup, s -> new ArrayList<>()).add(loreEntry);
            }
        }

        // iterate through the small array, not the map, so that ordering matches Java Edition
        for (var slot : SLOT_NAMES.keySet()) {
            List<String> modifierStrings = slotsToModifiers.get(slot);
            if (modifierStrings == null || modifierStrings.isEmpty()) {
                continue;
            }

            // Declare the slot, e.g. "When in Main Hand"
            Component slotComponent = Component.text()
                    .resetStyle()
                    .color(NamedTextColor.GRAY)
                    .append(Component.newline(), Component.translatable("item.modifiers." + SLOT_NAMES.get(slot)))
                    .build();
            builder.getOrCreateLore().add(MessageTranslator.convertMessage(slotComponent, language));

            // Then list all the modifiers when used in this slot
            for (String modifier : modifierStrings) {
                builder.getOrCreateLore().add(modifier);
            }
        }
    }

    @Nullable
    private static String attributeToLore(ItemAttributeModifiers.AttributeModifier modifier, String language) {
        double amount = modifier.getAmount();
        if (amount == 0) {
            return null;
        }

        String name = modifier.getName().replace("minecraft:", "");
        // the namespace does not need to be present, but if it is, the java client ignores it as of pre-1.20.5

        ModifierOperation operation = modifier.getOperation();
        String operationTotal = switch (operation) {
            case ADD -> {
                if (name.equals("generic.knockback_resistance")) {
                    amount *= 10;
                }
                yield ATTRIBUTE_FORMAT.format(amount);
            }
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL ->
                    ATTRIBUTE_FORMAT.format(amount * 100) + "%";
        };
        if (amount > 0) {
            operationTotal = "+" + operationTotal;
        }

        Component attributeComponent = Component.text()
                .resetStyle()
                .color(amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED)
                .append(Component.text(operationTotal + " "), Component.translatable("attribute.name." + name))
                .build();

        return MessageTranslator.convertMessage(attributeComponent, language);
    }

    private static void addAdvancedTooltips(@Nullable DataComponents components, BedrockItemBuilder builder, Item item, String language) {
        int maxDurability = item.maxDamage();

        if (maxDurability != 0 && components != null) {
            Integer durabilityComponent = components.get(DataComponentType.DAMAGE);
            if (durabilityComponent != null) {
                int durability = maxDurability - durabilityComponent;
                if (durability != maxDurability) {
                    Component component = Component.text()
                            .resetStyle()
                            .color(NamedTextColor.WHITE)
                            .append(Component.translatable("item.durability",
                                    Component.text(durability),
                                    Component.text(maxDurability)))
                            .build();
                    builder.getOrCreateLore().add(MessageTranslator.convertMessage(component, language));
                }
            }
        }

        builder.getOrCreateLore().add(ChatColor.RESET + ChatColor.DARK_GRAY + item.javaIdentifier());
        if (components != null) {
            Component component = Component.text()
                    .resetStyle()
                    .color(NamedTextColor.DARK_GRAY)
                    .append(Component.translatable("item.components",
                            Component.text(components.getDataComponents().size())))
                    .build();
            builder.getOrCreateLore().add(MessageTranslator.convertMessage(component, language));
        }
    }

    /**
     * Translates the Java NBT of canDestroy and canPlaceOn to its Bedrock counterparts.
     * In Java, this is treated as normal NBT, but in Bedrock, these arguments are extra parts of the item data itself.
     *
     * @param canModifyJava the list of items in Java
     * @return the new list of items in Bedrock
     */
    // TODO this is now more complicated in 1.20.5. Yippee!
    private static String @Nullable [] getCanModify(@Nullable AdventureModePredicate canModifyJava) {
        if (canModifyJava == null) {
            return null;
        }
        List<AdventureModePredicate.BlockPredicate> predicates = canModifyJava.getPredicates();
        if (predicates.size() > 0) {
            String[] canModifyBedrock = new String[predicates.size()];
            for (int i = 0; i < canModifyBedrock.length; i++) {
                // Get the Java identifier of the block that can be placed
                String location = predicates.get(i).getLocation();
                if (location == null) {
                    canModifyBedrock[i] = ""; // So it'll serialize
                    continue; // ???
                }
                String block = Identifier.formalize(location);
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
    public static ItemDefinition getBedrockItemDefinition(GeyserSession session, @NonNull GeyserItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemDefinition.AIR;
        }

        ItemMapping mapping = itemStack.asItem().toBedrockDefinition(itemStack.getComponents(), session.getItemMappings());

        ItemDefinition itemDefinition = mapping.getBedrockDefinition();
        CustomBlockData customBlockData = BlockRegistries.CUSTOM_BLOCK_ITEM_OVERRIDES.getOrDefault(
                mapping.getJavaItem().javaIdentifier(), null);
        if (customBlockData != null) {
            itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
        }

        if (mapping.getJavaItem().equals(Items.PLAYER_HEAD)) {
            CustomSkull customSkull = getCustomSkull(itemStack.getComponents());
            if (customSkull != null) {
                itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customSkull.getCustomBlockData());
            }
        }

        ItemDefinition definition = CustomItemTranslator.getCustomItem(itemStack.getComponents(), mapping);
        if (definition == null) {
            // No custom item
            return itemDefinition;
        } else {
            return definition;
        }
    }

    /**
     * Translates the display name of the item
     * @param session the Bedrock client's session
     * @param components the components to translate
     * @param mapping the item entry, in case it requires translation
     */
    public static String getCustomName(GeyserSession session, DataComponents components, ItemMapping mapping) {
        return getCustomName(session, components, mapping, 'f');
    }

    /**
     * @param translationColor if this item is not available on Java, the color that the new name should be.
     *                         Normally, this should just be white, but for shulker boxes this should be gray.
     */
    public static String getCustomName(GeyserSession session, DataComponents components, ItemMapping mapping, char translationColor) {
        if (components != null) {
            // ItemStack#getHoverName as of 1.20.5
            Component customName = components.get(DataComponentType.CUSTOM_NAME);
            if (customName == null) {
                customName = components.get(DataComponentType.ITEM_NAME);
            }
            if (customName != null) {
                // Get the translated name and prefix it with a reset char
                return MessageTranslator.convertMessage(customName, session.locale());
            }
        }

        if (mapping.hasTranslation()) {
            // No custom name, but we need to localize the item's name
            String translationKey = mapping.getTranslationString();
            // Reset formatting since Bedrock defaults to italics
            return ChatColor.RESET + ChatColor.ESCAPE + translationColor + MinecraftLocale.getLocaleString(translationKey, session.locale());
        }
        // No custom name
        return null;
    }

    /**
     * Translates the custom model data of an item
     */
    public static void translateCustomItem(DataComponents components, ItemData.Builder builder, ItemMapping mapping) {
        ItemDefinition definition = CustomItemTranslator.getCustomItem(components, mapping);
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

    private static @Nullable CustomSkull getCustomSkull(DataComponents components) {
        if (components == null) {
            return null;
        }
        
        GameProfile profile = components.get(DataComponentType.PROFILE);
        if (profile != null) {
            Map<TextureType, Texture> textures = null;
            try {
                textures = profile.getTextures(false);
            } catch (PropertyException e) {
                GeyserImpl.getInstance().getLogger().debug("Failed to get textures from GameProfile: " + e);
            }

            if (textures == null || textures.isEmpty()) {
                return null;
            }

            Texture skinTexture = textures.get(TextureType.SKIN);

            if (skinTexture == null) {
                return null;
            }

            String skinHash = skinTexture.getURL().substring(skinTexture.getURL().lastIndexOf('/') + 1);
            return BlockRegistries.CUSTOM_SKULLS.get(skinHash);
        }
        return null;
    }

    private static void translatePlayerHead(GeyserSession session, DataComponents components, ItemData.Builder builder) {
        CustomSkull customSkull = getCustomSkull(components);
        if (customSkull != null) {
            CustomBlockData customBlockData = customSkull.getCustomBlockData();
            ItemDefinition itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
            BlockDefinition blockDefinition = session.getBlockMappings().getCustomBlockStateDefinitions().get(customBlockData.defaultBlockState());
            builder.definition(itemDefinition);
            builder.blockDefinition(blockDefinition);
        }
    }
}
