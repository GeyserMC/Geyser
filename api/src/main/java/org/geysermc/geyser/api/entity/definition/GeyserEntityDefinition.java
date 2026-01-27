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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.util.Identifier;

import java.util.List;

/**
 * Represents a Bedrock entity definition.
 * Custom Bedrock entity definitions must be registered in the
 * {@link GeyserDefineEntitiesEvent} before usage!
 */
public interface GeyserEntityDefinition {

    /**
     * @return the Bedrock entity identifier
     */
    Identifier identifier();

    /**
     * @return the properties registered for this entity type
     */
    List<GeyserEntityProperty<?>> properties();

    /**
     * @return whether this entity is a vanilla entity
     */
    boolean vanilla();

    /**
     * @return whether this definition has been registered
     */
    boolean registered();

    /**
     * Creates or retrieves a GeyserEntityDefinition by the Bedrock entity type identifier.
     *
     * @param identifier the Bedrock entity identifier
     * @return the GeyserEntityDefinition
     */
    static @NonNull GeyserEntityDefinition of(@NonNull Identifier identifier) {
        return GeyserApi.api().provider(GeyserEntityDefinition.class, identifier);
    }
}
