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

package org.geysermc.geyser.api.entity.custom;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.entity.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.JavaEntityType;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Represents a custom Minecraft: Java Edition entity type.
 * This can only be used with modded servers!
 */
public interface CustomJavaEntityType extends JavaEntityType {

    @Override
    default boolean vanilla() {
        return false;
    }

    interface Builder {

        /**
         * The entity type's identifier. It cannot be in the Minecraft namespace
         * for custom entities!
         *
         * @param entityType the identifier
         * @return this builder
         */
        Builder type(Identifier entityType);

        /**
         * The entity type's numeric network id.
         * @param javaId the java id
         * @return this builder
         */
        Builder javaId(int javaId);

        /**
         * The width of this entity.
         * @param width the width of this entity
         * @return this builder
         */
        Builder width(@NonNegative float width);

        /**
         * The height of this entity
         * @param height the height
         * @return this builder
         */
        Builder height(@NonNegative float height);

        /**
         * The default Bedrock edition entity definition.
         * You can define custom Bedrock entities, or use vanilla definitions
         * obtainable via the {@link GeyserDefineEntitiesEvent#entities()} collection.
         * Calling this method with a non-registered {@link CustomEntityDefinition} will
         * register it too.
         *
         * @param defaultBedrockDefinition the default Bedrock definition
         * @return this builder
         */
        Builder defaultBedrockDefinition(@Nullable GeyserEntityDefinition defaultBedrockDefinition);
    }
}
