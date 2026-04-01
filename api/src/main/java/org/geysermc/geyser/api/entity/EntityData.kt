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
package org.geysermc.geyser.api.entity

import org.checkerframework.checker.index.qual.NonNegative
import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.entity.type.GeyserEntity
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * This class holds all the methods that relate to entities.
 * Can be accessed through [GeyserConnection.entities].
 */
interface EntityData {
    /**
     * Returns a [GeyserEntity] to e.g. make them play an emote.
     * 
     * @param javaId the Java entity ID to look up
     * @return a [GeyserEntity] if present in this connection's entity tracker
     */
    fun entityByJavaId(javaId: @NonNegative Int): CompletableFuture<GeyserEntity?>

    /**
     * (Un)locks the client's movement inputs, so that they cannot move.
     * To ensure that movement is only unlocked when all locks are released, you must supply
     * a UUID with this method, and use the same UUID to unlock the camera.
     * 
     * @param lock whether to lock the movement
     * @param owner the owner of the lock
     * @return if the movement is locked after this method call
     */
    fun lockMovement(lock: Boolean, owner: UUID): Boolean

    /**
     * Returns whether the client's movement is currently locked.
     * 
     * @return whether the movement is locked
     */
    val isMovementLocked: Boolean

    @Deprecated("use {@link GeyserConnection#requestOffhandSwap()} instead")
    fun switchHands()

    @Deprecated("Use {@link GeyserConnection#showEmote(GeyserPlayerEntity, String)} instead.")
    fun showEmote(emoter: GeyserPlayerEntity, emoteId: String)

    @Deprecated("Use {@link GeyserConnection#playerEntity} instead.")
    fun playerEntity(): GeyserPlayerEntity
}
