/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinition;
import org.geysermc.geyser.api.biome.custom.CustomBiomeDefinitionRegisterException;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * Called on Geyser's startup when looking for custom biomes. Custom biomes must be
 * registered through this event. They are most useful for Java biomes that have no
 * Bedrock equivalent, such as biomes added by datapacks or mods, but vanilla Java
 * biomes can be overridden as well.
 *
 * <p>A registered definition is only used on sessions where the Java server has the
 * Java biome in its registry.</p>
 *
 * <p>This event will not be called if the "enable-custom-content" setting is disabled
 * in the Geyser config.</p>
 *
 * @since 2.11.1
 */
@ApiStatus.NonExtendable
public interface GeyserDefineCustomBiomesEvent extends Event {

    /**
     * A map of all the already registered custom biome definitions, indexed by the
     * identifier of the Java biome they were registered for.
     *
     * @return an unmodifiable map of the registered definitions
     * @since 2.11.1
     */
    Map<Identifier, CustomBiomeDefinition> customBiomeDefinitions();

    /**
     * Registers a custom biome definition for a Java biome; the definition must come from
     * {@link CustomBiomeDefinition#builder(Identifier)}. Registering is only possible
     * while this event is being fired. Every registration needs its own Java biome and
     * its own Bedrock identifier; reusing either will throw an exception.
     *
     * @param javaIdentifier the identifier of the Java biome to register the definition for
     * @param definition the custom biome definition to register
     * @throws CustomBiomeDefinitionRegisterException when an error occurred while registering the biome
     * @since 2.11.1
     */
    void register(Identifier javaIdentifier, CustomBiomeDefinition definition);
}
