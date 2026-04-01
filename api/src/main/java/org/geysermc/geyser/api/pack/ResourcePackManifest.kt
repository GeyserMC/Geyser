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
package org.geysermc.geyser.api.pack

import org.geysermc.geyser.api.pack.option.SubpackOption
import java.util.*

/**
 * Represents a Bedrock edition resource pack manifest (manifest.json).
 * All resource packs are required to have such a file as it identifies the resource pack.
 * See [
 * Microsoft's docs for more info](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/addonsreference/examples/addonmanifest?view=minecraft-bedrock-stable).
 * @since 2.1.1
 */
interface ResourcePackManifest {
    /**
     * Gets the format version of the resource pack.
     * 
     * 
     * "1" is used for skin packs,
     * "2" is used for resource and behavior packs, and world templates.
     * 
     * @return the format version
     * @since 2.1.1
     */
    fun formatVersion(): Int

    /**
     * Gets the [Header] of the resource pack.
     * 
     * @return the [Header]
     * @since 2.1.1
     */
    fun header(): Header

    /**
     * Gets the [Module]'s of the resource pack.
     * 
     * @return a collection of modules
     * @since 2.1.1
     */
    fun modules(): MutableCollection<out Module?>

    /**
     * Gets the [Dependency]'s of the resource pack.
     * 
     * @return a collection of dependencies
     * @since 2.6.2
     */
    fun dependencies(): MutableCollection<out Dependency?>

    /**
     * Gets the [Subpack]'s of the resource pack.
     * See [Microsoft's docs on subpacks
    ](https://learn.microsoft.com/en-us/minecraft/creator/documents/utilizingsubpacks) *  for more information.
     * 
     * @return a collection of subpacks
     * @since 2.6.2
     */
    fun subpacks(): MutableCollection<out Subpack?>

    /**
     * Gets the [Setting]'s of the resource pack.
     * These are shown to Bedrock client's in the resource pack settings menu ([see here](https://learn.microsoft.com/en-us/minecraft/creator/documents/media/utilizingsubpacks/subpackgif.gif?view=minecraft-bedrock-stable))
     * to inform users about what the resource pack and sub-packs include.
     * 
     * @return a collection of settings
     * @since 2.6.2
     */
    fun settings(): MutableCollection<out Setting?>

    /**
     * Represents the header of a resource pack.
     * It contains the main information about the resource pack, such as
     * the name, description, or uuid.
     * See [
     * Microsoft's docs for further details on headers.](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/addonsreference/examples/addonmanifest?view=minecraft-bedrock-stable#header)
     * @since 2.1.1
     */
    interface Header {
        /**
         * Gets the UUID of the resource pack. It is a unique identifier that differentiates this resource pack from any other resource pack.
         * Bedrock clients will cache resource packs, and download resource packs when the uuid is new (or the version changes).
         * 
         * @return the UUID
         * @since 2.1.1
         */
        fun uuid(): UUID

        /**
         * Gets the version of the resource pack.
         * 
         * @return the version
         * @since 2.1.1
         */
        fun version(): Version

        /**
         * Gets the name of the resource pack.
         * 
         * @return the name
         * @since 2.1.1
         */
        fun name(): String

        /**
         * Gets the description of the resource pack.
         * 
         * @return the description
         * @since 2.1.1
         */
        fun description(): String

        /**
         * Gets the minimum supported Minecraft version of the resource pack.
         * 
         * @return the minimum supported Minecraft version
         * @since 2.1.1
         */
        fun minimumSupportedMinecraftVersion(): Version
    }

    /**
     * Represents a module of a resource pack.
     * It contains information about the content type that is
     * offered by this resource pack.
     * See [
     * Microsoft's docs for further details on modules.](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/addonsreference/examples/addonmanifest?view=minecraft-bedrock-stable#modules)
     * @since 2.1.1
     */
    interface Module {
        /**
         * Gets the UUID of the module.
         * This should usually be different from the UUID in the [Header].
         * 
         * @return the UUID
         * @since 2.1.1
         */
        fun uuid(): UUID

        /**
         * Gets the [Version] of the module.
         * 
         * @return the [Version]
         * @since 2.1.1
         */
        fun version(): Version

        /**
         * Gets the type of the module.
         * 
         * @return the type
         * @since 2.1.1
         */
        fun type(): String

        /**
         * Gets the description of the module.
         * 
         * @return the description
         * @since 2.1.1
         */
        fun description(): String
    }

    /**
     * Represents a dependency of a resource pack.
     * These are references to other resource packs that must be
     * present in order for this resource pack to apply.
     * See [
     * Microsoft's docs for further details on dependencies.](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/addonsreference/examples/addonmanifest?view=minecraft-bedrock-stable#dependencies)
     * @since 2.1.1
     */
    interface Dependency {
        /**
         * Gets the UUID of the resource pack dependency.
         * 
         * @return the uuid
         * @since 2.1.1
         */
        fun uuid(): UUID

        /**
         * Gets the [Version] of the dependency.
         * 
         * @return the [Version]
         * @since 2.1.1
         */
        fun version(): Version
    }

    /**
     * Represents a subpack of a resource pack. These are often used for "variants" of the resource pack,
     * such as lesser details, or additional features either to be determined by player's taste or adapted to the player device's performance.
     * See [Micoroft's docs](https://learn.microsoft.com/en-us/minecraft/creator/documents/utilizingsubpacks) for more information.
     */
    interface Subpack {
        /**
         * Gets the folder name where this sub-pack is placed in.
         * 
         * @return the folder name
         * @since 2.6.2
         */
        fun folderName(): String

        /**
         * Gets the name of this subpack. Required for each subpack to be valid.
         * To make a Bedrock client load any subpack, register the resource pack
         * in the [SessionLoadResourcePacksEvent] or [GeyserDefineResourcePacksEvent] and specify a
         * [SubpackOption] with the name of the subpack to load.
         * 
         * @return the subpack name
         * @since 2.6.2
         */
        fun name(): String

        /**
         * Gets the memory tier of this Subpack, representing how much RAM a device must have to run it.
         * Each memory tier requires 0.25 GB of RAM. For example, a memory tier of 0 is no requirement,
         * and a memory tier of 4 requires 1GB of RAM.
         * 
         * @return the memory tier
         * @since 2.6.2
         */
        fun memoryTier(): Float?
    }

    /**
     * Represents a setting that is shown client-side that describe what a pack does.
     * Multiple setting entries are shown in separate paragraphs.
     * @since 2.6.2
     */
    interface Setting {
        /**
         * The type of the setting. Usually just "label".
         * 
         * @return the type
         * @since 2.6.2
         */
        fun type(): String

        /**
         * The text shown for the setting.
         * 
         * @return the text content
         * @since 2.6.2
         */
        fun text(): String
    }

    /**
     * Represents a version of a resource pack.
     * @since 2.1.1
     */
    interface Version {
        /**
         * Gets the major version.
         * 
         * @return the major version
         * @since 2.1.1
         */
        fun major(): Int

        /**
         * Gets the minor version.
         * 
         * @return the minor version
         * @since 2.1.1
         */
        fun minor(): Int

        /**
         * Gets the patch version.
         * 
         * @return the patch version
         * @since 2.1.1
         */
        fun patch(): Int

        /**
         * Gets the version formatted as a String.
         * 
         * @return the version string
         * @since 2.1.1
         */
        override fun toString(): String
    }
}

