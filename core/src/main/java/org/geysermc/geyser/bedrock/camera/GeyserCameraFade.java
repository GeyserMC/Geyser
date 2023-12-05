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

package org.geysermc.geyser.bedrock.camera;

import org.geysermc.geyser.api.bedrock.camera.CameraFade;

public record GeyserCameraFade(
        int red,
        int green,
        int blue,
        int fadeInSeconds,
        int holdSeconds,
        int fadeOutSeconds
) implements CameraFade {

    public static class Builder implements CameraFade.Builder {
        private int red;
        private int green;
        private int blue;

        private int fadeInSeconds;
        private int holdSeconds;
        private int fadeOutSeconds;

        @Override
        public CameraFade.Builder red(int red) {
            this.red = red;
            return this;
        }

        @Override
        public CameraFade.Builder green(int green) {
            this.green = green;
            return this;
        }

        @Override
        public CameraFade.Builder blue(int blue) {
            this.blue = blue;
            return this;
        }

        @Override
        public CameraFade.Builder fadeInSeconds(int fadeInSeconds) {
            this.fadeInSeconds = fadeInSeconds;
            return this;
        }

        @Override
        public CameraFade.Builder holdSeconds(int holdSeconds) {
            this.holdSeconds = holdSeconds;
            return this;
        }

        @Override
        public CameraFade.Builder fadeOutSeconds(int fadeOutSeconds) {
            this.fadeOutSeconds = fadeOutSeconds;
            return this;
        }

        @Override
        public CameraFade build() {
            return new GeyserCameraFade(red, green, blue, fadeInSeconds, holdSeconds, fadeOutSeconds);
        }
    }
}
