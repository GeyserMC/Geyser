/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.populator.custom.CustomItemContext;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record GeyserCustomItemBedrockOptions(@Nullable String icon, boolean allowOffhand, boolean displayHandheld, int protectionValue,
                                             @NonNull CreativeCategory creativeCategory, @Nullable String creativeGroup, @NonNull Set<Identifier> tags) implements CustomItemBedrockOptions {

    @Override
    public int protectionValue() {
        return protectionValue == -1 ? 0 : protectionValue;
    }

    public int protectionValue(CustomItemContext context) {
        if (protectionValue == -1 && context.vanillaMapping().isPresent()) {
            ItemAttributeModifiers attributeModifiers = context.components().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            Equippable equippable = context.components().get(DataComponentTypes.EQUIPPABLE);
            if (attributeModifiers != null && equippable != null) {
                ItemAttributeModifiers.EquipmentSlotGroup slotGroup = switch (equippable.slot()) {
                    case MAIN_HAND -> ItemAttributeModifiers.EquipmentSlotGroup.MAIN_HAND;
                    case OFF_HAND -> ItemAttributeModifiers.EquipmentSlotGroup.OFF_HAND;
                    case BOOTS -> ItemAttributeModifiers.EquipmentSlotGroup.FEET;
                    case LEGGINGS -> ItemAttributeModifiers.EquipmentSlotGroup.LEGS;
                    case CHESTPLATE -> ItemAttributeModifiers.EquipmentSlotGroup.CHEST;
                    case HELMET -> ItemAttributeModifiers.EquipmentSlotGroup.HEAD;
                    case BODY -> ItemAttributeModifiers.EquipmentSlotGroup.BODY;
                    case SADDLE -> ItemAttributeModifiers.EquipmentSlotGroup.SADDLE;
                };
                return (int) attributeModifiers.getModifiers().stream()
                    .filter(entry -> entry.getAttribute() == AttributeType.Builtin.ARMOR.getId()
                        // entry.slot == any || entry.slot == expected || (expected == hands && entry.slot == hand)
                        && (entry.getSlot() == ItemAttributeModifiers.EquipmentSlotGroup.ANY || entry.getSlot() == slotGroup
                            || ((slotGroup == ItemAttributeModifiers.EquipmentSlotGroup.MAIN_HAND || slotGroup == ItemAttributeModifiers.EquipmentSlotGroup.OFF_HAND)
                                && entry.getSlot() == ItemAttributeModifiers.EquipmentSlotGroup.HAND))
                        && entry.getModifier().getOperation() == ModifierOperation.ADD)
                    .mapToDouble(entry -> entry.getModifier().getAmount())
                    .sum();
            }
        }
        return protectionValue();
    }

    public static class Builder implements CustomItemBedrockOptions.Builder {
        private String icon = null;
        private boolean allowOffhand = true;
        private boolean displayHandheld = false;
        private int protectionValue = -1;
        private CreativeCategory creativeCategory = CreativeCategory.NONE;
        private String creativeGroup = null;
        private Set<Identifier> tags = new HashSet<>();

        @Override
        public Builder icon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }

        @Override
        public Builder allowOffhand(boolean allowOffhand) {
            this.allowOffhand = allowOffhand;
            return this;
        }

        @Override
        public Builder displayHandheld(boolean displayHandheld) {
            this.displayHandheld = displayHandheld;
            return this;
        }

        @Override
        public Builder protectionValue(int protectionValue) {
            this.protectionValue = protectionValue;
            return this;
        }

        @Override
        public Builder creativeCategory(@NotNull CreativeCategory creativeCategory) {
            Objects.requireNonNull(creativeCategory, "creativeCategory cannot be null");
            this.creativeCategory = creativeCategory;
            return this;
        }

        @Override
        public Builder creativeGroup(@Nullable String creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        @Override
        public CustomItemBedrockOptions.Builder tag(@NotNull Identifier tag) {
            Objects.requireNonNull(tag, "tag cannot be null");
            this.tags.add(tag);
            return this;
        }

        @Override
        public Builder tags(@Nullable Set<Identifier> tags) {
            this.tags = Objects.requireNonNullElseGet(tags, HashSet::new);
            return this;
        }

        @Override
        public CustomItemBedrockOptions build() {
            return new GeyserCustomItemBedrockOptions(icon, allowOffhand, displayHandheld, protectionValue,
                creativeCategory, creativeGroup, Set.copyOf(tags));
        }
    }
}
