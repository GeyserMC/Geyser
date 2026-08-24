/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.entity;

import org.checkerframework.checker.index.qual.NonNegative;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides entity lookup and input-lock utilities for a specific connection.
 * Accessed via {@link GeyserConnection#entities()}.
 */
public interface EntityData {

    /**
     * @deprecated use {@link #byJavaId(int)}
     * @since 2.3.0
     */
    @Deprecated(since = "2.11.0")
    CompletableFuture<@Nullable GeyserEntity> entityByJavaId(@NonNegative int javaId);

    /**
     * Returns the {@link GeyserEntity} for the given Java entity ID if it is tracked
     * in this connection's entity cache, or {@code null} if not found.
     *
     * @param javaId the Java entity ID to look up
     * @return the entity, or {@code null} if not found
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    @Nullable GeyserEntity byJavaId(@NonNegative int javaId);

    /**
     * Returns the {@link GeyserEntity} for the given Java entity UUID if it is tracked
     * in this connection's entity cache, or {@code null} if not found.
     *
     * @param javaUuid the Java entity UUID to look up
     * @return the entity, or {@code null} if not found
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    @Nullable GeyserEntity byUuid(UUID javaUuid);

    /**
     * Returns the {@link GeyserEntity} for the given Geyser runtime entity ID if it is tracked
     * in this connection's entity cache, or {@code null} if not found.
     *
     * @param geyserId the Geyser entity ID (as returned by {@link GeyserEntity#geyserId()})
     * @return the entity, or {@code null} if not found
     * @since 2.11.0
     */
    @ApiStatus.Experimental
    @Nullable GeyserEntity byGeyserId(@NonNegative long geyserId);

    /**
     * (Un)locks the client's movement inputs, so that they cannot move.
     * To ensure that movement is only unlocked when all locks are released, you must supply
     * a UUID with this method, and use the same UUID to unlock the camera.
     *
     * @param lock whether to lock the movement
     * @param owner the owner of the lock
     * @return if the movement is locked after this method call
     */
    boolean lockMovement(boolean lock, UUID owner);

    /**
     * Returns whether the client's movement is currently locked.
     *
     * @return whether the movement is locked
     */
    boolean isMovementLocked();

    /**
     * @deprecated use {@link GeyserConnection#requestOffhandSwap()} instead
     */
    @Deprecated(since = "2.9.3")
    void switchHands();

    /**
     * @deprecated Use {@link GeyserConnection#showEmote(GeyserPlayerEntity, String)} instead.
     */
    @Deprecated(since = "2.9.3")
    void showEmote(GeyserPlayerEntity emoter, String emoteId);

    /**
     * @deprecated Use {@link GeyserConnection#playerEntity()} instead.
     */
    @Deprecated(since = "2.9.3")
    GeyserPlayerEntity playerEntity();
}
