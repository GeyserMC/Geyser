/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.tooltip.providers;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.item.tooltip.ComponentTooltipProvider;
import org.geysermc.geyser.item.tooltip.TooltipContext;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AttributeModifiersTooltip implements ComponentTooltipProvider<ItemAttributeModifiers> {
    private static final EnumMap<ItemAttributeModifiers.EquipmentSlotGroup, String> SLOT_NAMES = new EnumMap<>(ItemAttributeModifiers.EquipmentSlotGroup.class);

    private static final DecimalFormat ATTRIBUTE_FORMAT = new DecimalFormat("0.#####");
    private static final Key BASE_ATTACK_DAMAGE_ID = MinecraftKey.key("base_attack_damage");
    private static final Key BASE_ATTACK_SPEED_ID = MinecraftKey.key("base_attack_speed");

    static {
        // Maps slot groups to their respective translation names, ordered in their Java edition order in the item tooltip
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

    @Override
    public void addTooltip(TooltipContext context, Consumer<Component> adder, @NonNull ItemAttributeModifiers modifiers) {
        // maps each slot to the modifiers applied when in such slot
        Map<ItemAttributeModifiers.EquipmentSlotGroup, List<Component>> slotsToModifiers = new HashMap<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.getModifiers()) {
            // convert the modifier tag to a lore entry
            Component loreEntry = attributeToLore(context, entry.getAttribute(), entry.getModifier(), entry.getDisplay());
            if (loreEntry == null) {
                continue; // invalid, failed, or hidden
            }

            slotsToModifiers.computeIfAbsent(entry.getSlot(), s -> new ArrayList<>()).add(loreEntry);
        }

        // iterate through the small array, not the map, so that ordering matches Java Edition
        for (ItemAttributeModifiers.EquipmentSlotGroup slot : SLOT_NAMES.keySet()) {
            List<Component> modifierTooltips = slotsToModifiers.get(slot);
            if (modifierTooltips == null || modifierTooltips.isEmpty()) {
                continue;
            }

            // Declare the slot, e.g. "When in Main Hand"
            adder.accept(Component.empty());
            adder.accept(Component.text()
                .append(Component.translatable("item.modifiers." + SLOT_NAMES.get(slot)))
                .color(NamedTextColor.GRAY)
                .build());

            // Then list all the modifiers when used in this slot
            for (Component modifier : modifierTooltips) {
                adder.accept(modifier);
            }
        }
    }

    @Nullable
    private static Component attributeToLore(TooltipContext context, int attribute, ItemAttributeModifiers.AttributeModifier modifier,
                                             ItemAttributeModifiers.Display display) {
        if (display.getType() == ItemAttributeModifiers.DisplayType.HIDDEN) {
            return null;
        } else if (display.getType() == ItemAttributeModifiers.DisplayType.OVERRIDE) {
            return display.getComponent();
        }

        double amount = modifier.getAmount();
        if (amount == 0) {
            return null;
        }

        String name = AttributeType.Builtin.from(attribute).getIdentifier().asMinimalString();
        // the namespace does not need to be present, but if it is, the java client ignores it as of pre-1.20.5

        ModifierOperation operation = modifier.getOperation();
        boolean baseModifier = false;
        String operationTotal = switch (operation) {
            case ADD -> {
                if (name.equals("knockback_resistance")) {
                    amount *= 10.0;
                }

                if (modifier.getId().equals(BASE_ATTACK_DAMAGE_ID)) {
                    amount += context.session().map(session -> session.getPlayerEntity().attributeOrDefault(GeyserAttributeType.ATTACK_DAMAGE)).orElse(1.0F);
                    baseModifier = true;
                } else if (modifier.getId().equals(BASE_ATTACK_SPEED_ID)) {
                    amount += context.session().map(session -> session.getPlayerEntity().attributeOrDefault(GeyserAttributeType.ATTACK_SPEED)).orElse(4.0F);
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


        return Component.text()
            .color(baseModifier ? NamedTextColor.DARK_GREEN : amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED)
            .append(Component.text(operationTotal + " "), Component.translatable("attribute.name." + name))
            .build();
    }
}
