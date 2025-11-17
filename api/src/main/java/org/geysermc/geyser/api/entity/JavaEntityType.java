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

package org.geysermc.geyser.api.entity;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Represents a Java edition entity type
 */
public interface JavaEntityType {

    /**
     * @return the Java identifier of the type
     */
    Identifier identifier();

    /**
     * @return the numeric Java entity type id
     */
    int javaId();

    /**
     * @return whether this entity exists in the vanilla base game
     */
    boolean vanilla();

    /**
     * Compares two entity identifiers.
     *
     * @param javaIdentifier the other entity identifier
     * @return true if the entity identifier is the same
     */
    default boolean is(Identifier javaIdentifier) {
        return identifier().equals(javaIdentifier);
    }

    static JavaEntityType ofVanilla(@NonNull Identifier javaIdentifier) {
        return GeyserApi.api().provider(JavaEntityType.class, javaIdentifier);
    }

    // TODO move to event???
    static JavaEntityType createAndRegister(@NonNull Identifier javaIdentifier, @NonNegative int javaId) {
        return GeyserApi.api().provider(JavaEntityType.class, javaIdentifier, javaId);
    }
}
