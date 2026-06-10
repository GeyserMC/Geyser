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

import org.geysermc.geyser.api.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Represents a Java edition entity type
 *
 * @since 2.11.0
 */
public interface JavaEntityType {

    /**
     * @return the Java identifier of the entity type
     * @since 2.11.0
     */
    Identifier identifier();

    /**
     * @return whether this entity exists in the vanilla base game
     * @since 2.11.0
     */
    boolean vanilla();

    /**
     * @return the default width of the Java entity type
     * @since 2.11.0
     */
    float width();

    /**
     * @return the default height of the Java entity type
     * @since 2.11.0
     */
    float height();

    /**
     * Compares two entity identifiers.
     *
     * @param javaIdentifier the other entity identifier
     * @return true if the entity identifier is the same
     * @since 2.11.0
     */
    default boolean is(Identifier javaIdentifier) {
        return identifier().equals(javaIdentifier);
    }

    /**
     * Gets the default Bedrock entity definition, if available,
     * that is associated with this Minecraft: Java Edition entity type.
     *
     * @return the default Bedrock entity definition
     * @since 2.11.0
     */
    @Nullable GeyserEntityDefinition defaultBedrockDefinition();
}
