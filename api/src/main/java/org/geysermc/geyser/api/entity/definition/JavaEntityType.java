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
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Represents a Java edition entity type.
 *
 * @since 2.11.0
 */
@ApiStatus.Experimental
public interface JavaEntityType {

    /**
     * Each entity type on Java edition has an identifier, such as {@code "minecraft:pig"} that uniquely identifies it.
     * Modded entities cannot be in the minecraft namespace!
     *
     * @return the Java identifier of the entity type
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    Identifier identifier();

    /**
     * Indicates whether this entity type exists in the Minecraft: Java Edition base game, or whether it is a custom entity.
     * Note: As of API 2.11.0, this would always be true!
     *
     * @return whether this entity exists in the vanilla base game
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    boolean vanilla();

    /**
     * This entity type's default width, not respective of pose-adjusted width.
     *
     * @return the default width of the Java entity type
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    float width();

    /**
     * This entity type's default height, not respective of pose-adjusted height.
     *
     * @return the default height of the Java entity type
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    float height();

    /**
     * Compares two entity identifiers.
     *
     * @param javaIdentifier the other entity identifier
     * @return true if the entity identifier is the same
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    default boolean is(Identifier javaIdentifier) {
        return identifier().equals(javaIdentifier);
    }

    /**
     * Gets the default Bedrock entity definition which is associated with this Minecraft: Java Edition entity type.
     *
     * @return the default Bedrock entity definition
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    @Nullable GeyserEntityDefinition defaultBedrockDefinition();
}
