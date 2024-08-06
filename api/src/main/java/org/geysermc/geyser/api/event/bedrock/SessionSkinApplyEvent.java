/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.bedrock;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.skin.Cape;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.skin.SkinGeometry;

import java.util.UUID;

/**
 * Called when a skin is applied to a player.
 * <p>
 * Won't be called when a fake player is spawned for a player skull.
 */
public abstract class SessionSkinApplyEvent extends ConnectionEvent {

    private final String username;
    private final UUID uuid;
    private final boolean slim;
    private final boolean bedrock;
    private final SkinData originalSkinData;

    public SessionSkinApplyEvent(@NonNull GeyserConnection connection, String username, UUID uuid, boolean slim, boolean bedrock, SkinData skinData) {
        super(connection);
        this.username = username;
        this.uuid = uuid;
        this.slim = slim;
        this.bedrock = bedrock;
        this.originalSkinData = skinData;
    }

    /**
     * The username of the player.
     *
     * @return the username of the player
     */
    public @NonNull String username() {
        return username;
    }

    /**
     * The UUID of the player.
     *
     * @return the UUID of the player
     */
    public @NonNull UUID uuid() {
        return uuid;
    }

    /**
     * If the player is using a slim model.
     *
     * @return if the player is using a slim model
     */
    public boolean slim() {
        return slim;
    }

    /**
     * If the player is a Bedrock player.
     *
     * @return if the player is a Bedrock player
     */
    public boolean bedrock() {
        return bedrock;
    }

    /**
     * The original skin data of the player.
     *
     * @return the original skin data of the player
     */
    public @NonNull SkinData originalSkin() {
        return originalSkinData;
    }

    /**
     * The skin data of the player.
     *
     * @return the current skin data of the player
     */
    public abstract @NonNull SkinData skinData();

    /**
     * Change the skin of the player.
     *
     * @param newSkin the new skin
     */
    public abstract void skin(@NonNull Skin newSkin);

    /**
     * Change the cape of the player.
     *
     * @param newCape the new cape
     */
    public abstract void cape(@NonNull Cape newCape);

    /**
     * Change the geometry of the player.
     *
     * @param newGeometry the new geometry
     */
    public abstract void geometry(@NonNull SkinGeometry newGeometry);

    /**
     * Change the geometry of the player.
     * <p>
     * Constructs a generic {@link SkinGeometry} object with the given data.
     *
     * @param geometryName the name of the geometry
     * @param geometryData the data of the geometry
     */
    public void geometry(@NonNull String geometryName, @NonNull String geometryData) {
        geometry(new SkinGeometry("{\"geometry\" :{\"default\" :\"" + geometryName + "\"}}", geometryData));
    }
}
