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

package org.geysermc.geyser.api.entity.data;

import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Represents a list of objects for specific entity data types.
 * For example, there can be multiple hitboxes on an entity.
 *
 * @param <T> the object type in the list
 * @since 2.11.0
 */
@ApiStatus.Experimental
public abstract sealed class GeyserListEntityDataType<T> extends GeyserEntityDataType<List<T>>
        permits GeyserListEntityDataType.SimpleListType {

    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    GeyserListEntityDataType(Identifier identifier) {
        super(identifier, (Class<List<T>>) (Class<?>) List.class);
    }

    /**
     * @return the class of the list entries
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    public abstract Class<T> listEntryClass();

    @ApiStatus.Internal
    static <T> GeyserListEntityDataType<T> createList(Identifier identifier, Class<T> listEntryClass) {
        return new SimpleListType<>(identifier, listEntryClass);
    }

    @ApiStatus.Internal
    static final class SimpleListType<T> extends GeyserListEntityDataType<T> {
        private final Class<T> listEntryClass;

        @ApiStatus.Internal
        SimpleListType(Identifier identifier, Class<T> listEntryClass) {
            super(identifier);
            this.listEntryClass = listEntryClass;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<T> listEntryClass() {
            return listEntryClass;
        }
    }
}
