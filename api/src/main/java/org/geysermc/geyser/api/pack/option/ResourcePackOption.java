/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.pack.option;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.pack.ResourcePack;

/**
 * Represents a resource pack option that can be used to specify how a resource
 * pack is sent to Bedrock clients.
 */
public interface ResourcePackOption<T> {

    /**
     * @return the option type
     */
    @NonNull Type type();

    /**
     * @return the value of the option
     */
    @NonNull T value();

    /**
     * Used to validate a specific options for a pack.
     * Some options are not applicable to some packs.
     *
     * @param pack the resource pack to validate the option for
     */
    void validate(@NonNull ResourcePack pack);

    /**
     * The different types of resource pack options.
     */
    enum Type {
        SUBPACK,
        PRIORITY,
        FALLBACK
    }

}
