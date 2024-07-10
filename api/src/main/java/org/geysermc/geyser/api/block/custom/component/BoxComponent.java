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

package org.geysermc.geyser.api.block.custom.component;

/**
 * This class is used to store a box component for the selection and
 * collision boxes of a custom block.
 * 
 * @param originX The origin X of the box
 * @param originY The origin Y of the box
 * @param originZ The origin Z of the box
 * @param sizeX The size X of the box
 * @param sizeY The size Y of the box
 * @param sizeZ The size Z of the box
 */
public record BoxComponent(float originX, float originY, float originZ, float sizeX, float sizeY, float sizeZ) {
    private static final BoxComponent FULL_BOX = new BoxComponent(-8, 0, -8, 16, 16, 16);
    private static final BoxComponent EMPTY_BOX = new BoxComponent(0, 0, 0, 0, 0, 0);

    /**
     * Gets a full box component
     *
     * @return A full box component
     */
    public static BoxComponent fullBox() {
        return FULL_BOX;
    }

    /**
     * Gets an empty box component
     *
     * @return An empty box component
     */
    public static BoxComponent emptyBox() {
        return EMPTY_BOX;
    }

    /**
     * Gets if the box component is empty
     *
     * @return If the box component is empty.
     */
    public boolean isEmpty() {
        return sizeX == 0 && sizeY == 0 && sizeZ == 0;
    }
}
