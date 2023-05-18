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

package org.geysermc.geyser.level;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import org.geysermc.geyser.util.JavaCodecUtil;

import java.util.Map;

/**
 * Represents the information we store from the current Java dimension
 * @param piglinSafe Whether piglins and hoglins are safe from conversion in this dimension.
 *      This controls if they have the shaking effect applied in the dimension.
 */
public record JavaDimension(int minY, int maxY, boolean piglinSafe, double worldCoordinateScale) {

    public static void load(CompoundTag tag, Map<String, JavaDimension> map) {
        for (CompoundTag dimension : JavaCodecUtil.iterateAsTag(tag.get("minecraft:dimension_type"))) {
            CompoundTag elements = dimension.get("element");
            int minY = ((IntTag) elements.get("min_y")).getValue();
            int maxY = ((IntTag) elements.get("height")).getValue();
            // Logical height can be ignored probably - seems to be for artificial limits like the Nether.

            // Set if piglins/hoglins should shake
            boolean piglinSafe = ((Number) elements.get("piglin_safe").getValue()).byteValue() != (byte) 0;
            // Load world coordinate scale for the world border
            double coordinateScale = ((Number) elements.get("coordinate_scale").getValue()).doubleValue();

            map.put((String) dimension.get("name").getValue(), new JavaDimension(minY, maxY, piglinSafe, coordinateScale));
        }
    }
}
