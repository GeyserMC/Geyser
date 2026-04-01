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
package org.geysermc.geyser.api.extension

import org.geysermc.api.GeyserApiBase
import org.geysermc.geyser.api.GeyserApi
import org.geysermc.geyser.api.event.EventRegistrar
import org.geysermc.geyser.api.event.ExtensionEventBus
import java.nio.file.Path
import java.util.*

/**
 * Represents an extension within Geyser.
 */
interface Extension : EventRegistrar {
    var isEnabled: Boolean
        /**
         * Gets if the extension is enabled
         * 
         * @return true if the extension is enabled
         */
        get() = this.extensionLoader().isEnabled(this)
        /**
         * Enables or disables the extension
         * 
         * @param enabled if the extension should be enabled
         */
        set(enabled) {
            this.extensionLoader().setEnabled(this, enabled)
        }

    /**
     * Gets the extension's data folder
     * 
     * @return the extension's data folder
     */
    fun dataFolder(): Path {
        return this.extensionLoader().dataFolder(this)
    }

    /**
     * Gets the [ExtensionEventBus].
     * 
     * @return the extension event bus
     */
    fun eventBus(): ExtensionEventBus {
        return this.extensionLoader().eventBus(this)
    }

    /**
     * Gets the [ExtensionManager].
     * 
     * @return the extension manager
     */
    fun extensionManager(): ExtensionManager {
        return this.geyserApi().extensionManager()
    }

    /**
     * Gets the extension's name
     * 
     * @return the extension's name
     */
    fun name(): String {
        return this.description().name()
    }

    /**
     * Gets this extension's [ExtensionDescription].
     * 
     * @return the extension's description
     */
    fun description(): ExtensionDescription {
        return this.extensionLoader().description(this)
    }

    /**
     * @return the root command that all of this extension's commands will stem from.
     * By default, this is the extension's id.
     */
    fun rootCommand(): String {
        return this.description().id()
    }

    /**
     * Gets the extension's logger
     * 
     * @return the extension's logger
     */
    fun logger(): ExtensionLogger {
        return this.extensionLoader().logger(this)
    }

    /**
     * Gets the [ExtensionLoader].
     * 
     * @return the extension loader
     */
    fun extensionLoader(): ExtensionLoader {
        return Objects.requireNonNull<ExtensionLoader?>(this.extensionManager().extensionLoader())
    }

    /**
     * Gets the [GeyserApiBase] instance
     * 
     * @return the geyser api instance
     */
    fun geyserApi(): GeyserApi {
        return GeyserApi.Companion.api()
    }

    /**
     * Disable the extension.
     */
    fun disable() {
        this.isEnabled = false
        this.eventBus().unregisterAll()
    }
}
