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

package org.geysermc.geyser.api.bedrock.camera;

import org.checkerframework.common.value.qual.IntRange;

/**
 * Represents a fade in/out color overlay
 */
public interface CameraFade {

    /**
     * Gets the red value of the color overlay.
     * If not set, defaults to 0.
     * Must be between 0 and 255.
     *
     * @return the red value of the color overlay.
     */
    @IntRange(from = 0, to = 255) int red();

    /**
     * Gets the green value of the color overlay.
     * If not set, defaults to 0.
     * Must be between 0 and 255.
     *
     * @return the green value of the color overlay.
     */
    @IntRange(from = 0, to = 255) int green();

    /**
     * Gets the blue value of the color overlay.
     * If not set, defaults to 0.
     * Must be between 0 and 255.
     *
     * @return the blue value of the color overlay.
     */
    @IntRange(from = 0, to = 255) int blue();

    /**
     * Gets the seconds it takes to fade in.
     * All fade times combined must take at least 0.5 seconds, and at most 10 seconds.
     *
     * @return the seconds it takes to fade in.
     */
    int fadeInSeconds();

    /**
     * Gets the seconds the overlay is held.
     * All fade times combined must take at least 0.5 seconds, and at most 10 seconds.
     *
     * @return the seconds the overlay is held.
     */
    int holdSeconds();

    /**
     * Gets the seconds it takes to fade out.
     * All fade times combined must take at least 0.5 seconds, and at most 10 seconds.
     *
     * @return the seconds it takes to fade out.
     */
    int fadeOutSeconds();

    interface Builder {

        Builder red(int red);

        Builder green(int green);

        Builder blue(int blue);

        Builder fadeInSeconds(int fadeInSeconds);

        Builder holdSeconds(int holdSeconds);

        Builder fadeOutSeconds(int fadeOutSeconds);

        CameraFade build();
    }
}