/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.api.connection

import org.checkerframework.checker.index.qual.NonNegative
import org.checkerframework.checker.index.qual.Positive
import org.geysermc.api.connection.Connection
import org.geysermc.geyser.api.bedrock.camera.CameraData
import org.geysermc.geyser.api.bedrock.camera.CameraShake
import org.geysermc.geyser.api.command.CommandSource
import org.geysermc.geyser.api.entity.EntityData
import org.geysermc.geyser.api.entity.type.GeyserEntity
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity
import org.geysermc.geyser.api.skin.SkinData
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.Float
import kotlin.Int
import kotlin.String

/**
 * Represents a player connection used in Geyser.
 */
interface GeyserConnection : Connection, CommandSource {
    /**
     * Exposes the [CameraData] for this connection.
     * It allows you to send fogs, camera shakes, force camera perspectives, and more.
     * 
     * @return the CameraData for this connection.
     */
    fun camera(): CameraData

    /**
     * Exposes the [EntityData] for this connection.
     * It allows you to get entities by their Java entity ID, show emotes, and get the player entity.
     * 
     * @return the EntityData for this connection.
     */
    fun entities(): EntityData

    /**
     * Returns the current ping of the connection.
     */
    fun ping(): Int

    /**
     * @return `true` if the client currently has a form open.
     * @since 2.8.0
     */
    fun hasFormOpen(): Boolean

    /**
     * Closes the currently open form on the client.
     */
    fun closeForm()

    /**
     * Gets the Bedrock protocol version of the player.
     */
    fun protocolVersion(): Int

    /**
     * Attempts to open the `minecraft:pause_screen_additions` dialog tag. This method opens this dialog the same way Java does, that is:
     * 
     * 
     *  * If there are multiple dialogs in the additions tag, the `minecraft:custom_options` dialog is opened to select a dialog.
     *  * If there is one dialog in the additions tag, that dialog is opened.
     *  * If there are no dialogs in the tag, but there are server links sent to the client, the `minecraft:server_links` dialog is opened.
     *  * If all of the above fails, no dialog is opened.
     * 
     * 
     * 
     * Use [GeyserConnection.hasFormOpen] to check if a dialog was opened.
     * @since 2.8.0
     */
    fun openPauseScreenAdditions()

    /**
     * Attempts to open the `minecraft:quick_actions` dialog tag. This method opens this dialog the same way Java does, that is:
     * 
     * 
     *  * If there are multiple dialogs in the actions tag, the `minecraft:quick_actions` dialog is opened to select a dialog.
     *  * If there is one dialog in the actions tag, that dialog is opened.
     *  * If there are no dialogs in the tag, no dialog is opened.
     * 
     * 
     * 
     * Use [GeyserConnection.hasFormOpen] to check if a dialog was opened.
     * @since 2.8.0
     */
    fun openQuickActions()

    /**
     * Sends a command as if the player had executed it.
     * 
     * @param command the command without the leading forward-slash
     * @since 2.8.0
     */
    fun sendCommand(command: String?)

    /**
     * Gets the hostname or ip address the player used to join this Geyser instance.
     * Example:
     * 
     *  *  `test.geysermc.org` 
     *  *  `127.0.0.1` 
     *  *  `06e9:c755:4eff:5f13:9b4c:4b21:9df2:6a73` 
     * 
     * 
     * @throws NoSuchElementException if called before the session is fully initialized
     * @return the ip address or hostname string the player used to join
     * @since 2.8.3
     */
    fun joinAddress(): String

    /**
     * Gets the port the player used to join this Geyser instance.
     * Example:
     * 
     *  *  `19132` 
     *  *  `2202` 
     * 
     * 
     * @throws NoSuchElementException if called before the session is fully initialized
     * @return the port the player used to join
     * @since 2.8.3
     */
    fun joinPort(): @Positive Int

    /**
     * Applies a skin to a player seen by this Geyser connection.
     * If the uuid matches the [GeyserConnection.javaUuid], this
     * will update the skin of this Geyser connection.
     * If the player uuid provided is not known to this connection, this method
     * will silently return.
     * 
     * @param player which player this skin should be applied to
     * @param skinData the skin data to apply
     * @since 2.8.3
     */
    fun sendSkin(player: UUID, skinData: SkinData)

    /**
     * @param javaId the Java entity ID to look up.
     * @return a [GeyserEntity] if present in this connection's entity tracker.
     */
    @Deprecated("Use {@link EntityData#entityByJavaId(int)} instead")
    fun entityByJavaId(javaId: @NonNegative Int): CompletableFuture<GeyserEntity?>

    /**
     * Displays a player entity as emoting to this client.
     * 
     * @param emoter the player entity emoting.
     * @param emoteId the emote ID to send to this client.
     */
    fun showEmote(emoter: GeyserPlayerEntity, emoteId: String)

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
     * 
     */
    @Deprecated("Use {@link CameraData#shakeCamera(float, float, CameraShake)} instead.")
    fun shakeCamera(intensity: Float, duration: Float, type: CameraShake)

    /**
     * Stops all camera shake of any type.
     * 
     */
    @Deprecated("Use {@link CameraData#stopCameraShake()} instead.")
    fun stopCameraShake()

    /**
     * Adds the given fog IDs to the fog cache, then sends all fog IDs in the cache to the client.
     * 
     * 
     * Fog IDs can be found [here](https://wiki.bedrock.dev/documentation/fog-ids.html)
     * 
     * @param fogNameSpaces the fog IDs to add. If empty, the existing cached IDs will still be sent.
     */
    @Deprecated("Use {@link CameraData#sendFog(String...)} instead.")
    fun sendFog(vararg fogNameSpaces: String?)

    /**
     * Removes the given fog IDs from the fog cache, then sends all fog IDs in the cache to the client.
     * 
     * @param fogNameSpaces the fog IDs to remove. If empty, all fog IDs will be removed.
     */
    @Deprecated("Use {@link CameraData#removeFog(String...)} instead.")
    fun removeFog(vararg fogNameSpaces: String?)

    /**
     * Returns an immutable copy of all fog affects currently applied to this client.
     * 
     */
    @Deprecated("Use {@link CameraData#fogEffects()} instead.")
    fun fogEffects(): MutableSet<String?>

    /**
     * Returns the associated player entity for this connection.
     * 
     * @return the [GeyserPlayerEntity] for this connection
     * @since 2.9.3
     */
    fun playerEntity(): GeyserPlayerEntity

    /**
     * Requests an offhand swap from the Java server.
     * There is no guarantee of the server accepting the request.
     * 
     * @since 2.9.3
     */
    fun requestOffhandSwap()

    /**
     * The PlayFab ID of this player.
     * 
     * @since 2.9.4
     */
    fun playFabId(): String
}
