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

public interface CameraInstruction {

    /**
     * Gets the name of the camera preset.
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
    CameraEaseType easeType();

    /**
     * Gets the easing duration of the camera.
     * Is only used if a {@link CameraEaseType} is set.
     *
     * @return Camera's easing duration.
     */
    int easeDuration();

    /**
     * Gets the x position of the camera.
     *
     * @return Camera's x position.
     */
    int pos_x();

    /**
     * Gets the y position of the camera.
     *
     * @return Camera's y position.
     */
    int pos_y();

    /**
     * Gets the z position of the camera.
     *
     * @return Camera's z position.
     */
    int pos_z();

    /**
     * Gets the x rotation of the camera.
     * To prevent the camera from being upside down, the range is limited to -90 to 90.
     *
     * @return Camera's x rotation.
     */
    @IntRange(from = -90, to = 90) int rot_x();

    /**
     * Gets the y rotation of the camera.
     *
     * @return Camera's y rotation.
     */
    int rot_y();

    // TODO: implement this?
    int facing_x();
    int facing_y();
    int facing_z();

    interface Builder {

        Builder type(@NonNull CameraPreset type);

        Builder renderPlayerEffects(boolean renderPlayerEffects);

        Builder playerPositionForAudio(boolean playerPositionForAudio);

        Builder easeType(@NonNull CameraEaseType easeType);

        Builder easeDuration(int easeDuration);

        Builder pos_x(int pos_x);

        Builder pos_y(int pos_y);

        Builder pos_z(int pos_z);

        Builder rot_x(int rot_x);

        Builder rot_y(int rot_y);

        Builder rot_z(int rot_z);

        Builder facing_x(int facing_x);

        Builder facing_y(int facing_y);

        Builder facing_z(int facing_z);

        CameraInstruction build();
    }
}



