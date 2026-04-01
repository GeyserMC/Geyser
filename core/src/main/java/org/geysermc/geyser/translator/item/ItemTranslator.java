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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.item.type.PotionItem;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.CustomSkull;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.WrittenBookContent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ItemTranslator {

    
    private static final EnumMap<ItemAttributeModifiers.EquipmentSlotGroup, String> SLOT_NAMES;
    private static final ItemAttributeModifiers.EquipmentSlotGroup[] ARMOR_SLOT_NAMES = new ItemAttributeModifiers.EquipmentSlotGroup[] {
        ItemAttributeModifiers.EquipmentSlotGroup.HEAD,
        ItemAttributeModifiers.EquipmentSlotGroup.CHEST,
        ItemAttributeModifiers.EquipmentSlotGroup.LEGS,
        ItemAttributeModifiers.EquipmentSlotGroup.FEET
    };
    private static final DecimalFormat ATTRIBUTE_FORMAT = new DecimalFormat("0.#####");
    private static final Key BASE_ATTACK_DAMAGE_ID = MinecraftKey.key("base_attack_damage");
    private static final Key BASE_ATTACK_SPEED_ID = MinecraftKey.key("base_attack_speed");

    static {
        
        SLOT_NAMES = new EnumMap<>(ItemAttributeModifiers.EquipmentSlotGroup.class);
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.ANY, "any");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.MAIN_HAND, "mainhand");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.OFF_HAND, "offhand");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.HAND, "hand");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.FEET, "feet");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.LEGS, "legs");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.CHEST, "chest");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.HEAD, "head");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.ARMOR, "armor");
        SLOT_NAMES.put(ItemAttributeModifiers.EquipmentSlotGroup.BODY, "body");
    }

    private final static List<Item> GLINT_PRESENT = List.of(Items.ENCHANTED_GOLDEN_APPLE, Items.EXPERIENCE_BOTTLE, Items.WRITTEN_BOOK,
        Items.NETHER_STAR, Items.ENCHANTED_BOOK, Items.END_CRYSTAL);

    private ItemTranslator() {
    }

    public static ItemStack translateToJava(GeyserSession session, ItemData data) {
        if (data == null) {
            return new ItemStack(Items.AIR_ID);
        }

        ItemMapping bedrockItem = session.getItemMappings().getMapping(data);
        Item javaItem = bedrockItem.getJavaItem();

        GeyserItemStack itemStack = javaItem.translateToJava(session, data, bedrockItem, session.getItemMappings());

        NbtMap nbt = data.getTag();
        if (nbt != null && !nbt.isEmpty()) {
            
            DataComponents components = itemStack.getOrCreateComponents();
            javaItem.translateNbtToJava(session, nbt, components, bedrockItem);
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
        
        return translateToBedrock(session, Registries.JAVA_ITEMS.get().get(stack.getId()), bedrockItem, stack.getAmount(), stack.getDataComponentsPatch())
                .build();
    }

    @NonNull
    public static ItemData translateToBedrock(GeyserSession session, @NonNull GeyserItemStack stack) {
        if (stack.isEmpty()) {
            return ItemData.AIR;
        }

        ItemMapping bedrockItem = session.getItemMappings().getMapping(stack.getJavaId());
        if (bedrockItem == ItemMapping.AIR) {
            session.getGeyser().getLogger().debug("ItemMapping returned air: " + stack);
            return ItemData.AIR;
        }

        return translateToBedrock(session, stack.asItem(), bedrockItem, stack.getAmount(), stack.getComponents())
                .build();
    }

    public static ItemData.@NonNull Builder translateToBedrock(GeyserSession session, Item javaItem, ItemMapping bedrockItem, int count, @Nullable DataComponents customComponents) {
        BedrockItemBuilder nbtBuilder = new BedrockItemBuilder();

        
        DataComponents components = javaItem.gatherComponents(session.getComponentCache(), customComponents);
        TooltipOptions tooltip = TooltipOptions.fromComponents(components);

        
        javaItem.translateComponentsToBedrock(session, components, tooltip, nbtBuilder);

        Rarity rarity = Rarity.fromId(components.getOrDefault(DataComponentTypes.RARITY, 0));
        String customName = getCustomName(session, customComponents, bedrockItem, rarity.getColor(), false, false);
        if (customName != null) {
            PotionContents potionContents = components.get(DataComponentTypes.POTION_CONTENTS);
            
            if (potionContents != null && tooltip.showInTooltip(DataComponentTypes.POTION_CONTENTS)) {
                customName += getPotionEffectInfo(potionContents, session.locale()); 
            }

            nbtBuilder.setCustomName(customName);
        }

        ItemAttributeModifiers attributeModifiers = components.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributeModifiers != null && tooltip.showInTooltip(DataComponentTypes.ATTRIBUTE_MODIFIERS )) {
            
            addAttributeLore(session, attributeModifiers, nbtBuilder, session.locale());
        }

        if (session.isAdvancedTooltips() && !TooltipOptions.hideTooltip(components)) {
            addAdvancedTooltips(components, nbtBuilder, javaItem, session.locale());
        }

        
        if (components.getOrDefault(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false) && !GLINT_PRESENT.contains(javaItem)) {
            NbtMapBuilder nbtMapBuilder = nbtBuilder.getOrCreateNbt();
            nbtMapBuilder.putIfAbsent("ench", NbtList.EMPTY);
        }

        ItemData.Builder builder = javaItem.translateToBedrock(session, count, components, bedrockItem, session.getItemMappings());
        
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
            translatePlayerHead(session, components.get(DataComponentTypes.PROFILE), builder);
        }

        translateCustomItem(session, count, components, builder, bedrockItem);

        
        AdventureModePredicate canDestroy = components.get(DataComponentTypes.CAN_BREAK);
        AdventureModePredicate canPlaceOn = components.get(DataComponentTypes.CAN_PLACE_ON);
        String[] canBreak = getCanModify(session, canDestroy);
        String[] canPlace = getCanModify(session, canPlaceOn);
        if (canBreak != null) {
            builder.canBreak(canBreak);
        }
        if (canPlace != null) {
            builder.canPlace(canPlace);
        }

        return builder;
    }

    
    private static void addAttributeLore(GeyserSession session, ItemAttributeModifiers modifiers, BedrockItemBuilder builder, String language) {
        
        Map<ItemAttributeModifiers.EquipmentSlotGroup, List<String>> slotsToModifiers = new HashMap<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.getModifiers()) {
            
            String loreEntry = attributeToLore(session, entry.getAttribute(), entry.getModifier(), entry.getDisplay(), language);
            if (loreEntry == null) {
                continue; 
            }

            slotsToModifiers.computeIfAbsent(entry.getSlot(), s -> new ArrayList<>()).add(loreEntry);
        }

        
        for (var slot : SLOT_NAMES.keySet()) {
            List<String> modifierStrings = slotsToModifiers.get(slot);
            if (modifierStrings == null || modifierStrings.isEmpty()) {
                continue;
            }

            
            Component slotComponent = Component.text()
                    .resetStyle()
                    .color(NamedTextColor.GRAY)
                    .append(Component.newline(), Component.translatable("item.modifiers." + SLOT_NAMES.get(slot)))
                    .build();
            builder.getOrCreateLore().add(MessageTranslator.convertMessage(slotComponent, language));

            
            for (String modifier : modifierStrings) {
                builder.getOrCreateLore().add(modifier);
            }
        }
    }

    @Nullable
    private static String attributeToLore(GeyserSession session, int attribute, ItemAttributeModifiers.AttributeModifier modifier,
                                          ItemAttributeModifiers.Display display, String language) {
        if (display.getType() == ItemAttributeModifiers.DisplayType.HIDDEN) {
            return null;
        } else if (display.getType() == ItemAttributeModifiers.DisplayType.OVERRIDE) {
            return MessageTranslator.convertMessage(Objects.requireNonNull(display.getComponent())
                .colorIfAbsent(NamedTextColor.WHITE)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE), language);
        }

        double amount = modifier.getAmount();
        if (amount == 0) {
            return null;
        }

        String name = AttributeType.Builtin.from(attribute).getIdentifier().asMinimalString();
        

        ModifierOperation operation = modifier.getOperation();
        boolean baseModifier = false;
        String operationTotal = switch (operation) {
            case ADD -> {
                if (name.equals("knockback_resistance")) {
                    amount *= 10;
                }

                if (modifier.getId().equals(BASE_ATTACK_DAMAGE_ID)) {
                    amount += session.getPlayerEntity().attributeOrDefault(GeyserAttributeType.ATTACK_DAMAGE);
                    baseModifier = true;
                } else if (modifier.getId().equals(BASE_ATTACK_SPEED_ID)) {
                    amount += session.getAttackSpeed();
                    baseModifier = true;
                }

                yield ATTRIBUTE_FORMAT.format(amount);
            }
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL ->
                    ATTRIBUTE_FORMAT.format(amount * 100) + "%";
        };
        if (amount > 0 && !baseModifier) {
            operationTotal = "+" + operationTotal;
        }


        Component attributeComponent = Component.text()
                .resetStyle()
                .color(baseModifier ? NamedTextColor.DARK_GREEN : amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED)
                .append(Component.text(operationTotal + " "), Component.translatable("attribute.name." + name))
                .build();

        return MessageTranslator.convertMessage(attributeComponent, language);
    }

    private static final List<Effect> negativeEffectList = List.of(
        Effect.SLOWNESS,
        Effect.MINING_FATIGUE,
        Effect.INSTANT_DAMAGE,
        Effect.NAUSEA,
        Effect.BLINDNESS,
        Effect.HUNGER,
        Effect.WEAKNESS,
        Effect.POISON,
        Effect.WITHER,
        Effect.LEVITATION,
        Effect.UNLUCK,
        Effect.DARKNESS,
        Effect.WIND_CHARGED,
        Effect.WEAVING,
        Effect.OOZING,
        Effect.INFESTED
    );

    public static String getPotionEffectInfo(PotionContents contents, String language) {
        StringBuilder finalText = new StringBuilder();
        List<MobEffectInstance> effectInstanceList = contents.getCustomEffects();
        for (MobEffectInstance effectInstance : effectInstanceList) {
            Effect effect = effectInstance.getEffect();
            MobEffectDetails details = effectInstance.getDetails();
            int amplifier = details.getAmplifier();
            int durations = details.getDuration();
            TranslatableComponent appendTranslatable = Component.translatable("effect.minecraft." + effect.toString().toLowerCase(Locale.ROOT));
            if (amplifier != 0) {
                appendTranslatable = Component.translatable("potion.withAmplifier",
                    appendTranslatable,
                    Component.translatable("potion.potency." + amplifier));
            }
            if (durations > 20) {
                int seconds = durations / 20;
                int secondsFormat = seconds % 60;
                int minutes = seconds / 60;
                int minutesFormat = minutes % 60;
                int hours = minutes / 60;
                String text = ((minutesFormat > 9) ? "" : "0") + minutesFormat + ":" + ((secondsFormat > 9) ? "" : "0") + secondsFormat;
                if (minutes >= 60) {
                    text = ((hours > 9) ? "" : "0") + hours + ":" + text;
                }
                appendTranslatable = Component.translatable("potion.withDuration",
                    appendTranslatable,
                    Component.text(text));
            } else if (durations == -1) {
                appendTranslatable = Component.translatable("potion.withDuration",
                    appendTranslatable,
                    Component.translatable("effect.duration.infinite"));
            }
            Component component = Component.text()
                .resetStyle()
                .color((negativeEffectList.contains(effect)) ? NamedTextColor.RED : NamedTextColor.BLUE)
                .append(appendTranslatable)
                .build();
            
            finalText.append('\n').append(MessageTranslator.convertMessage(component, language));
        }
        return finalText.toString();
    }

    public static String getPotionName(PotionContents contents, ItemMapping mapping, String language) {
        String customPotionName = contents.getCustomName();
        Potion potion = Potion.getByJavaId(contents.getPotionId());

        if (customPotionName != null) {
            
            return MessageTranslator.convertMessage(
                Component.translatable(mapping.getJavaItem().translationKey() + ".effect." + customPotionName),
                language);
        }
        if (!contents.getCustomEffects().isEmpty()) {
            
            String potionName = potion == null ? "empty" : potion.toString().toLowerCase(Locale.ROOT);
            return MessageTranslator.convertMessage(Component.translatable(mapping.getJavaItem().translationKey() + ".effect." + potionName), language);
        }
        return null;
    }

    private static void addAdvancedTooltips(@Nullable DataComponents components, BedrockItemBuilder builder, Item item, String language) {
        int maxDurability = item.defaultMaxDamage();

        if (maxDurability != 0 && components != null) {
            Integer durabilityComponent = components.get(DataComponentTypes.DAMAGE);
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

    
    
    
    
    private static String @Nullable [] getCanModify(GeyserSession session, @Nullable AdventureModePredicate canModifyJava) {
        if (canModifyJava == null) {
            return null;
        }
        List<AdventureModePredicate.BlockPredicate> predicates = canModifyJava.getPredicates();
        if (!predicates.isEmpty()) {
            List<String> canModifyBedrock = new ArrayList<>(); 
            for (int i = 0; i < predicates.size(); i++) {
                HolderSet holderSet = predicates.get(i).getBlocks();
                if (holderSet == null) {
                    continue;
                }
                int[] holders = holderSet.getHolders();
                if (holders == null) {
                    continue;
                }
                
                for (int blockId : holders) {
                    
                    
                    Block block = BlockRegistries.JAVA_BLOCKS.get(blockId);
                    if (block == null) {
                        continue;
                    }
                    String identifier = session.getBlockMappings().getJavaToBedrockIdentifiers().get(block.javaId());
                    if (identifier == null) {
                        canModifyBedrock.add(block.javaIdentifier().value());
                    } else {
                        canModifyBedrock.add(identifier);
                    }
                }
            }
            return canModifyBedrock.toArray(new String[0]);
        }
        return null;
    }

    
    @NonNull
    public static ItemDefinition getBedrockItemDefinition(GeyserSession session, @NonNull GeyserItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return ItemDefinition.AIR;
        }

        ItemMapping mapping = itemStack.asItem().toBedrockDefinition(itemStack.getAllComponents(), session.getItemMappings());

        ItemDefinition itemDefinition = mapping.getBedrockDefinition();
        CustomBlockData customBlockData = BlockRegistries.CUSTOM_BLOCK_ITEM_OVERRIDES.getOrDefault(
                mapping.getJavaItem().javaIdentifier(), null);
        if (customBlockData != null) {
            itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
        }

        if (mapping.getJavaItem().equals(Items.PLAYER_HEAD)) {
            CustomSkull customSkull = getCustomSkull(itemStack.getComponent(DataComponentTypes.PROFILE));
            if (customSkull != null) {
                itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customSkull.getCustomBlockData());
            }
        }

        ItemDefinition definition = CustomItemTranslator.getCustomItem(session, itemStack.getAmount(), itemStack.getAllComponents(), mapping);
        if (definition == null) {
            
            return itemDefinition;
        } else {
            return definition;
        }
    }

    
    public static String getCustomName(GeyserSession session, DataComponents components, ItemMapping mapping,
                                       char translationColor, boolean customNameOnly, boolean includeAll) {
        if (components != null) {
            
            if (TooltipOptions.hideTooltip(components)) {
                return ""; 
            }

            
            Component customName = components.get(DataComponentTypes.CUSTOM_NAME);
            if (customName != null) {
                return MessageTranslator.convertMessage(customName, session.locale());
            }

            if (!customNameOnly) {
                if (mapping.getJavaItem() instanceof PotionItem) {
                    PotionContents potionContents = components.get(DataComponentTypes.POTION_CONTENTS);
                    if (potionContents != null) {
                        String potionName = getPotionName(potionContents, mapping, session.locale()); 
                        if (potionName != null) {
                            return ChatColor.RESET + ChatColor.ESCAPE + translationColor + potionName;
                        }
                    }
                }

                if (includeAll) {
                    
                    WrittenBookContent bookContent = components.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                    if (bookContent != null) {
                        return ChatColor.RESET + ChatColor.ESCAPE + translationColor + bookContent.getTitle().getRaw();
                    }
                }

                customName = components.get(DataComponentTypes.ITEM_NAME);
                if (customName != null) {
                    
                    
                    return ChatColor.RESET + ChatColor.ESCAPE + translationColor + MessageTranslator.convertMessage(customName, session.locale());
                }
            }
        }

        if (mapping.hasTranslation()) {
            
            String translationKey = mapping.getTranslationString();
            
            return ChatColor.RESET + ChatColor.ESCAPE + translationColor + MinecraftLocale.getLocaleString(translationKey, session.locale());
        }
        
        return null;
    }

    
    public static void translateCustomItem(GeyserSession session, int stackSize, DataComponents components, ItemData.Builder builder, ItemMapping mapping) {
        ItemDefinition definition = CustomItemTranslator.getCustomItem(session, stackSize, components, mapping);
        if (definition != null) {
            builder.definition(definition);
            builder.blockDefinition(null);
        }
    }

    
    private static void translateCustomBlock(CustomBlockData customBlockData, GeyserSession session, ItemData.Builder builder) {
        ItemDefinition itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
        BlockDefinition blockDefinition = session.getBlockMappings().getCustomBlockStateDefinitions().get(customBlockData.defaultBlockState());
        builder.definition(itemDefinition);
        builder.blockDefinition(blockDefinition);
    }

    private static @Nullable CustomSkull getCustomSkull(@Nullable ResolvableProfile profile) {
        if (profile == null) {
            return null;
        }

        
        
        GameProfile resolved = SkinManager.resolveProfile(profile).getNow(null);
        if (resolved == null) {
            return null;
        }

        GameProfile.Texture skinTexture = SkinManager.getTextureDataFromProfile(resolved, GameProfile.TextureType.SKIN);
        if (skinTexture == null) {
            return null;
        }
        return BlockRegistries.CUSTOM_SKULLS.get(skinTexture.getHash());
    }

    private static void translatePlayerHead(GeyserSession session, ResolvableProfile profile, ItemData.Builder builder) {
        CustomSkull customSkull = getCustomSkull(profile);
        if (customSkull != null) {
            CustomBlockData customBlockData = customSkull.getCustomBlockData();
            ItemDefinition itemDefinition = session.getItemMappings().getCustomBlockItemDefinitions().get(customBlockData);
            BlockDefinition blockDefinition = session.getBlockMappings().getCustomBlockStateDefinitions().get(customBlockData.defaultBlockState());
            builder.definition(itemDefinition);
            builder.blockDefinition(blockDefinition);
        }
    }
}
