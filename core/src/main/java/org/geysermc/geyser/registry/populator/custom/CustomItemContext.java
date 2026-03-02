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
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.custom.ComponentConverters;
import org.geysermc.geyser.item.custom.GeyserCustomItemDefinition;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Contains various context properties regarding a custom item definition.
 *
 * @param definition the custom item definition itself
 * @param components the full component map of the custom item definition. For vanilla-item overrides, this is the component patch applied on top of the vanilla item's default components. For non-vanilla items, this is the default components that were set in the API.
 * @param resolvableComponents for non-vanilla custom items, any components that should be resolved once the session has finished the configuration phase
 * @param vanillaMapping for vanilla-item overrides, the vanilla bedrock mapping of the item
 * @param customItemId the bedrock ID of the item
 * @param protocolVersion the bedrock protocol version
 */
public record CustomItemContext(CustomItemDefinition definition, DataComponents components, List<ResolvableComponent<?>> resolvableComponents,
                                Optional<GeyserMappingItem> vanillaMapping, int customItemId, int protocolVersion) {

    /**
     * Creates a CustomItemContext for a vanilla-item override. This patches the component patch of the {@code CustomItemDefinition} onto the default components of the {@code javaItem}
     *
     * @param javaItem the vanilla Java item
     * @param vanillaMapping the mapping of the vanilla Java item
     * @param customItem the custom item definition
     * @param customItemId the bedrock ID of the item
     * @param protocolVersion the bedrock protocol version
     * @return the created context
     * @throws InvalidItemComponentsException when the custom item definition has an invalid combination of components in its component patch
     */
    public static CustomItemContext createVanillaAndValidateComponents(Item javaItem, GeyserMappingItem vanillaMapping, CustomItemDefinition customItem,
                                                                       int customItemId, int protocolVersion) throws InvalidItemComponentsException {
        return new CustomItemContext(customItem, checkComponents(customItem, javaItem, resolvable -> {}), List.of(), Optional.of(vanillaMapping), customItemId, protocolVersion);
    }

    /**
     * Creates a CustomItemContext for a non-vanilla custom item.
     *
     * @param customItem the non-vanilla custom item definition
     * @param customItemId the bedrock ID of the item
     * @param protocolVersion the bedrock protocol version
     * @return the created context
     * @throws InvalidItemComponentsException when the custom item definition has an invalid combination of components in its component patch
     */
    public static CustomItemContext createNonVanillaAndValidateComponents(NonVanillaCustomItemDefinition customItem, int customItemId, int protocolVersion) throws InvalidItemComponentsException {
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
            // V1 compat: Allow old items to function; we'll patch in stack size to one later
            if (!(definition instanceof GeyserCustomItemDefinition)) {
                throw new InvalidItemComponentsException("Bedrock doesn't support equippable items with a stack size above 1");
            }
        } else if (stackSize > 1 && maxDamage > 0) {
            throw new InvalidItemComponentsException("Stack size must be 1 when max damage is above 0");
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
    private static DataComponents patchDataComponents(@Nullable Item javaItem, CustomItemDefinition definition, Consumer<ResolvableComponent<?>> resolvableConsumer) throws InvalidItemComponentsException {
        DataComponents convertedComponents = ComponentConverters.convertComponentPatch(definition.components(), definition.removedComponents(), resolvableConsumer);
        if (javaItem != null) {
            // componentCache can be null here because javaItem will always be a vanilla item
            return javaItem.gatherComponents(null, convertedComponents);
        }
        return convertedComponents;
    }
}
