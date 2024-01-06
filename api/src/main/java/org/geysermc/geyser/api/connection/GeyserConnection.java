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

package org.geysermc.geyser.api.connection;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.connection.Connection;
import org.geysermc.geyser.api.bedrock.camera.CameraData;
import org.geysermc.geyser.api.bedrock.camera.CameraShake;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.entity.EntityData;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a player connection used in Geyser.
 */
public interface GeyserConnection extends Connection, CommandSource {

    /**
     * Exposes the {@link CameraData} for this connection.
     * It allows you to send fogs, camera shakes, force camera perspectives, and more.
     *
     * @return the CameraData for this connection.
     */
    @NonNull CameraData camera();

    /**
     * Exposes the {@link EntityData} for this connection.
     * It allows you to get entities by their Java entity ID, show emotes, and get the player entity.
     *
     * @return the EntityData for this connection.
     */
    @NonNull EntityData entities();

    /**
     * @param javaId the Java entity ID to look up.
     * @return a {@link GeyserEntity} if present in this connection's entity tracker.
     * @deprecated Use {@link EntityData#entityByJavaId(int)} instead
     */
    @Deprecated
    @NonNull
    CompletableFuture<@Nullable GeyserEntity> entityByJavaId(@NonNegative int javaId);

    /**
     * Displays a player entity as emoting to this client.
     *
     * @param emoter the player entity emoting.
     * @param emoteId the emote ID to send to this client.
     * @deprecated use {@link EntityData#showEmote(GeyserPlayerEntity, String)} instead
     */
    @Deprecated
    void showEmote(@NonNull GeyserPlayerEntity emoter, @NonNull String emoteId);

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
     *
     * @deprecated Use {@link CameraData#shakeCamera(float, float, CameraShake)} instead.
     */
    @Deprecated
    void shakeCamera(float intensity, float duration, @NonNull CameraShake type);

    /**
     * Stops all camera shake of any type.
     *
     * @deprecated Use {@link CameraData#stopCameraShake()} instead.
     */
    @Deprecated
    void stopCameraShake();

    /**
     * Adds the given fog IDs to the fog cache, then sends all fog IDs in the cache to the client.
     * <p>
     * Fog IDs can be found <a href="https://wiki.bedrock.dev/documentation/fog-ids.html">here</a>
     *
     * @param fogNameSpaces the fog IDs to add. If empty, the existing cached IDs will still be sent.
     * @deprecated Use {@link CameraData#sendFog(String...)} instead.
     */
    @Deprecated
    void sendFog(String... fogNameSpaces);

    /**
     * Removes the given fog IDs from the fog cache, then sends all fog IDs in the cache to the client.
     *
     * @param fogNameSpaces the fog IDs to remove. If empty, all fog IDs will be removed.
     * @deprecated Use {@link CameraData#removeFog(String...)} instead.
     */
    @Deprecated
    void removeFog(String... fogNameSpaces);

    /**
     * Returns an immutable copy of all fog affects currently applied to this client.
     *
     * @deprecated Use {@link CameraData#fogEffects()} instead.
     */
    @Deprecated
    @NonNull
    Set<String> fogEffects();
}
