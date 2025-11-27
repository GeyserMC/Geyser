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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.entity.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.custom.CustomJavaEntityType;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Called when entities are defined within Geyser.
 * <p>
 * This event can be used to add custom entities to Geyser.
 * Entity definitions can be created using the builder provided
 * inside of {@link GeyserEntityDefinition}.
 */
public interface GeyserDefineEntitiesEvent extends Event {

    /**
     * @return an immutable collection of all registered entity definitions
     */
    Collection<GeyserEntityDefinition> entities();

    /**
     * Registers a custom entity definition
     * @param customEntityDefinition the custom entity definition to register
     */
    void register(@NonNull CustomEntityDefinition customEntityDefinition);

    /**
     * Registers a non-vanilla Java entity type.
     *
     * @param builderConsumer the builder for the non-vanilla type
     */
    void registerEntityType(Consumer<CustomJavaEntityType.Builder> builderConsumer);
}
