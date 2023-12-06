/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

/**
 * Represents the creative menu categories or tabs.
 */
public enum CreativeCategory {
    COMMANDS("commands", 1),
    CONSTRUCTION("construction", 2),
    EQUIPMENT("equipment", 3),
    ITEMS("items", 4),
    NATURE("nature", 5),
    NONE("none", 6);

    private final String internalName;
    private final int id;

    CreativeCategory(String internalName, int id) {
        this.internalName = internalName;
        this.id = id;
    }

    /**
     * Gets the internal name of the category.
     * 
     * @return the name of the category
     */
    public @NonNull String internalName() {
        return internalName;
    }

    /**
     * Gets the internal ID of the category.
     * 
     * @return the ID of the category
     */
    public int id() {
        return id;
    }
}
