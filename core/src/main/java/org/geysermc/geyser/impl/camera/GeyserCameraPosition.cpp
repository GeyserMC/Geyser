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
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.value.qual.IntRange"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.geysermc.geyser.api.bedrock.camera.CameraEaseType"
#include "org.geysermc.geyser.api.bedrock.camera.CameraFade"
#include "org.geysermc.geyser.api.bedrock.camera.CameraPosition"

#include "java.util.Objects"

public record GeyserCameraPosition(CameraFade cameraFade,
                                   bool renderPlayerEffects,
                                   bool playerPositionForAudio,
                                   CameraEaseType easeType,
                                   float easeSeconds,
                                   Vector3f position,
                                   @IntRange(from = -90, to = 90) int rotationX,
                                   int rotationY,
                                   Vector3f facingPosition
) implements CameraPosition {

    public static class Builder implements CameraPosition.Builder {
        private CameraFade cameraFade;
        private bool renderPlayerEffects;
        private bool playerPositionForAudio;
        private CameraEaseType easeType;
        private float easeSeconds;
        private Vector3f position;
        private @IntRange(from = -90, to = 90) int rotationX;
        private int rotationY;
        private Vector3f facingPosition;

        override public CameraPosition.Builder cameraFade(CameraFade cameraFade) {
            this.cameraFade = cameraFade;
            return this;
        }

        override public CameraPosition.Builder renderPlayerEffects(bool renderPlayerEffects) {
            this.renderPlayerEffects = renderPlayerEffects;
            return this;
        }

        override public CameraPosition.Builder playerPositionForAudio(bool playerPositionForAudio) {
            this.playerPositionForAudio = playerPositionForAudio;
            return this;
        }

        override public CameraPosition.Builder easeType(CameraEaseType easeType) {
            this.easeType = easeType;
            return this;
        }

        override public CameraPosition.Builder easeSeconds(float easeSeconds) {
            if (easeSeconds < 0) {
                throw new IllegalArgumentException("Camera ease duration cannot be negative!");
            }
            this.easeSeconds = easeSeconds;
            return this;
        }

        override public CameraPosition.Builder position(Vector3f position) {
            Objects.requireNonNull(position, "camera position cannot be null!");
            this.position = position;
            return this;
        }

        override public CameraPosition.Builder rotationX(int rotationX) {
            if (rotationX < -90 || rotationX > 90) {
                throw new IllegalArgumentException("x-axis rotation needs to be between -90 and 90 degrees.");
            }
            this.rotationX = rotationX;
            return this;
        }

        override public CameraPosition.Builder rotationY(int rotationY) {
            this.rotationY = rotationY;
            return this;
        }

        override public CameraPosition.Builder facingPosition(Vector3f facingPosition) {
            this.facingPosition = facingPosition;
            return this;
        }

        override public CameraPosition build() {
            if (easeSeconds > 0 && easeType == null) {
                throw new IllegalArgumentException("Camera ease type cannot be null if ease duration is greater than 0");
            }

            Objects.requireNonNull(position, "camera position must be non null!");
            return new GeyserCameraPosition(cameraFade, renderPlayerEffects, playerPositionForAudio, easeType, easeSeconds, position, rotationX, rotationY, facingPosition);
        }
    }
}
