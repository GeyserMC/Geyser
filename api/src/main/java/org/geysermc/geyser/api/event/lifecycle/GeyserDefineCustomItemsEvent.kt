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
package org.geysermc.geyser.api.event.lifecycle

import org.geysermc.event.Event
import org.geysermc.geyser.api.util.Identifier
import org.jetbrains.annotations.ApiStatus

/**
 * Called on Geyser's startup when looking for custom items. Custom items must be registered through this event.
 * 
 * 
 * This event will not be called if the "add-non-bedrock-items" setting is disabled in the Geyser config.
 */
@ApiStatus.NonExtendable
interface GeyserDefineCustomItemsEvent : Event {
    @get:Deprecated("replaced by {@link GeyserDefineCustomItemsEvent#customItemDefinitions()}")
    val existingCustomItems: MutableMap<String?, MutableCollection<CustomItemData>?>

    /**
     * A multimap of all the already registered custom item definitions
     * indexed by the [Identifier] of the Java item which the item is based on.
     * @since 2.9.3
     */
    fun customItemDefinitions(): MutableMap<Identifier?, MutableCollection<CustomItemDefinition?>?>

    @get:Deprecated("replaced by {@link GeyserDefineCustomItemsEvent#nonVanillaCustomItemDefinitions()}")
    val existingNonVanillaCustomItems: MutableList<NonVanillaCustomItemData>

    /**
     * A multimap of all the already registered non-vanilla custom item definitions indexed by the non-vanilla Java item [Identifier] these are mapped to.
     * 
     * 
     * This multimap will, at the moment, always have one entry per key.
     * @since 2.9.3
     */
    fun nonVanillaCustomItemDefinitions(): MutableMap<Identifier?, MutableCollection<NonVanillaCustomItemDefinition?>?>

    /**
     * Registers a custom item with a base Java item. This is used to register items with custom textures and properties
     * based on NBT data. This method should not be used anymore, [CustomItemDefinition]s are preferred now and this method will convert [CustomItemData] to [CustomItemDefinition] internally.
     * 
     * @param identifier the base (java) item
     * @param customItemData the custom item data to register
     * @return if the item was registered
     */
    @Deprecated("use {@link GeyserDefineCustomItemsEvent#register(Identifier, CustomItemDefinition)}")
    fun register(identifier: String, customItemData: CustomItemData): Boolean

    /**
     * Registers a Bedrock custom item definition based on a Java item. This is used to register items with custom textures and properties
     * created using item data component patches.
     * 
     * @param identifier of the Java edition base item
     * @param customItemDefinition the custom item definition to register
     * @throws CustomItemDefinitionRegisterException when an error occurred while registering the item
     * @since 2.9.3
     */
    fun register(identifier: Identifier, customItemDefinition: CustomItemDefinition)

    /**
     * Registers a custom item with no base item. This is used for mods.
     * This method should not be used anymore, [NonVanillaCustomItemDefinition]s are preferred now and this method will convert [NonVanillaCustomItemData] to [NonVanillaCustomItemDefinition] internally.
     * 
     * @param customItemData the custom item data to register
     * @return if the item was registered
     */
    @Deprecated("use {@link GeyserDefineCustomItemsEvent#register(NonVanillaCustomItemDefinition)}")
    fun register(customItemData: NonVanillaCustomItemData): Boolean

    /**
     * Registers a custom item with no base Java edition item. This is used for non-vanilla items added by mods.
     * 
     * @param customItemDefinition the custom item definition to register
     * @throws CustomItemDefinitionRegisterException when an error occurred while registering the item
     * @since 2.9.3
     */
    fun register(customItemDefinition: NonVanillaCustomItemDefinition)
}
