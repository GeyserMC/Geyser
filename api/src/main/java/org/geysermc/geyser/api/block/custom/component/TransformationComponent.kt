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
 * This class is used to store the transformation component of a block
 * 
 * @param rx The rotation on the x axis
 * @param ry The rotation on the y axis
 * @param rz The rotation on the z axis
 * @param sx The scale on the x axis
 * @param sy The scale on the y axis
 * @param sz The scale on the z axis
 * @param tx The translation on the x axis
 * @param ty The translation on the y axis
 * @param tz The translation on the z axis
 */
public record TransformationComponent(int rx, int ry, int rz, float sx, float sy, float sz, float tx, float ty, float tz) {

    /**
     * Constructs a new TransformationComponent with the rotation values and assumes default scale and translation
     *
     * @param rx The rotation on the x axis
     * @param ry The rotation on the y axis
     * @param rz The rotation on the z axis
     */
    public TransformationComponent(int rx, int ry, int rz) {
        this(rx, ry, rz, 1, 1, 1, 0, 0, 0);
    }

    /**
     * Constructs a new TransformationComponent with the rotation and scale values and assumes default translation
     *
     * @param rx The rotation on the x axis
     * @param ry The rotation on the y axis
     * @param rz The rotation on the z axis
     * @param sx The scale on the x axis
     * @param sy The scale on the y axis
     * @param sz The scale on the z axis
     */
    public TransformationComponent(int rx, int ry, int rz, float sx, float sy, float sz) {
        this(rx, ry, rz, sx, sy, sz, 0, 0, 0);
    }
}
