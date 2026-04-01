/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.api.pack.ResourcePack
import org.geysermc.geyser.api.pack.option.ResourcePackOption
import java.util.*

/**
 * Called when [ResourcePack]'s are loaded within Geyser.
 * @since 2.6.2
 */
abstract class GeyserDefineResourcePacksEvent : Event {
    /**
     * Gets the [ResourcePack]'s that will be sent to connecting Bedrock clients.
     * To remove packs, use [.unregister], as the list returned
     * by this method is unmodifiable.
     * 
     * @return an unmodifiable list of [ResourcePack]'s
     * @since 2.6.2
     */
    abstract fun resourcePacks(): MutableList<ResourcePack?>

    /**
     * Registers a [ResourcePack] to be sent to the client, optionally alongside
     * [ResourcePackOption]'s specifying how it will be applied on clients.
     * 
     * @param pack a resource pack that will be sent to the client
     * @param options [ResourcePackOption]'s that specify how clients load the pack
     * @throws ResourcePackException if an issue occurred during pack registration
     * @since 2.6.2
     */
    abstract fun register(pack: ResourcePack, vararg options: ResourcePackOption<*>?)

    /**
     * Sets [ResourcePackOption]'s for a [ResourcePack].
     * 
     * @param uuid the uuid of the resource pack to register the options for
     * @param options the [ResourcePackOption]'s to register for the resource pack
     * @throws ResourcePackException if an issue occurred during [ResourcePackOption] registration
     * @since 2.6.2
     */
    abstract fun registerOptions(uuid: UUID, vararg options: ResourcePackOption<*>)

    /**
     * Returns a collection of [ResourcePackOption]'s for a registered [ResourcePack].
     * The collection returned here is not modifiable.
     * 
     * @param uuid the uuid of the [ResourcePack] for which the options are set
     * @return a collection of [ResourcePackOption]'s
     * @throws ResourcePackException if the pack was not registered
     * @since 2.6.2
     */
    abstract fun options(uuid: UUID): MutableCollection<ResourcePackOption<*>?>?

    /**
     * Returns the current option, or null, for a given [ResourcePackOption.Type].
     * 
     * @param uuid the [ResourcePack] for which to query this option type
     * @param type the [ResourcePackOption.Type] of the option to query
     * @throws ResourcePackException if the queried option is invalid or not present on the resource pack
     * @since 2.6.2
     */
    abstract fun option(uuid: UUID, type: ResourcePackOption.Type): ResourcePackOption<*>?

    /**
     * Unregisters a [ResourcePack] from the list of packs sent to connecting Bedrock clients.
     * 
     * @param uuid the UUID of the [ResourcePack] to be removed
     * @since 2.6.2
     */
    abstract fun unregister(uuid: UUID)
}
