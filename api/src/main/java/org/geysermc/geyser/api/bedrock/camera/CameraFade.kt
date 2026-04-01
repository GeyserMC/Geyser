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
package org.geysermc.geyser.api.bedrock.camera

import org.checkerframework.common.value.qual.IntRange
import org.geysermc.geyser.api.GeyserApi
import java.awt.Color

/**
 * Represents a coloured fade overlay on the camera.
 * 
 * 
 * Can be sent with [CameraData.sendCameraFade], or with a [CameraPosition] instruction.
 */
interface CameraFade {
    /**
     * Gets the color overlay of the camera.
     * Bedrock uses an RGB color system.
     * 
     * @return the color of the fade
     */
    fun color(): Color

    /**
     * Gets the seconds it takes to fade in.
     * All fade times combined must take at least 0.5 seconds, and at most 30 seconds.
     * 
     * @return the seconds it takes to fade in
     */
    fun fadeInSeconds(): Float

    /**
     * Gets the seconds the overlay is held.
     * All fade times combined must take at least 0.5 seconds, and at most 30 seconds.
     * 
     * @return the seconds the overlay is held
     */
    fun fadeHoldSeconds(): Float

    /**
     * Gets the seconds it takes to fade out.
     * All fade times combined must take at least 0.5 seconds, and at most 30 seconds.
     * 
     * @return the seconds it takes to fade out
     */
    fun fadeOutSeconds(): Float

    interface Builder {
        fun color(color: Color): Builder?

        fun fadeInSeconds(fadeInSeconds: @IntRange(from = 0, to = 10) Float): Builder?

        fun fadeHoldSeconds(fadeHoldSeconds: @IntRange(from = 0, to = 10) Float): Builder?

        fun fadeOutSeconds(fadeOutSeconds: @IntRange(from = 0, to = 10) Float): Builder?

        fun build(): CameraFade?
    }

    companion object {
        /**
         * Creates a Builder for CameraFade
         * 
         * @return a CameraFade Builder
         */
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
