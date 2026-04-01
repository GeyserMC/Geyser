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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.geyser.api.GeyserApi;

import java.awt.Color;

/**
 * Represents a coloured fade overlay on the camera.
 * <p>
 * Can be sent with {@link CameraData#sendCameraFade(CameraFade)}, or with a {@link CameraPosition} instruction.
 */
public interface CameraFade {

    /**
     * Gets the color overlay of the camera.
     * Bedrock uses an RGB color system.
     *
     * @return the color of the fade
     */
    @NonNull Color color();

    /**
     * Gets the seconds it takes to fade in.
     * All fade times combined must take at least 0.5 seconds, and at most 30 seconds.
     *
     * @return the seconds it takes to fade in
     */
    float fadeInSeconds();

    /**
     * Gets the seconds the overlay is held.
     * All fade times combined must take at least 0.5 seconds, and at most 30 seconds.
     *
     * @return the seconds the overlay is held
     */
    float fadeHoldSeconds();

    /**
     * Gets the seconds it takes to fade out.
     * All fade times combined must take at least 0.5 seconds, and at most 30 seconds.
     *
     * @return the seconds it takes to fade out
     */
    float fadeOutSeconds();

    /**
     * Creates a Builder for CameraFade
     *
     * @return a CameraFade Builder
     */
    static CameraFade.Builder builder() {
        return GeyserApi.api().provider(CameraFade.Builder.class);
    }

    interface Builder {

        Builder color(@NonNull Color color);

        Builder fadeInSeconds(@IntRange(from = 0, to = 10) float fadeInSeconds);

        Builder fadeHoldSeconds(@IntRange(from = 0, to = 10) float fadeHoldSeconds);

        Builder fadeOutSeconds(@IntRange(from = 0, to = 10) float fadeOutSeconds);

        CameraFade build();
    }
}
