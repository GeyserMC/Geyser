/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is a way to represent a boolean, but with a non set value added.
 * This class was inspired by adventure's <a href="https://github.com/KyoriPowered/adventure/blob/main/4/api/src/main/java/net/kyori/adventure/util/TriState.java">TriState</a>
 */
public enum TriState {
    /**
     * Describes a value that is not set, null, or not present.
     */
    NOT_SET,

    /**
     * Describes a true value.
     */
    TRUE,

    /**
     * Describes a false value.
     */
    FALSE;

    /**
     * Converts the TriState to a boolean.
     *
     * @return the boolean value of the TriState
     */
    public @Nullable Boolean toBoolean() {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            default -> null;
        };
    }

    /**
     * Creates a TriState from a boolean.
     *
     * @param value the Boolean value
     * @return the created TriState
     */
    public static @NonNull TriState fromBoolean(@Nullable Boolean value) {
        return value == null ? NOT_SET : fromBoolean(value.booleanValue());
    }

    /**
     * Creates a TriState from a primitive boolean.
     *
     * @param value the boolean value
     * @return the created TriState
     */
    public @NonNull static TriState fromBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }
}
