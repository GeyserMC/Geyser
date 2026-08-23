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

package org.geysermc.geyser.api.event.lifecycle;

import org.geysermc.event.Event;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

/**
 * Called once during Geyser startup to allow users to register custom Bedrock entity types.
 * <p>
 * All {@link CustomEntityDefinition}'s must be registered in this event before being used!
 * <p>
 * To register a custom entity, create a definition with {@link CustomEntityDefinition#of(Identifier)}
 * and pass it to {@link #register(CustomEntityDefinition)}. Additional Bedrock entity properties
 * can be registered in the subsequent {@link GeyserDefineEntityPropertiesEvent}.
 *
 * @since 2.11.0
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface GeyserDefineEntitiesEvent extends Event {

    /**
     * Returns an immutable view of all currently registered Bedrock entity definitions,
     * including both vanilla and any custom definitions registered so far in this event.
     *
     * @return an immutable collection of all registered entity definitions
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Collection<GeyserEntityDefinition> entities();

    /**
     * Returns an immutable view of all custom entity definitions registered so far.
     *
     * @return an immutable collection of registered custom entity definitions
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Collection<CustomEntityDefinition> customEntities();

    /**
     * Registers a custom entity definition. Using the same identifier twice in different definitions
     * or providing a definition in the vanilla namespace will throw an exception.
     *
     * @param definition the custom entity definition to register; must not be null
     * @throws IllegalArgumentException if {@code definition} was not created via {@link CustomEntityDefinition#of}
     * @throws IllegalStateException if a custom entity definition with this identifier has already been registered
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    void register(CustomEntityDefinition definition);
}
