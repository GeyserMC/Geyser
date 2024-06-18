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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.Set;
import java.util.UUID;

/**
 * This interface holds all the methods that relate to a client's camera.
 * Can be accessed through {@link GeyserConnection#camera()}.
 */
public interface CameraData {

    /**
     * Sends a camera fade instruction to the client.
     * If an existing camera fade is already in progress, the current fade will be prolonged.
     * Can be built using {@link CameraFade.Builder}.
     * To stop a fade early, use {@link #clearCameraInstructions()}.
     *
     * @param fade the camera fade instruction to send
     */
    void sendCameraFade(@NonNull CameraFade fade);

    /**
     * Sends a camera position instruction to the client.
     * If an existing camera movement is already in progress,
     * the final camera position will be the one of the latest instruction, and
     * the (optional) camera fade will be added on top of the existing fade.
     * Can be built using {@link CameraPosition.Builder}.
     * To stop reset the camera position/stop ongoing instructions, use {@link #clearCameraInstructions()}.
     *
     * @param position the camera position instruction to send
     */
    void sendCameraPosition(@NonNull CameraPosition position);

    /**
     * Stops all sent camera instructions (fades, movements, and perspective locks).
     * This will not stop any camera shakes/input locks/fog effects, use the respective methods for those.
     */
    void clearCameraInstructions();

    /**
     * Forces a {@link CameraPerspective} on the client. This will prevent the client
     * from changing their camera perspective until it is unlocked via {@link #clearCameraInstructions()}.
     * <p>
     * Note: You cannot force a client into a free camera perspective with this method.
     * To do that, send a {@link CameraPosition} via {@link #sendCameraPosition(CameraPosition)} - it requires a set position
     * instead of being relative to the player.
     *
     * @param perspective the {@link CameraPerspective} to force
     */
    void forceCameraPerspective(@NonNull CameraPerspective perspective);

    /**
     * Gets the client's current {@link CameraPerspective}, if one is currently forced.
     * This will return {@code null} if the client is not currently forced into a perspective.
     * If a perspective is forced, the client will not be able to change their camera perspective until it is unlocked.
     *
     * @return the forced perspective, or {@code null} if none is forced
     */
    @Nullable CameraPerspective forcedCameraPerspective();

    /**
     * Shakes the client's camera.
     * <p>
     * If the camera is already shaking with the same {@link CameraShake} type, then the additional intensity
     * will be layered on top of the existing intensity, with their own distinct durations.<br>
     * If the existing shake type is different and the new intensity/duration are not positive, the existing shake only
     * switches to the new type. Otherwise, the existing shake is completely overridden.
     *
     * @param intensity the intensity of the shake. The client has a maximum total intensity of 4.
     * @param duration the time in seconds that the shake will occur for
     * @param type the type of shake
     */
    void shakeCamera(float intensity, float duration, @NonNull CameraShake type);

    /**
     * Stops all camera shakes of any type.
     */
    void stopCameraShake();

    /**
     * Adds the given fog IDs to the fog cache, then sends all fog IDs in the cache to the client.
     * <p>
     * Fog IDs can be found <a href="https://wiki.bedrock.dev/documentation/fog-ids.html">here</a>
     *
     * @param fogNameSpaces the fog IDs to add. If empty, the existing cached IDs will still be sent.
     */
    void sendFog(String... fogNameSpaces);

    /**
     * Removes the given fog IDs from the fog cache, then sends all fog IDs in the cache to the client.
     *
     * @param fogNameSpaces the fog IDs to remove. If empty, all fog IDs will be removed.
     */
    void removeFog(String... fogNameSpaces);

    /**
     * Returns an immutable copy of all fog affects currently applied to this client.
     */
    @NonNull
    Set<String> fogEffects();

    /**
     * (Un)locks the client's camera, so that they cannot look around.
     * To ensure the camera is only unlocked when all locks are released, you must supply
     * a UUID when using method, and use the same UUID to unlock the camera.
     *
     * @param lock whether to lock the camera
     * @param owner the owner of the lock, represented with a UUID
     * @return if the camera is locked after this method call
     */
    boolean lockCamera(boolean lock, @NonNull UUID owner);

    /**
     * Returns whether the client's camera is locked.
     *
     * @return whether the camera is currently locked
     */
    boolean isCameraLocked();

    /**
     * Hides a {@link GuiElement} on the client's side.
     *
     * @param element the {@link GuiElement} to hide
     */
    void hideElement(@NonNull GuiElement... element);

    /**
     * Resets a {@link GuiElement} on the client's side.
     * This makes the client decide on its own - e.g. based on client settings -
     * whether to show or hide the gui element.
     * <p>
     * If no elements are specified, this will reset all currently hidden elements
     *
     * @param element the {@link GuiElement} to reset
     */
    void resetElement(@NonNull GuiElement @Nullable... element);

    /**
     * Determines whether a {@link GuiElement} is currently hidden.
     *
     * @param element the {@link GuiElement} to check
     */
    boolean isHudElementHidden(@NonNull GuiElement element);

    /**
     * Returns the currently hidden {@link GuiElement}s.
     *
     * @return an unmodifiable view of all currently hidden {@link GuiElement}s
     */
    @NonNull Set<GuiElement> hiddenElements();
}
