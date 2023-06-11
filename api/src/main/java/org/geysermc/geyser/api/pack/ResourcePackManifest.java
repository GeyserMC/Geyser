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

package org.geysermc.geyser.api.pack;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents a resource pack manifest.
 */
public interface ResourcePackManifest {

    /**
     * Gets the format version of the resource pack.
     *
     * @return the format version
     */
    @NonNull
    Integer formatVersion();

    /**
     * Gets the header of the resource pack.
     *
     * @return the header
     */
    @NonNull
    Header header();

    /**
     * Gets the modules of the resource pack.
     *
     * @return the modules
     */
    @NonNull
    Collection<? extends Module> modules();

    /**
     * Gets the dependencies of the resource pack.
     *
     * @return the dependencies
     */
    @NonNull
    Collection<? extends Dependency> dependencies();

    /**
     * Represents the header of a resource pack.
     */
    interface Header {

        /**
         * Gets the UUID of the resource pack.
         *
         * @return the UUID
         */
        @NonNull
        UUID uuid();

        /**
         * Gets the version of the resource pack.
         *
         * @return the version
         */
        int @NonNull [] version();

        /**
         * Gets the name of the resource pack.
         *
         * @return the name
         */
        @NonNull
        String name();

        /**
         * Gets the description of the resource pack.
         *
         * @return the description
         */
        @NonNull
        String description();

        /**
         * Gets the version string of the resource pack.
         *
         * @return the version string
         */
        @NonNull
        String versionString();

        /**
         * Gets the minimum supported Minecraft version of the resource pack.
         *
         * @return the minimum supported Minecraft version
         */
        int @NonNull [] minimumSupportedMinecraftVersion();
    }

    /**
     * Represents a module of a resource pack.
     */
    interface Module {

        /**
         * Gets the UUID of the module.
         *
         * @return the UUID
         */
        @NonNull
        UUID uuid();

        /**
         * Gets the version of the module.
         *
         * @return the version
         */
        int @NonNull [] version();

        /**
         * Gets the type of the module.
         *
         * @return the type
         */
        @NonNull
        String type();

        /**
         * Gets the description of the module.
         *
         * @return the description
         */
        @NonNull
        String description();
    }

    /**
     * Represents a dependency of a resource pack.
     */
    interface Dependency {

        /**
         * Gets the UUID of the dependency.
         *
         * @return the uuid
         */
        @NonNull
        UUID uuid();

        /**
         * Gets the version of the dependency.
         *
         * @return the version
         */
        int @NonNull [] version();
    }
}

