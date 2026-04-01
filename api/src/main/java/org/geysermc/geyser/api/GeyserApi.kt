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
package org.geysermc.geyser.api

import org.geysermc.api.Geyser
import org.geysermc.api.GeyserApiBase
import org.geysermc.api.util.ApiVersion
import org.geysermc.geyser.api.command.CommandSource
import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.event.EventBus
import org.geysermc.geyser.api.event.EventRegistrar
import org.geysermc.geyser.api.extension.ExtensionManager
import org.geysermc.geyser.api.network.BedrockListener
import org.geysermc.geyser.api.network.RemoteServer
import org.geysermc.geyser.api.util.MinecraftVersion
import org.geysermc.geyser.api.util.PlatformType
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import java.util.*

/**
 * Represents the API used in Geyser.
 */
@ApiStatus.NonExtendable
interface GeyserApi : GeyserApiBase {
    /**
     * {@inheritDoc}
     */
    override fun connectionByUuid(uuid: UUID): GeyserConnection?

    /**
     * {@inheritDoc}
     */
    override fun connectionByXuid(xuid: String): GeyserConnection?

    /**
     * {@inheritDoc}
     */
    override fun onlineConnections(): MutableList<out GeyserConnection?>

    /**
     * Gets the [ExtensionManager].
     * 
     * @return the extension manager
     */
    fun extensionManager(): ExtensionManager

    /**
     * Provides an implementation for the specified API type.
     * 
     * @param apiClass the builder class
     * @param <R> the implementation type
     * @param <T> the API type
     * @throws IllegalArgumentException if there is no provider for the specified API class
     * @return the builder instance
    </T></R> */
    fun <R : T?, T> provider(apiClass: Class<T?>, vararg args: Any?): R

    /**
     * Gets the [EventBus] for handling
     * Geyser events.
     * 
     * @return the event bus
     */
    fun eventBus(): EventBus<EventRegistrar?>

    /**
     * Gets the default [RemoteServer] configured
     * within the config file that is used by default.
     * 
     * @return the default remote server used within Geyser
     */
    fun defaultRemoteServer(): RemoteServer

    /**
     * Gets the [BedrockListener] used for listening
     * for Minecraft: Bedrock Edition client connections.
     * 
     * @return the listener used for Bedrock client connectins
     */
    fun bedrockListener(): BedrockListener

    /**
     * Gets the [Path] to the Geyser config directory.
     * 
     * @return the path to the Geyser config directory
     */
    fun configDirectory(): Path

    /**
     * Gets the [Path] to the Geyser packs directory.
     * 
     * @return the path to the Geyser packs directory
     */
    fun packDirectory(): Path

    /**
     * Gets [PlatformType] the extension is running on
     * 
     * @return type of platform
     */
    fun platformType(): PlatformType

    /**
     * Gets the version of Java Minecraft that is supported.
     * 
     * @return the supported version of Java Minecraft
     */
    fun supportedJavaVersion(): MinecraftVersion

    /**
     * Gets a list of Bedrock Minecraft versions that are supported.
     * 
     * @return the list of supported Bedrock Minecraft versions
     */
    fun supportedBedrockVersions(): MutableList<MinecraftVersion?>

    /**
     * Gets the [CommandSource] for the console.
     * 
     * @return the console command source
     */
    fun consoleCommandSource(): CommandSource

    /**
     * Returns the [ApiVersion] representing the current Geyser api version.
     * See the [Geyser version outline](https://github.com/geysermc/api/blob/master/geyser-versioning.md))
     * 
     * @return the current geyser api version
     */
    fun geyserApiVersion(): ApiVersion {
        return BuildData.API_VERSION
    }

    companion object {
        /**
         * Gets the current [GeyserApiBase] instance.
         * 
         * @return the current geyser api instance
         */
        @kotlin.jvm.JvmStatic
        fun api(): GeyserApi {
            return Geyser.api<GeyserApi>(GeyserApi::class.java)
        }
    }
}
