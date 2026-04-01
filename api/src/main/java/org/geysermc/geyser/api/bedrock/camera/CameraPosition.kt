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

import org.checkerframework.common.value.qual.IntRange
import org.cloudburstmc.math.vector.Vector3f
import org.geysermc.geyser.api.GeyserApi

/**
 * This interface represents a camera position instruction. Can be built with the [.builder].
 * 
 * 
 * Any camera position instruction pins the client camera to a specific position and rotation.
 * You can set [CameraEaseType] to ensure a smooth transition that will last [.easeSeconds] seconds.
 * A [CameraFade] can also be sent, which will transition the player to a coloured transition during the transition.
 * 
 * 
 * Use [CameraData.sendCameraPosition] to send such an instruction to any connection.
 */
interface CameraPosition {
    /**
     * Gets the camera's position.
     * 
     * @return camera position vector
     */
    fun position(): Vector3f

    /**
     * Gets the [CameraEaseType] of the camera.
     * If not set, there is no easing.
     * 
     * @return camera ease type
     */
    fun easeType(): CameraEaseType?

    /**
     * Gets the [CameraFade] to be sent along the camera position instruction.
     * If set, they will run at once.
     * 
     * @return camera fade, or null if not present
     */
    fun cameraFade(): CameraFade?

    /**
     * Gets the easing duration of the camera, in seconds.
     * Is only used if a [CameraEaseType] is set.
     * 
     * @return camera easing duration in seconds
     */
    fun easeSeconds(): Float

    /**
     * Gets the x-axis rotation of the camera.
     * To prevent the camera from being upside down, Bedrock limits the range to -90 to 90.
     * Will be overridden if [.facingPosition] is set.
     * 
     * @return camera x-axis rotation
     */
    fun rotationX(): @IntRange(from = -90, to = 90) Int

    /**
     * Gets the y-axis rotation of the camera.
     * Will be overridden if [.facingPosition] is set.
     * 
     * @return camera y-axis rotation
     */
    fun rotationY(): Int

    /**
     * Gets the position that the camera is facing.
     * Can be used instead of manually setting rotation values.
     * 
     * 
     * If set, the rotation values set via [.rotationX] and [.rotationY] will be ignored.
     * 
     * @return Camera's facing position
     */
    fun facingPosition(): Vector3f?

    /**
     * Controls whether player effects, such as night vision or blindness, should be rendered on the camera.
     * Defaults to false.
     * 
     * @return whether player effects should be rendered
     */
    fun renderPlayerEffects(): Boolean

    /**
     * Controls whether the player position should be used for directional audio.
     * If false, the camera position will be used instead.
     * 
     * @return whether the players position should be used for directional audio
     */
    fun playerPositionForAudio(): Boolean

    interface Builder {
        fun cameraFade(cameraFade: CameraFade?): Builder?

        fun renderPlayerEffects(renderPlayerEffects: Boolean): Builder?

        fun playerPositionForAudio(playerPositionForAudio: Boolean): Builder?

        fun easeType(easeType: CameraEaseType?): Builder?

        fun easeSeconds(easeSeconds: Float): Builder?

        fun position(position: Vector3f): Builder?

        fun rotationX(rotationX: @IntRange(from = -90, to = 90) Int): Builder?

        fun rotationY(rotationY: Int): Builder?

        fun facingPosition(facingPosition: Vector3f?): Builder?

        fun build(): CameraPosition?
    }

    companion object {
        /**
         * Creates a Builder for CameraPosition
         * 
         * @return a CameraPosition Builder
         */
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
