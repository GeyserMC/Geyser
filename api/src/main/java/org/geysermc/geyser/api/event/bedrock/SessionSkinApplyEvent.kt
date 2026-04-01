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
package org.geysermc.geyser.api.event.bedrock

import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.event.connection.ConnectionEvent
import org.geysermc.geyser.api.skin.Cape
import org.geysermc.geyser.api.skin.Skin
import org.geysermc.geyser.api.skin.SkinData
import org.geysermc.geyser.api.skin.SkinGeometry
import java.util.*

/**
 * Called when a skin is applied to a player.
 * 
 * 
 * Won't be called when a fake player is spawned for a player skull.
 */
abstract class SessionSkinApplyEvent(
    connection: GeyserConnection,
    private val username: String,
    private val uuid: UUID,
    private val slim: Boolean,
    private val bedrock: Boolean,
    private val originalSkinData: SkinData
) : ConnectionEvent(connection) {
    /**
     * The username of the player.
     * 
     * @return the username of the player
     */
    fun username(): String {
        return username
    }

    /**
     * The UUID of the player.
     * 
     * @return the UUID of the player
     */
    fun uuid(): UUID {
        return uuid
    }

    /**
     * If the player is using a slim model.
     * 
     * @return if the player is using a slim model
     */
    fun slim(): Boolean {
        return slim
    }

    /**
     * If the player is a Bedrock player.
     * 
     * @return if the player is a Bedrock player
     */
    fun bedrock(): Boolean {
        return bedrock
    }

    /**
     * The original skin data of the player.
     * 
     * @return the original skin data of the player
     */
    fun originalSkin(): SkinData {
        return originalSkinData
    }

    /**
     * The skin data of the player.
     * 
     * @return the current skin data of the player
     */
    abstract fun skinData(): SkinData

    /**
     * Change the skin of the player.
     * 
     * @param newSkin the new skin
     */
    abstract fun skin(newSkin: Skin)

    /**
     * Change the cape of the player.
     * 
     * @param newCape the new cape
     */
    abstract fun cape(newCape: Cape)

    /**
     * Change the geometry of the player.
     * 
     * @param newGeometry the new geometry
     */
    abstract fun geometry(newGeometry: SkinGeometry)

    /**
     * Change the geometry of the player.
     * 
     * 
     * Constructs a generic [SkinGeometry] object with the given data.
     * 
     * @param geometryName the name of the geometry
     * @param geometryData the data of the geometry
     */
    fun geometry(geometryName: String, geometryData: String) {
        geometry(SkinGeometry("{\"geometry\" :{\"default\" :\"" + geometryName + "\"}}", geometryData))
    }
}
