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

package org.geysermc.geyser.api.event.lifecycle;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinitionRegisterException;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Called on Geyser's startup when looking for custom items. Custom items must be registered through this event.
 * <p>
 * This event will not be called if the "add-non-bedrock-items" setting is disabled in the Geyser config.
 */
@ApiStatus.NonExtendable
public interface GeyserDefineCustomItemsEvent extends Event {

    /**
     * A multimap of all the already registered custom items indexed by the item's extended java item's identifier.
     * The map returned here will only contain items registered with the deprecated
     * {@link GeyserDefineCustomItemsEvent#register(String, CustomItemData)} method.
     *
     * @deprecated replaced by {@link GeyserDefineCustomItemsEvent#customItemDefinitions()}
     */
    @Deprecated
    @NonNull
    Map<String, Collection<CustomItemData>> getExistingCustomItems();

    /**
     * A multimap of all the already registered custom item definitions
     * indexed by the {@link Identifier} of the Java item which the item is based on.
     * @since 2.9.3
     */
    @NonNull
    Map<Identifier, Collection<CustomItemDefinition>> customItemDefinitions();

    /**
     * A list of the already registered non-vanilla custom items.
     * The map returned here will only contain items registered with the deprecated
     * {@link GeyserDefineCustomItemsEvent#register(NonVanillaCustomItemData)} method.
     *
     * @deprecated replaced by {@link GeyserDefineCustomItemsEvent#nonVanillaCustomItemDefinitions()}
     */
    @Deprecated
    @NonNull
    List<NonVanillaCustomItemData> getExistingNonVanillaCustomItems();

    /**
     * A multimap of all the already registered non-vanilla custom item definitions indexed by the non-vanilla Java item {@link Identifier} these are mapped to.
     *
     * <p>This multimap will, at the moment, always have one entry per key.</p>
     * @since 2.9.3
     */
    @NonNull
    Map<Identifier, Collection<NonVanillaCustomItemDefinition>> nonVanillaCustomItemDefinitions();

    /**
     * Registers a custom item with a base Java item. This is used to register items with custom textures and properties
     * based on NBT data. This method should not be used anymore, {@link CustomItemDefinition}s are preferred now and this method will convert {@link CustomItemData} to {@link CustomItemDefinition} internally.
     *
     * @param identifier the base (java) item
     * @param customItemData the custom item data to register
     * @return if the item was registered
     * @deprecated use {@link GeyserDefineCustomItemsEvent#register(Identifier, CustomItemDefinition)}
     */
    @Deprecated
    boolean register(@NonNull String identifier, @NonNull CustomItemData customItemData);

    /**
     * Registers a Bedrock custom item definition based on a Java item. This is used to register items with custom textures and properties
     * created using item data component patches.
     *
     * @param identifier of the Java edition base item
     * @param customItemDefinition the custom item definition to register
     * @throws CustomItemDefinitionRegisterException when an error occurred while registering the item
     * @since 2.9.3
     */
    void register(@NonNull Identifier identifier, @NonNull CustomItemDefinition customItemDefinition);

    /**
     * Registers a custom item with no base item. This is used for mods.
     * This method should not be used anymore, {@link NonVanillaCustomItemDefinition}s are preferred now and this method will convert {@link NonVanillaCustomItemData} to {@link NonVanillaCustomItemDefinition} internally.
     *
     * @param customItemData the custom item data to register
     * @return if the item was registered
     * @deprecated use {@link GeyserDefineCustomItemsEvent#register(NonVanillaCustomItemDefinition)}
     */
    @Deprecated
    boolean register(@NonNull NonVanillaCustomItemData customItemData);

    /**
     * Registers a custom item with no base Java edition item. This is used for non-vanilla items added by mods.
     *
     * @param customItemDefinition the custom item definition to register
     * @throws CustomItemDefinitionRegisterException when an error occurred while registering the item
     * @since 2.9.3
     */
    void register(@NonNull NonVanillaCustomItemDefinition customItemDefinition);
}
