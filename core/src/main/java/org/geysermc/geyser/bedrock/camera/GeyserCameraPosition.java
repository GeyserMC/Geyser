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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.geyser.api.bedrock.camera.CameraEaseType;
import org.geysermc.geyser.api.bedrock.camera.CameraFade;
import org.geysermc.geyser.api.bedrock.camera.CameraPosition;
import org.geysermc.geyser.api.util.Position;

public record GeyserCameraPosition(CameraFade fade,
                                   boolean renderPlayerEffects,
                                   boolean playerPositionForAudio,
                                   CameraEaseType easeType,
                                   float easeDuration,
                                   Position position,
                                   @IntRange(from = -90, to = 90) int rotationX,
                                   int rotationY,
                                   Position facingPosition
) implements CameraPosition {


    public static class CameraPositionBuilder implements CameraPosition.Builder {
        private CameraFade fade;
        private boolean renderPlayerEffects;
        private boolean playerPositionForAudio;
        private CameraEaseType easeType;
        private float easeDuration;
        private Position position;
        private @IntRange(from = -90, to = 90) int rotationX;
        private int rotationY;
        private Position facingPosition;

        @Override
        public CameraPosition.Builder fade(@Nullable CameraFade fade) {
            this.fade = fade;
            return this;
        }

        @Override
        public CameraPosition.Builder renderPlayerEffects(boolean renderPlayerEffects) {
            this.renderPlayerEffects = renderPlayerEffects;
            return this;
        }

        @Override
        public CameraPosition.Builder playerPositionForAudio(boolean playerPositionForAudio) {
            this.playerPositionForAudio = playerPositionForAudio;
            return this;
        }

        @Override
        public CameraPosition.Builder easeType(@Nullable CameraEaseType easeType) {
            this.easeType = easeType;
            return this;
        }

        @Override
        public CameraPosition.Builder easeDuration(float easeDuration) {
            if (easeDuration < 0) {
                throw new IllegalArgumentException("Camera ease duration cannot be negative");
            }
            this.easeDuration = easeDuration;
            return this;
        }

        @Override
        public CameraPosition.Builder position(@NonNull Position position) {
            if (position == null) {
                throw new IllegalArgumentException("Camera position cannot be null");
            }
            this.position = position;
            return this;
        }

        @Override
        public CameraPosition.Builder rotationX(int rotationX) {
            this.rotationX = rotationX;
            return this;
        }

        @Override
        public CameraPosition.Builder rotationY(int rotationY) {
            this.rotationY = rotationY;
            return this;
        }

        @Override
        public CameraPosition.Builder facingPosition(@NonNull Position facingPosition) {
            if (facingPosition == null) {
                throw new IllegalArgumentException("Camera facing position cannot be null");
            }
            this.facingPosition = facingPosition;
            return this;
        }

        @Override
        public CameraPosition build() {
            if (easeDuration > 0 && easeType == null) {
                throw new IllegalArgumentException("Camera ease type cannot be null if ease duration is greater than 0");
            }

            if (position == null) {
                throw new IllegalArgumentException("Camera position cannot be null");
            }

            return new GeyserCameraPosition(fade, renderPlayerEffects, playerPositionForAudio, easeType, easeDuration, position, rotationX, rotationY, facingPosition);
        }
    }
}
