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
import org.geysermc.geyser.api.util.Position;

public interface CameraMovement {

    /**
     * Gets the name of the camera preset, if set
     *
     * @return the {@link CameraPreset}
     */
    @NonNull CameraPreset type();

    /**
     * Controls whether player effects, such as night vision or blindness, should be rendered.
     *
     * @return whether player effects should be rendered
     */
    boolean renderPlayerEffects();

    /**
     * Controls whether the player position should be used for directional audio.
     * If false, the camera position will be used instead.
     *
     * @return whether the players position should be used for directional audio.
     */
    boolean playerPositionForAudio();

    /**
     * Gets the {@link CameraEaseType} of the camera.
     * Only applies to the {@link CameraPreset#FREE} preset.
     *
     * @return Camera's ease type.
     */
    @NonNull CameraEaseType easeType();

    /**
     * Gets the easing duration of the camera.
     * Is only used if a {@link CameraEaseType} is set.
     *
     * @return Camera's easing duration.
     */
    int easeDuration();

    /**
     * Gets the camera {@link Position}.
     *
     * @return Camera's position.
     */
    @NonNull Position position();

    /**
     * Gets the x rotation of the camera.
     * To prevent the camera from being upside down, the range is limited to -90 to 90.
     *
     * @return Camera's x rotation.
     */
    @IntRange(from = -90, to = 90) int rotationX();

    /**
     * Gets the y rotation of the camera.
     *
     * @return Camera's y rotation.
     */
    int rotationY();

    /**
     * Gets the facing position of the camera.
     *
     * @return Camera's facing position.
     */
    @NonNull Position facingPosition();


    interface Builder {

        Builder type(@NonNull CameraPreset type);

        Builder fade(@NonNull CameraFade fade);

        Builder renderPlayerEffects(boolean renderPlayerEffects);

        Builder playerPositionForAudio(boolean playerPositionForAudio);

        Builder easeType(@NonNull CameraEaseType easeType);

        Builder easeDuration(int easeDuration);

        Builder position(@NonNull Position position);

        Builder rotationX(int rot_x);

        Builder rotationY(int rot_y);

        Builder facingPosition(@NonNull Position facingPosition);

        CameraMovement build();
    }
}
