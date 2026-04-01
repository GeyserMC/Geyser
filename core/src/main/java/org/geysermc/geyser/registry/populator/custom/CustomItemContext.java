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

package org.geysermc.geyser.registry.populator.custom;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.custom.ComponentConverters;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public record CustomItemContext(CustomItemDefinition definition, DataComponents components, List<ResolvableComponent<?>> resolvableComponents,
                                Optional<GeyserMappingItem> vanillaMapping, int customItemId, int protocolVersion) {

    
    public static CustomItemContext createVanillaAndValidateComponents(Item javaItem, GeyserMappingItem vanillaMapping, CustomItemDefinition customItem,
                                                                       int customItemId, int protocolVersion, boolean firstPass) throws InvalidItemComponentsException {
        return new CustomItemContext(customItem, checkComponents(customItem, javaItem, resolvable -> {}, firstPass), List.of(), Optional.of(vanillaMapping), customItemId, protocolVersion);
    }

    
    public static CustomItemContext createNonVanillaAndValidateComponents(NonVanillaCustomItemDefinition customItem, int customItemId, int protocolVersion, boolean firstPass) throws InvalidItemComponentsException {
        List<ResolvableComponent<?>> resolvableComponents = new ArrayList<>();
        DataComponents components = checkComponents(customItem, null, resolvableComponents::add, firstPass);
        return new CustomItemContext(customItem, components, resolvableComponents, Optional.empty(), customItemId, protocolVersion);
    }

    
    private static DataComponents checkComponents(CustomItemDefinition definition, Item javaItem, Consumer<ResolvableComponent<?>> resolvableConsumer, boolean firstPass) throws InvalidItemComponentsException {
        DataComponents components = patchDataComponents(javaItem, definition, resolvableConsumer);
        int stackSize = components.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 0);
        int maxDamage = components.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
        Equippable equippable = components.get(DataComponentTypes.EQUIPPABLE);

        if (equippable != null && stackSize > 1 && firstPass) {
            GeyserImpl.getInstance().getLogger().warning("Bedrock doesn't support stackable equippable items! Custom item %s with stack size %s and equippable component for slot %s will not work as expected!"
                .formatted(definition.bedrockIdentifier(), stackSize, equippable.slot()));
        } else if (stackSize > 1 && maxDamage > 0) {
            throw new InvalidItemComponentsException("Stack size must be 1 when max damage is above 0");
        }

        return components;
    }

    
    private static DataComponents patchDataComponents(@Nullable Item javaItem, CustomItemDefinition definition, Consumer<ResolvableComponent<?>> resolvableConsumer) throws InvalidItemComponentsException {
        DataComponents convertedComponents = ComponentConverters.convertComponentPatch(definition.components(), definition.removedComponents(), resolvableConsumer);
        if (javaItem != null) {
            
            return javaItem.gatherComponents(null, convertedComponents);
        }
        return convertedComponents;
    }
}
