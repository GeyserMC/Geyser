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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.value.qual.IntRange"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.geysermc.geyser.api.GeyserApi"


public interface CameraPosition {


    Vector3f position();


    CameraEaseType easeType();


    CameraFade cameraFade();


    float easeSeconds();


    @IntRange(from = -90, to = 90) int rotationX();


    int rotationY();


    Vector3f facingPosition();


    bool renderPlayerEffects();


    bool playerPositionForAudio();


    static CameraPosition.Builder builder() {
        return GeyserApi.api().provider(CameraPosition.Builder.class);
    }

    interface Builder {

        Builder cameraFade(CameraFade cameraFade);

        Builder renderPlayerEffects(bool renderPlayerEffects);

        Builder playerPositionForAudio(bool playerPositionForAudio);

        Builder easeType(CameraEaseType easeType);

        Builder easeSeconds(float easeSeconds);

        Builder position(Vector3f position);

        Builder rotationX(@IntRange(from = -90, to = 90) int rotationX);

        Builder rotationY(int rotationY);

        Builder facingPosition(Vector3f facingPosition);

        CameraPosition build();
    }
}
