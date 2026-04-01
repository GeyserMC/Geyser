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
package org.geysermc.geyser.api.item.custom

/**
 * This class is used to store the render offsets of custom items.
 */
@kotlin.jvm.JvmRecord
data class CustomRenderOffsets(@kotlin.jvm.JvmField val mainHand: Hand?, @kotlin.jvm.JvmField val offhand: Hand?) {
    /**
     * The hand that is used for the offset.
     */
    @kotlin.jvm.JvmRecord
    data class Hand(@kotlin.jvm.JvmField val firstPerson: Offset?, @kotlin.jvm.JvmField val thirdPerson: Offset?)

    /**
     * The offset of the item.
     */
    @kotlin.jvm.JvmRecord
    data class Offset(@kotlin.jvm.JvmField val position: OffsetXYZ?, @kotlin.jvm.JvmField val rotation: OffsetXYZ?, @kotlin.jvm.JvmField val scale: OffsetXYZ?)

    /**
     * X, Y and Z positions for the offset.
     */
    @kotlin.jvm.JvmRecord
    data class OffsetXYZ(@kotlin.jvm.JvmField val x: Float, @kotlin.jvm.JvmField val y: Float, @kotlin.jvm.JvmField val z: Float)
}
