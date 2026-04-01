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
package org.geysermc.geyser.api.bedrock.camera

import org.geysermc.geyser.api.connection.GeyserConnection
import java.util.*

/**
 * This interface holds all the methods that relate to a client's camera.
 * Can be accessed through [GeyserConnection.camera].
 */
interface CameraData {
    /**
     * Sends a camera fade instruction to the client.
     * If an existing camera fade is already in progress, the current fade will be prolonged.
     * Can be built using [CameraFade.Builder].
     * To stop a fade early, use [.clearCameraInstructions].
     * 
     * @param fade the camera fade instruction to send
     */
    fun sendCameraFade(fade: CameraFade)

    /**
     * Sends a camera position instruction to the client.
     * If an existing camera movement is already in progress,
     * the final camera position will be the one of the latest instruction, and
     * the (optional) camera fade will be added on top of the existing fade.
     * Can be built using [CameraPosition.Builder].
     * To stop reset the camera position/stop ongoing instructions, use [.clearCameraInstructions].
     * 
     * @param position the camera position instruction to send
     */
    fun sendCameraPosition(position: CameraPosition)

    /**
     * Stops all sent camera instructions (fades, movements, and perspective locks).
     * This will not stop any camera shakes/input locks/fog effects, use the respective methods for those.
     */
    fun clearCameraInstructions()

    /**
     * Forces a [CameraPerspective] on the client. This will prevent the client
     * from changing their camera perspective until it is unlocked via [.clearCameraInstructions].
     * 
     * 
     * Note: You cannot force a client into a free camera perspective with this method.
     * To do that, send a [CameraPosition] via [.sendCameraPosition] - it requires a set position
     * instead of being relative to the player.
     * 
     * @param perspective the [CameraPerspective] to force
     */
    fun forceCameraPerspective(perspective: CameraPerspective)

    /**
     * Gets the client's current [CameraPerspective], if one is currently forced.
     * This will return `null` if the client is not currently forced into a perspective.
     * If a perspective is forced, the client will not be able to change their camera perspective until it is unlocked.
     * 
     * @return the forced perspective, or `null` if none is forced
     */
    fun forcedCameraPerspective(): CameraPerspective?

    /**
     * Shakes the client's camera.
     * 
     * 
     * If the camera is already shaking with the same [CameraShake] type, then the additional intensity
     * will be layered on top of the existing intensity, with their own distinct durations.<br></br>
     * If the existing shake type is different and the new intensity/duration are not positive, the existing shake only
     * switches to the new type. Otherwise, the existing shake is completely overridden.
     * 
     * @param intensity the intensity of the shake. The client has a maximum total intensity of 4.
     * @param duration the time in seconds that the shake will occur for
     * @param type the type of shake
     */
    fun shakeCamera(intensity: Float, duration: Float, type: CameraShake)

    /**
     * Stops all camera shakes of any type.
     */
    fun stopCameraShake()

    /**
     * Adds the given fog IDs to the fog cache, then sends all fog IDs in the cache to the client.
     * 
     * 
     * Fog IDs can be found [here](https://wiki.bedrock.dev/documentation/fog-ids.html)
     * 
     * @param fogNameSpaces the fog IDs to add. If empty, the existing cached IDs will still be sent.
     */
    fun sendFog(vararg fogNameSpaces: String?)

    /**
     * Removes the given fog IDs from the fog cache, then sends all fog IDs in the cache to the client.
     * 
     * @param fogNameSpaces the fog IDs to remove. If empty, all fog IDs will be removed.
     */
    fun removeFog(vararg fogNameSpaces: String?)

    /**
     * Returns an immutable copy of all fog affects currently applied to this client.
     */
    fun fogEffects(): MutableSet<String?>

    /**
     * (Un)locks the client's camera, so that they cannot look around.
     * To ensure the camera is only unlocked when all locks are released, you must supply
     * a UUID when using method, and use the same UUID to unlock the camera.
     * 
     * @param lock whether to lock the camera
     * @param owner the owner of the lock, represented with a UUID
     * @return if the camera is locked after this method call
     */
    fun lockCamera(lock: Boolean, owner: UUID): Boolean

    /**
     * Returns whether the client's camera is locked.
     * 
     * @return whether the camera is currently locked
     */
    val isCameraLocked: Boolean

    /**
     * Hides a [GuiElement] on the client's side.
     * 
     * @param element the [GuiElement] to hide
     */
    fun hideElement(vararg element: GuiElement)

    /**
     * Resets a [GuiElement] on the client's side.
     * This makes the client decide on its own - e.g. based on client settings -
     * whether to show or hide the gui element.
     * 
     * 
     * If no elements are specified, this will reset all currently hidden elements
     * 
     * @param element the [GuiElement] to reset
     */
    fun resetElement(vararg element: GuiElement?)

    /**
     * Determines whether a [GuiElement] is currently hidden.
     * 
     * @param element the [GuiElement] to check
     */
    fun isHudElementHidden(element: GuiElement): Boolean

    /**
     * Returns the currently hidden [GuiElement]s.
     * 
     * @return an unmodifiable view of all currently hidden [GuiElement]s
     */
    fun hiddenElements(): MutableSet<GuiElement?>
}
