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
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.geyser.api.bedrock.camera.CameraEaseType;
import org.geysermc.geyser.api.bedrock.camera.CameraFade;
import org.geysermc.geyser.api.bedrock.camera.CameraMovement;
import org.geysermc.geyser.api.bedrock.camera.CameraPreset;
import org.geysermc.geyser.api.util.Position;

public record GeyserCameraMovement(CameraPreset type,
                                   CameraFade fade,
                                   boolean renderPlayerEffects,
                                   boolean playerPositionForAudio,

                                   CameraEaseType easeType,

                                   int easeDuration,
                                   Position position,

                                   @IntRange(from = -90, to = 90) int rotationX,
                                   int rotationY,

                                   Position facingPosition
                                      ) implements CameraMovement {


    public static class Builder implements CameraMovement.Builder {
        private CameraPreset type;
        private CameraFade fade;
        private boolean renderPlayerEffects;
        private boolean playerPositionForAudio;

        private CameraEaseType easeType;

        private int easeDuration;
        private Position position;

        private @IntRange(from = -90, to = 90) int rotationX;
        private int rotationY;

        private Position facingPosition;


        @Override
        public CameraMovement.Builder type(@NonNull CameraPreset type) {
            if (type == null) {
                throw new IllegalArgumentException("Camera type cannot be null");
            }
            this.type = type;
            return this;
        }

        @Override
        public CameraMovement.Builder fade(@NonNull CameraFade fade) {
            if (fade == null) {
                throw new IllegalArgumentException("Camera fade cannot be null");
            }
            this.fade = fade;
            return this;
        }

        @Override
        public CameraMovement.Builder renderPlayerEffects(boolean renderPlayerEffects) {
            this.renderPlayerEffects = renderPlayerEffects;
            return this;
        }

        @Override
        public CameraMovement.Builder playerPositionForAudio(boolean playerPositionForAudio) {
            this.playerPositionForAudio = playerPositionForAudio;
            return this;
        }

        @Override
        public CameraMovement.Builder easeType(@NonNull CameraEaseType easeType) {
            if (easeType == null) {
                throw new IllegalArgumentException("Camera ease type cannot be null");
            }
            this.easeType = easeType;
            return this;
        }

        @Override
        public CameraMovement.Builder easeDuration(int easeDuration) {
            this.easeDuration = easeDuration;
            return this;
        }

        @Override
        public CameraMovement.Builder position(@NonNull Position position) {
            if (position == null) {
                throw new IllegalArgumentException("Camera position cannot be null");
            }
            this.position = position;
            return this;
        }

        @Override
        public CameraMovement.Builder rotationX(int rotationX) {
            this.rotationX = rotationX;
            return this;
        }

        @Override
        public CameraMovement.Builder rotationY(int rotationY) {
            this.rotationY = rotationY;
            return this;
        }

        @Override
        public CameraMovement.Builder facingPosition(@NonNull Position facingPosition) {
            if (facingPosition == null) {
                throw new IllegalArgumentException("Camera facing position cannot be null");
            }
            this.facingPosition = facingPosition;
            return this;
        }

        @Override
        public CameraMovement build() {
            return new GeyserCameraMovement(type, fade, renderPlayerEffects, playerPositionForAudio, easeType, easeDuration, position, rotationX, rotationY, facingPosition);
        }
    }
}
