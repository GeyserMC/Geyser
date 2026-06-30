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

package org.geysermc.geyser.api.entity.definition;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Represents a Bedrock entity definition registered in the {@link GeyserDefineEntitiesEvent}.
 * @since 2.11.0
 */
@ApiStatus.Experimental
public interface GeyserEntityDefinition {

    /**
     * This entity's identifier as it's known to Bedrock clients and used in resource packs.
     * See <a href="https://learn.microsoft.com/en-us/minecraft/creator/documents/introductiontoaddentity?view=minecraft-bedrock-stable#naming">the official docs for further information</a>
     *
     * @return the Bedrock entity identifier
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Identifier identifier();

    /**
     * Returns the entity properties registered in the {@link GeyserDefineEntityPropertiesEvent} for this entity type.
     *
     * @see GeyserEntityProperty
     * @return an unmodifiable list containing entity properties registered for this entity type
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    List<GeyserEntityProperty<?>> properties();

    /**
     * Indicates whether this entity exists in Minecraft: Bedrock Edition, or whether it is a custom entity.
     *
     * @return whether this entity exists in the vanilla base game
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    boolean vanilla();

    /**
     * Returns whether this definition is currently registered via {@link GeyserDefineEntitiesEvent}.
     * Unregistered definitions cannot be sent to Bedrock clients!
     *
     * @return whether this definition is registered
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    boolean registered();

    /**
     * Retrieves a registered {@link GeyserEntityDefinition} by its Bedrock entity type identifier,
     * or returns a new unregistered definition if none is found.
     *
     * @param identifier the Bedrock entity identifier
     * @return the GeyserEntityDefinition for the given identifier
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    static GeyserEntityDefinition of(Identifier identifier) {
        return GeyserApi.api().provider(GeyserEntityDefinition.class, identifier);
    }
}
