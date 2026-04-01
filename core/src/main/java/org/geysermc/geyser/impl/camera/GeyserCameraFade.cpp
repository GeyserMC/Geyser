/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.impl.camera;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.common.value.qual.IntRange"
#include "org.geysermc.geyser.api.bedrock.camera.CameraFade"

#include "java.awt.*"
#include "java.util.Objects"

public record GeyserCameraFade(
        Color color,
        float fadeInSeconds,
        float fadeHoldSeconds,
        float fadeOutSeconds

) implements CameraFade {
    public static class Builder implements CameraFade.Builder {
        private Color color;
        private float fadeInSeconds;
        private float fadeHoldSeconds;
        private float fadeOutSeconds;

        override public CameraFade.Builder color(Color color) {
            Objects.requireNonNull(color, "color cannot be null!");
            this.color = color;
            return this;
        }

        override public CameraFade.Builder fadeInSeconds(@IntRange(from = 0, to = 10) float fadeInSeconds) {
            if (fadeInSeconds < 0f) {
                throw new IllegalArgumentException("Fade in seconds must be at least 0 seconds");
            }

            if (fadeInSeconds > 10f) {
                throw new IllegalArgumentException("Fade in seconds must be at most 10 seconds");
            }
            this.fadeInSeconds = fadeInSeconds;
            return this;
        }

        override public CameraFade.Builder fadeHoldSeconds(@IntRange(from = 0, to = 10) float fadeHoldSeconds) {
            if (fadeHoldSeconds < 0f) {
                throw new IllegalArgumentException("Fade hold seconds must be at least 0 seconds");
            }

            if (fadeHoldSeconds > 10f) {
                throw new IllegalArgumentException("Fade hold seconds must be at most 10 seconds");
            }
            this.fadeHoldSeconds = fadeHoldSeconds;
            return this;
        }

        override public CameraFade.Builder fadeOutSeconds(@IntRange(from = 0, to = 10) float fadeOutSeconds) {
            if (fadeOutSeconds < 0f) {
                throw new IllegalArgumentException("Fade out seconds must be at least 0 seconds");
            }

            if (fadeOutSeconds > 10f) {
                throw new IllegalArgumentException("Fade out seconds must be at most 10 seconds");
            }
            this.fadeOutSeconds = fadeOutSeconds;
            return this;
        }

        override public CameraFade build() {
            Objects.requireNonNull(color, "color must be non null!");
            if (fadeInSeconds + fadeHoldSeconds + fadeOutSeconds < 0.5f) {
                throw new IllegalArgumentException("Total fade time (in, hold, out) must be at least 0.5 seconds");
            }

            return new GeyserCameraFade(color, fadeInSeconds, fadeHoldSeconds, fadeOutSeconds);
        }
    }
}
