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
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.java.ItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.Repairable;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.custom.ComponentConverters;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public record CustomItemContext(CustomItemDefinition definition, DataComponents components, List<ResolvableComponent<?>> resolvableComponents,
                                Optional<GeyserMappingItem> vanillaMapping, int customItemId, int protocolVersion) {

    public static CustomItemContext createVanilla(Item javaItem, GeyserMappingItem vanillaMapping, CustomItemDefinition customItem,
                                                  int customItemId, int protocolVersion) throws InvalidItemComponentsException {
        return new CustomItemContext(customItem, checkComponents(customItem, javaItem, resolvable -> {}), List.of(), Optional.of(vanillaMapping), customItemId, protocolVersion);
    }

    public static CustomItemContext createNonVanilla(NonVanillaCustomItemDefinition customItem, int customItemId, int protocolVersion) throws InvalidItemComponentsException {
        List<ResolvableComponent<?>> resolvableComponents = new ArrayList<>();
        DataComponents components = checkComponents(customItem, null, resolvableComponents::add);
        return new CustomItemContext(customItem, components, resolvableComponents, Optional.empty(), customItemId, protocolVersion);
    }

    /**
     * Check for illegal combinations of item components that can be specified in the custom item API, and validated components that can't be checked in the API, e.g. components that reference items.
     *
     * <p>Note that, component validation is preferred to occur early in the API module. This method should primarily check for illegal <em>combinations</em> of item components.
     * It is expected that the values of the components separately have already been validated when possible (for example, it is expected that stack size is in the range [1, 99]).</p>
     *
     * @param javaItem the vanilla item to patch components on to, can be null for non-vanilla custom items
     * @return the custom data components patched on the vanilla item's components (if present), validated
     */
    private static DataComponents checkComponents(CustomItemDefinition definition, Item javaItem, Consumer<ResolvableComponent<?>> resolvableConsumer) throws InvalidItemComponentsException {
        DataComponents components = patchDataComponents(javaItem, definition, resolvableConsumer);
        int stackSize = components.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 0);
        int maxDamage = components.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);

        if (components.get(DataComponentTypes.EQUIPPABLE) != null && stackSize > 1) {
            throw new InvalidItemComponentsException("Bedrock doesn't support equippable items with a stack size above 1");
        } else if (stackSize > 1 && maxDamage > 0) {
            throw new InvalidItemComponentsException("Stack size must be 1 when max damage is above 0");
        }

        // TODO
        Repairable repairable = definition.components().get(ItemDataComponents.REPAIRABLE);
        if (repairable != null) {
            for (Identifier item : repairable.items()) {
                if (Registries.JAVA_ITEM_IDENTIFIERS.get(item.toString()) == null) {
                    throw new InvalidItemComponentsException("Unknown repair item " + item + " in minecraft:repairable component");
                }
            }
        }

        return components;
    }

    /**
     * Converts the API components to MCPL ones using the converters in {@link ComponentConverters}, and applies these on top of the default item components.
     *
     * <p>Note that not every API component has a converter in {@link ComponentConverters}. See the documentation there.</p>
     *
     * @param javaItem can be null for non-vanilla custom items
     * @see ComponentConverters
     */
    private static DataComponents patchDataComponents(@Nullable Item javaItem, CustomItemDefinition definition, Consumer<ResolvableComponent<?>> resolvableConsumer) {
        DataComponents convertedComponents = ComponentConverters.convertComponentPatch(definition.components(), definition.removedComponents());
        if (javaItem != null) {
            // session can be null here because javaItem will always be a vanilla item
            return javaItem.gatherComponents(null, convertedComponents);
        }
        return convertedComponents;
    }
}
