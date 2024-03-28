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

package org.geysermc.geyser.api.skin;

/**
 * Represents geometry of a skin.
 *
 * @param geometryName The name of the geometry (JSON)
 * @param geometryData The geometry data (JSON)
 */
public record SkinGeometry(String geometryName, String geometryData) {

    public static SkinGeometry WIDE = getLegacy(false);
    public static SkinGeometry SLIM = getLegacy(true);

    /**
     * Generate generic geometry
     *
     * @param isSlim if true, it will be the slimmer alex model
     * @return The generic geometry object
     */
    private static SkinGeometry getLegacy(boolean isSlim) {
        return new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.custom" + (isSlim ? "Slim" : "") + "\"}}", "");
    }
}
