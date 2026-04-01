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
package org.geysermc.geyser.api.block.custom.component

import kotlin.collections.LinkedHashMap
import kotlin.collections.MutableSet

/**
 * This class is used to store conditions for a placement filter for a custom block.
 * 
 * @param allowedFaces The faces that the block can be placed on
 * @param blockFilters The block filters that control what blocks the block can be placed on
 */
@kotlin.jvm.JvmRecord
data class PlacementConditions(
    @kotlin.jvm.JvmField val allowedFaces: MutableSet<Face?>,
    @kotlin.jvm.JvmField val blockFilters: LinkedHashMap<String?, BlockFilterType?>
) {
    enum class Face {
        DOWN,
        UP,
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    enum class BlockFilterType {
        BLOCK,
        TAG
    }
}
